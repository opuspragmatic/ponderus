#!/usr/bin/env bash
#
# deploy.sh — build et deploiement de ponderus-backend sur Cloud Run.
#
# Ce script :
#   1. build l'image via Cloud Build (le Dockerfile a la racine) et la pousse
#      vers Artifact Registry — pas besoin de Docker en local ;
#   2. deploie l'image sur Cloud Run ;
#   3. cable les secrets (mot de passe DB) depuis Secret Manager plutot que
#      de les passer en clair.
#
# Prerequis :
#   - gcloud CLI installe et authentifie (`gcloud auth login`)
#   - APIs activees sur le projet : run.googleapis.com, cloudbuild.googleapis.com,
#     artifactregistry.googleapis.com, secretmanager.googleapis.com
#     (le script les active automatiquement si besoin, cf. ensure_apis)
#   - Un repo Artifact Registry (cree automatiquement si absent)
#   - Les secrets DB crees dans Secret Manager (voir section "Secrets" plus bas)
#
# Usage :
#   ./deploy.sh                     # deploiement avec les valeurs par defaut
#   PROJECT_ID=mon-projet REGION=europe-west1 ./deploy.sh
#   SKIP_BUILD=true ./deploy.sh     # redeploie la derniere image sans rebuild
#
# Toutes les valeurs ci-dessous sont surchargeables via variables d'env.

set -euo pipefail

# --------------------------------------------------------------------------
# Configuration (surchargeable via variables d'environnement)
# --------------------------------------------------------------------------

PROJECT_ID="${PROJECT_ID:-$(gcloud config get-value project 2>/dev/null)}"
REGION="${REGION:-northamerica-northeast1}"
SERVICE_NAME="${SERVICE_NAME:-ponderus-backend}"

AR_REPO="${AR_REPO:-ponderus}"                 # nom du repo Artifact Registry
IMAGE_TAG="${IMAGE_TAG:-$(git rev-parse --short HEAD 2>/dev/null || echo latest)}"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${AR_REPO}/${SERVICE_NAME}:${IMAGE_TAG}"

# Compte de service utilise par le service Cloud Run (Application Default
# Credentials : c'est lui qui autorise l'appel a Firebase Admin, cf.
# FirebaseConfig.java qui appelle GoogleCredentials.getApplicationDefault()).
# Laisser vide pour utiliser le compte de service par defaut Compute Engine.
SERVICE_ACCOUNT="${SERVICE_ACCOUNT:-}"

# Nom de l'instance Cloud SQL au format PROJECT:REGION:INSTANCE.
# Requis : c'est cette valeur qui alimente INSTANCE_CONNECTION_NAME, utilisee
# par le Cloud SQL Socket Factory (voir datasource.url dans application.yml).
CLOUD_SQL_INSTANCE="${CLOUD_SQL_INSTANCE:?Variable CLOUD_SQL_INSTANCE requise (format PROJECT:REGION:INSTANCE)}"

# Ressources Cloud Run
CPU="${CPU:-1}"
MEMORY="${MEMORY:-512Mi}"
MIN_INSTANCES="${MIN_INSTANCES:-0}"
MAX_INSTANCES="${MAX_INSTANCES:-4}"

# Le service expose /api/** derriere Firebase Auth (voir
# SecurityConfigFilterChain.java) : l'acces public au conteneur est donc sans
# risque, l'autorisation se fait au niveau applicatif. Passer a "false" pour
# exiger IAM en plus.
ALLOW_UNAUTHENTICATED="${ALLOW_UNAUTHENTICATED:-true}"

# Sauter le build et redeployer la derniere image poussee.
SKIP_BUILD="${SKIP_BUILD:-false}"

# --------------------------------------------------------------------------
# Secrets Secret Manager attendus (correspondent aux clefs de .env / application.yml)
#   - db-password        -> SPRING_DATASOURCE_PASSWORD
# Les creer une fois avec, par exemple :
#   echo -n "monMotDePasse" | gcloud secrets create db-password --data-file=-
# --------------------------------------------------------------------------
DB_PASSWORD_SECRET="${DB_PASSWORD_SECRET:-db-password}"

# --------------------------------------------------------------------------
# Variables d'environnement non sensibles pour l'application (application.yml)
# --------------------------------------------------------------------------
DB_NAME="${DB_NAME:-ponderus}"
DB_USER="${DB_USER:-ponderus}"

# --------------------------------------------------------------------------
# Verifications
# --------------------------------------------------------------------------

# Couleurs desactivees si la sortie n'est pas un terminal (ex: logs Cloud Build).
if [ -t 1 ]; then
    COLOR_GREEN=$'\033[0;32m'
    COLOR_RED=$'\033[0;31m'
    COLOR_RESET=$'\033[0m'
else
    COLOR_GREEN=""
    COLOR_RED=""
    COLOR_RESET=""
fi

log() { echo "${COLOR_GREEN}[deploy]${COLOR_RESET} $*"; }
die() { echo "${COLOR_RED}[deploy] ERREUR: $*${COLOR_RESET}" >&2; exit 1; }

command -v gcloud >/dev/null 2>&1 || die "gcloud CLI introuvable. Installez le Google Cloud SDK."
[ -n "$PROJECT_ID" ] || die "PROJECT_ID non defini. Lancez 'gcloud config set project <id>' ou export PROJECT_ID=..."

log "Projet     : ${PROJECT_ID}"
log "Region     : ${REGION}"
log "Service    : ${SERVICE_NAME}"
log "Image      : ${IMAGE}"

ensure_apis() {
    log "Verification des APIs GCP requises..."
    gcloud services enable \
        run.googleapis.com \
        cloudbuild.googleapis.com \
        artifactregistry.googleapis.com \
        secretmanager.googleapis.com \
        --project "${PROJECT_ID}" >/dev/null
}

ensure_artifact_registry() {
    if ! gcloud artifacts repositories describe "${AR_REPO}" \
        --location "${REGION}" --project "${PROJECT_ID}" >/dev/null 2>&1; then
        log "Creation du repo Artifact Registry '${AR_REPO}'..."
        gcloud artifacts repositories create "${AR_REPO}" \
            --repository-format=docker \
            --location "${REGION}" \
            --project "${PROJECT_ID}" \
            --description "Images Docker ponderus"
    fi
}

build_and_push() {
    log "Build de l'image via Cloud Build (cloudbuild.yaml, BuildKit active)..."
    gcloud builds submit \
        --project "${PROJECT_ID}" \
        --config cloudbuild.yaml \
        --substitutions "_IMAGE=${IMAGE}" \
        .
}

deploy() {
    log "Deploiement sur Cloud Run..."

    local deploy_args=(
        run deploy "${SERVICE_NAME}"
        --image "${IMAGE}"
        --project "${PROJECT_ID}"
        --region "${REGION}"
        --platform managed
        --cpu "${CPU}"
        --memory "${MEMORY}"
        --min-instances "${MIN_INSTANCES}"
        --max-instances "${MAX_INSTANCES}"
        --set-env-vars "DB_NAME=${DB_NAME},DB_USER=${DB_USER},INSTANCE_CONNECTION_NAME=${CLOUD_SQL_INSTANCE}"
        --set-secrets "SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD_SECRET}:latest"
        --add-cloudsql-instances "${CLOUD_SQL_INSTANCE}"
    )

    if [ "${ALLOW_UNAUTHENTICATED}" = "true" ]; then
        deploy_args+=(--allow-unauthenticated)
    else
        deploy_args+=(--no-allow-unauthenticated)
    fi

    if [ -n "${SERVICE_ACCOUNT}" ]; then
        deploy_args+=(--service-account "${SERVICE_ACCOUNT}")
    fi

    gcloud "${deploy_args[@]}"
}

# --------------------------------------------------------------------------
# Execution
# --------------------------------------------------------------------------

ensure_apis
ensure_artifact_registry

if [ "${SKIP_BUILD}" != "true" ]; then
    build_and_push
else
    log "SKIP_BUILD=true : build ignore, redeploiement de l'image existante ${IMAGE}"
fi

deploy

SERVICE_URL="$(gcloud run services describe "${SERVICE_NAME}" \
    --project "${PROJECT_ID}" --region "${REGION}" \
    --format='value(status.url)')"

log "Deploiement termine : ${SERVICE_URL}"
log "Healthcheck         : ${SERVICE_URL}/actuator/health"
