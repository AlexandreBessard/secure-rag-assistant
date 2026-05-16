# Keycloak

Local Keycloak instance for development, running via Docker Compose at the repo root.

## Start

```bash
docker compose up -d
```

Admin console: `http://localhost:8180` — credentials: `admin` / `admin`

## First-time setup

After the very first start (or after running `docker compose down -v`), wait for Keycloak to fully start, then run:

```bash
docker compose exec keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin

docker compose exec keycloak /opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE
```

This disables the HTTPS requirement on the master realm. The setting is persisted in the named volume (`keycloak_data`) and survives restarts — you only need to do this once.

## Stop

```bash
docker compose down        # stops containers, keeps volume (no setup needed on next start)
docker compose down -v     # stops containers AND deletes volume (first-time setup required again)
```
