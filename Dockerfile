# syntax=docker/dockerfile:1

# ---- Stage 1: build ---------------------------------------------------
# Image JDK complete pour compiler ; jamais presente dans l'image finale.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Copie du wrapper + fichiers de config Gradle seuls d'abord : ce layer
# ne change que si les dependances changent, ce qui maximise le cache Docker.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew --no-daemon dependencies

# Le code source change souvent : on le copie apres pour ne pas invalider
# le cache des dependances ci-dessus.
COPY src src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon bootJar -x test

# Extraction des layers Spring Boot (dependances / ressources / classes
# separees) pour un cache Docker plus fin sur les futurs builds.
# Spring Boot 4 renomme le jarmode "layertools" en "tools", et le glob doit
# exclure le jar "-plain.jar" (sans manifest executable) genere a cote.
RUN JAR=$(find build/libs -name '*.jar' ! -name '*-plain.jar') && \
    java -Djarmode=tools -jar "$JAR" extract --launcher --layers --destination /workspace/extracted

# ---- Stage 2: runtime ---------------------------------------------------
# JRE seule (pas de JDK) sur base minimale : image finale plus petite et
# surface d'attaque reduite.
FROM eclipse-temurin:21-jre-jammy AS runtime

# Utilisateur non-root : bonne pratique de securite pour Cloud Run.
RUN groupadd --gid 1000 spring && \
    useradd --uid 1000 --gid spring --shell /bin/false --create-home spring
USER spring
WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/extracted/dependencies/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/spring-boot-loader/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/application/ ./

# Cloud Run injecte $PORT (8080 par defaut) : Spring Boot n'ecoute pas sur
# cette variable nativement, donc on la reinjecte explicitement au demarrage.
ENV PORT=8080
EXPOSE 8080

# -XX:MaxRAMPercentage laisse la JVM s'adapter automatiquement a la memoire
# allouee au service Cloud Run (au lieu d'une taille de heap fixe en dur).
# JarLauncher (et non java -jar) car on a extrait les layers ci-dessus.
# Forme JSON + "exec" : garde une gestion correcte des signaux (SIGTERM
# de Cloud Run) tout en permettant l'expansion de la variable $PORT.
ENTRYPOINT ["sh", "-c", "exec java \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Dserver.port=${PORT} \
    -Djava.security.egd=file:/dev/./urandom \
    org.springframework.boot.loader.launch.JarLauncher"]
