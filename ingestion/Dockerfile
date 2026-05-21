# Build context must be the repo root:
#   docker build -f ingestion/Dockerfile -t rag-ingestion .
#
# Runtime profile is controlled by the SPRING_PROFILES_ACTIVE env var:
#   local  → REST ingest endpoint on port 8081
#   aws    → SQS listener mode (no HTTP server needed)

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Root POM must be present so module POMs can resolve rag-parent via relativePath
COPY pom.xml .

# 1. Install common into the local m2 cache so ingestion can resolve rag-common
COPY common/pom.xml common/
COPY common/src    common/src
RUN mvn -f common/pom.xml install -DskipTests -q

# 2. Resolve ingestion dependencies as a separate layer (cache busts only on pom change)
COPY ingestion/pom.xml ingestion/
RUN mvn -f ingestion/pom.xml dependency:go-offline -q

# 3. Build the fat JAR
COPY ingestion/src ingestion/src
RUN mvn -f ingestion/pom.xml package -DskipTests -q

# ---- runtime stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /build/ingestion/target/rag-ingestion-*.jar app.jar

# Port is only reachable on the local profile; aws profile uses SQS — no HTTP needed
EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]