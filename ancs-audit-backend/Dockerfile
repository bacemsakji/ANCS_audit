# ============================================================
# ANCS Audit Backend — Dockerfile (multi-stage)
# Stage 1 : Build Maven (JDK 21)
# Stage 2 : Runtime JRE Alpine (image minimale)
# ============================================================

# ---- Stage 1 : Build ----------------------------------------
FROM maven:3.9.8-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copier pom.xml séparément pour profiter du cache des layers Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le source et construire
COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2 : Runtime --------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="ANCS — Agence Nationale de Cybersécurité <contact@ancs.gov.tn>"
LABEL version="1.0.0"
LABEL description="ANCS Security Audit Platform — Backend API"

# Utilisateur non-root pour la sécurité
RUN addgroup -S ancs && adduser -S ancs -G ancs

WORKDIR /app

# Copier le JAR depuis le stage de build
COPY --from=builder /build/target/*.jar app.jar

# Répertoire pour les secrets (service account Firebase, etc.)
RUN mkdir -p /secrets && chown ancs:ancs /secrets

# Répertoire pour les logs
RUN mkdir -p /app/logs && chown ancs:ancs /app/logs

USER ancs

EXPOSE 8080

# Options JVM optimisées pour les conteneurs
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} -jar app.jar"]
