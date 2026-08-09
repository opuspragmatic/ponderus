# ponderus

Backend du comparateur pondéré — un outil pour comparer plusieurs options (maisons, ou n'importe quelle décision structurée) selon des critères pondérés, avec analyse de sensibilité et critères éliminatoires.

Ce repo contient l'**API Spring Boot**. Le frontend Angular vit dans un repo séparé.

## Stack

| Couche         | Techno                                   |
|----------------|-------------------------------------------|
| Langage        | Java 21                                   |
| Framework      | Spring Boot 3 (Web, Data JPA, Security)   |
| Base de données| PostgreSQL                                |
| Migrations     | Flyway                                    |
| Auth           | Firebase Authentication (vérification via Firebase Admin SDK) |
| Build          | Gradle                                    |
| Déploiement    | Google Cloud Run                          |

## Modèle de données

Cinq tables : `users`, `projects`, `options`, `criteria`, `scores`. La table `scores` est le croisement critère × option — c'est elle qui permet de comparer N options (et pas seulement 2) sur un même projet. Détail des colonnes dans la migration [`V1__init_schema.sql`](src/main/resources/db/migration/V1__init_schema.sql).

## Prérequis

- JDK 21
- Gradle (ou le wrapper, une fois généré avec `gradle wrapper`)
- PostgreSQL 14+ en local (ou via Docker)

## Démarrage local

```bash
# 1. Créer la base locale
createdb comparateur

# 2. Variables d'environnement (ou valeurs par défaut dans application.yml)
export DB_USER=comparateur
export DB_PASSWORD=comparateur

# 3. Lancer l'application (les migrations Flyway s'exécutent automatiquement au démarrage)
./gradlew bootRun
```

L'API démarre sur `http://localhost:8080`.

## Structure

```
src/main/java/io/pragmatic/ponderus/
  ponderusApplication.java
  domain/              -> entités JPA (User, Project, ComparisonOption, Criterion, Score)
src/main/resources/
  application.yml
  db/migration/         -> scripts Flyway
```

## Avancement

- [x] Squelette du projet + entités JPA
- [x] Schéma initial (migration Flyway)
- [ ] Sécurité : vérification des tokens Firebase + CORS
- [ ] CRUD `Project` (controller / service / repository)
- [ ] Endpoints `Option` / `Criterion` / `Score`
- [ ] Isolation multi-tenant (filtrage systématique par `user_id`)
- [ ] Facturation Stripe (webhooks)
- [ ] Export PDF / partage lecture seule

## Déploiement

Conteneurisé (JAR Spring Boot) et déployé sur Google Cloud Run.
