# Keycloak

Local Keycloak instance for development, running via Docker Compose at the repo root.

## Start

```bash
docker compose up -d
```

Admin console: `http://localhost:8180` — credentials: `admin` / `admin`

Keycloak automatically imports `rag-realm.json` on first start (via `--import-realm`). The realm,
roles, client, and users are all created with no manual steps.

## Realm: `rag-assistant`

### Roles & document access

```
┌───────────────────────────────────┬───────────┬─────┬─────────┬──────────┐
│             Document              │ executive │ hr  │ manager │ employee │
├───────────────────────────────────┼───────────┼─────┼─────────┼──────────┤
│ Company handbook / general FAQ    │    ✅     │ ✅  │   ✅    │    ✅    │
├───────────────────────────────────┼───────────┼─────┼─────────┼──────────┤
│ IT & office policies              │    ✅     │ ✅  │   ✅    │    ✅    │
├───────────────────────────────────┼───────────┼─────┼─────────┼──────────┤
│ Team performance reports          │    ✅     │ ✅  │   ✅    │    ❌    │
├───────────────────────────────────┼───────────┼─────┼─────────┼──────────┤
│ Project budgets / headcount plans │    ✅     │ ✅  │   ✅    │    ❌    │
├───────────────────────────────────┼───────────┼─────┼─────────┼──────────┤
│ Salary bands / compensation grids │    ✅     │ ✅  │   ❌    │    ❌    │
├───────────────────────────────────┼───────────┼─────┼─────────┼──────────┤
│ Individual performance reviews    │    ✅     │ ✅  │   ❌    │    ❌    │
├───────────────────────────────────┼───────────┼─────┼─────────┼──────────┤
│ Strategic roadmap / board deck    │    ✅     │ ❌  │   ❌    │    ❌    │
├───────────────────────────────────┼───────────┼─────┼─────────┼──────────┤
│ M&A / financial forecasts         │    ✅     │ ❌  │   ❌    │    ❌    │
└───────────────────────────────────┴───────────┴─────┴─────────┴──────────┘
```

### Upload access (`admin` role)

`executive`, `hr`, and `manager` users also carry the `admin` role, which grants access to the
document upload endpoint (`POST /upload`). `employee` users cannot upload.

### Users

| Username | Password | Roles |
|---|---|---|
| `alice` | `alice123` | `executive`, `admin` |
| `bob` | `bob123` | `hr`, `admin` |
| `carol` | `carol123` | `manager`, `admin` |
| `dave` | `dave123` | `employee` |

### Client

| Field | Value |
|---|---|
| Client ID | `rag-frontend` |
| Type | Public (no secret) |
| Redirect URI | `http://localhost:4200/*` |
| Web Origins | `http://localhost:4200` |

## Re-importing after changes to `rag-realm.json`

The realm is only imported once (on a fresh volume). To re-import after editing the JSON:

```bash
docker compose stop keycloak
docker compose rm -f keycloak
docker volume rm secure-rag-assistant_keycloak_data
docker compose up -d keycloak
```

## Troubleshooting: "HTTPS required" error

**Cause.** Keycloak's master realm defaults to `sslRequired: external`, which blocks plain HTTP
requests from any IP that is not `127.0.0.1`. This is triggered when traffic reaches Keycloak
through a VPN or proxy (e.g. Cloudflare WARP) — even for `localhost` URLs in the browser, the
client IP seen inside the Docker container becomes an external address.

**Why the env vars alone are not enough.** `KC_HTTP_ENABLED`, `KC_HOSTNAME_STRICT`, and
`KC_HOSTNAME_STRICT_HTTPS` control Keycloak's HTTP listener and hostname validation, but they do
not override the `sslRequired` setting persisted in the master realm database inside the volume.

**Fix — update the master realm via `kcadm.sh` from inside the container:**

```bash
docker exec <keycloak-container-name> /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin

docker exec <keycloak-container-name> /opt/keycloak/bin/kcadm.sh update realms/master \
  -s sslRequired=none
```

This takes effect immediately without a restart. The change is persisted in the volume, so it
survives container restarts (but not a volume wipe).

The `docker-compose.yml` also sets `KC_HTTP_ENABLED: "true"`, `KC_HOSTNAME_STRICT: "false"`, and
`KC_HOSTNAME_STRICT_HTTPS: "false"` as a complementary measure for future fresh starts.

## Stop

```bash
docker compose down        # stops containers, keeps volume
docker compose down -v     # stops containers AND deletes volume (re-import on next start)
```
