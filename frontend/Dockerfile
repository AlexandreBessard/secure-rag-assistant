# Build context: frontend/
#   docker build \
#     --build-arg KEYCLOAK_URL=http://<alb-dns>:8180 \
#     -f frontend/Dockerfile \
#     -t rag-frontend \
#     frontend/

# ---- build stage ----
FROM --platform=linux/amd64 node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

# Copy source first, then inject — sed must run on the actual file
COPY . .
ARG KEYCLOAK_URL=http://localhost:8180
RUN sed -i "s|url: ''|url: '${KEYCLOAK_URL}'|" src/environments/environment.prod.ts
RUN npm run build

# ---- runtime stage ----
FROM --platform=linux/amd64 nginx:1.27-alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Angular 17+ outputs to dist/<project-name>/browser/
COPY --from=builder /app/dist/frontend/browser/ /usr/share/nginx/html/

COPY nginx.conf.template /etc/nginx/templates/nginx.conf.template
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80

ENTRYPOINT ["/docker-entrypoint.sh"]
