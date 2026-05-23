# Build context must be the repo root:
#   docker build -f tools/Dockerfile -t rag-tools .

# ---- build stage ----
FROM --platform=linux/amd64 maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# Root POM must be present so module POMs can resolve rag-parent via relativePath
COPY pom.xml .
RUN mvn -f pom.xml -N install -q

# 1. Install common into the local m2 cache so tools can resolve rag-common
COPY common/pom.xml common/
COPY common/src    common/src
RUN mvn -f common/pom.xml install -DskipTests -q

# 2. Resolve tools dependencies as a separate layer (cache busts only on pom change)
COPY tools/pom.xml tools/
RUN mvn -f tools/pom.xml dependency:go-offline -q

# 3. Build the fat JAR
COPY tools/src tools/src
RUN mvn -f tools/pom.xml package -DskipTests -q

# ---- runtime stage ----
FROM --platform=linux/amd64 eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /build/tools/target/rag-tools-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
