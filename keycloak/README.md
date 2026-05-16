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

### Users

| Username | Password | Role |
|---|---|---|
| `alice` | `alice123` | `executive` |
| `bob` | `bob123` | `hr` |
| `carol` | `carol123` | `manager` |
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
docker compose down -v   # wipes the volume
docker compose up -d     # fresh start — realm is re-imported automatically
```

## Stop

```bash
docker compose down        # stops containers, keeps volume
docker compose down -v     # stops containers AND deletes volume (re-import on next start)
```
