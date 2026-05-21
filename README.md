# Secure RAG Assistant

A proof-of-concept for a secure Retrieval-Augmented Generation (RAG) assistant with role-based document access, built on Spring Boot, Angular, and AWS Bedrock.

**AI model:** `eu.anthropic.claude-haiku-4-5-20251001-v1:0` via AWS Bedrock Converse API (eu-west-3)  
**Embeddings:** Amazon Titan Embed Text V2 (1024 dimensions)

---

## Prerequisites

- Docker and Docker Compose
- Java 17+, Maven (or use the included `mvnw` wrapper)
- Node.js 20+ and npm
- AWS credentials with access to Bedrock and S3 (`AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`)

---

## Quick start

### 1. Start infrastructure services

```bash
docker compose up -d
```

Starts **Keycloak** (http://localhost:8180) and **pgvector/PostgreSQL** (port 5433).  
Keycloak auto-imports the `rag-assistant` realm with test users on first boot.

### 2. Start the backend

```bash
cd backend
./mvnw spring-boot:run
# → http://localhost:8080
```

### 3. Start the ingestion service (optional — needed to seed documents)

```bash
cd ingestion
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# → http://localhost:8081

# Ingest a PDF
curl -X POST http://localhost:8081/ingest \
  -F "file=@/path/to/doc.pdf" \
  -F "requiredRole=employee" \
  -F "documentId=my-doc"
```

### 4. Start the MCP tools service

```bash
cd tools
./mvnw spring-boot:run
# → http://localhost:8082
```

### 5. Start the frontend

```bash
cd frontend
npm install
npm start
# → http://localhost:4200
```

---

## Test users

| User | Password | Roles |
|------|----------|-------|
| alice | alice123 | executive, admin |
| bob | bob123 | hr, admin |
| carol | carol123 | manager, admin |
| dave | dave123 | employee |

Admin users can upload documents via the UI. Role determines which documents are retrievable.

---

## Observability

| Service | URL |
|---------|-----|
| Keycloak admin | http://localhost:8180 — `admin` / `admin` |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 — `admin` / `admin` |
| Jaeger (traces) | http://localhost:16686 |

---

## Project structure

```
backend/     Spring Boot 3.5 — RAG query API, JWT auth, role-based retrieval
frontend/    Angular 21 — chat UI, document upload, Keycloak OIDC login
ingestion/   Spring Boot 3.5 — PDF → chunks → pgvector (local REST or AWS SQS)
tools/       Spring Boot 3.5 — MCP tool server (LLM-callable tools via SSE)
infra/       Terraform — AWS ECS, RDS, S3, SQS, networking
keycloak/    Realm configuration and test users
```
