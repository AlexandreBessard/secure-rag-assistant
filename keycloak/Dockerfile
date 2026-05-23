# Build context: keycloak/
#   docker build -f keycloak/Dockerfile -t rag-keycloak keycloak/

FROM --platform=linux/amd64 quay.io/keycloak/keycloak:26.1.5

# Bake the realm so --import-realm finds it at startup
COPY rag-realm.json /opt/keycloak/data/import/rag-realm.json

# Pre-optimise the runtime to reduce Fargate cold-start time
RUN /opt/keycloak/bin/kc.sh build

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
