#
# Multi-stage build: compila com Maven (Java 17) e roda com JRE leve.
#

# ---------- Build ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache de dependências
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -e -DskipTests dependency:go-offline

# Código fonte
COPY src ./src

# Build
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package spring-boot:repackage

# ---------- Runtime ----------
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Copia o JAR gerado
COPY --from=build /app/target/*-SNAPSHOT.jar /app/app.jar

# Porta padrão (pode ser sobrescrita via SERVER_PORT)
EXPOSE 9090

# Healthcheck simples (ajuste a URL conforme necessário)
HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
  CMD wget -qO- http://127.0.0.1:${SERVER_PORT:-9090}/actuator/health | grep -q '"status":"UP"' || exit 1

# Em produção, passe variáveis via `-e` ou `--env-file`. O .env é opcional.
ENTRYPOINT ["java","-jar","/app/app.jar"]


