## 1. Gitea Setup Steps (One-time)

Run `docker compose up gitea-db gitea-server`:

1. **Open** `http://localhost:3000`
2. **Run install wizard** → set admin account
3. **Create organization:** `bioinformatics`
4. **Create repository:** `config-repo` (initialize with README)
5. **Create a user** `config-user` (or use your admin) and generate an **Access Token** with `repo` scope
6. **Clone locally, add `YAML`files , push:**

```bash
git clone http://localhost:3000/bioinformatics/config-repo.git
cd config-repo
# ... create application.yml, bioinformatics-dashboard.yml, etc ...
git add .
git commit -m "Initial config"
git push origin main
```

### Audit service configuration

Publish `audit-service.yml`, `audit-service-dev.yml`, and `audit-service-prod.yml`
to the config repository alongside `application.yml`. Config Server combines
the shared configuration, the service configuration, and the active profile's
overrides for the `audit-service` application.

The base configuration uses the `audit_log` database schema, Flyway migrations
from `classpath:migration`, and the `audit-logger` Kafka consumer group with
manual acknowledgements. Development uses a random port; production uses
port `8089` and requires `SPRING_DATA_REDIS_HOST`, `REDIS_PASSWORD`, and
`EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE`. Database connection settings remain
in the service's environment-backed local configuration.

### Servlet API rate limits

Publish the updated `application.yml` and all six service base files (dashboard,
auth, analytics, import, export and audit) together with the shared-starter/controller
changes. `application.yml` supplies `app.rate-limiter.enabled` and the default
`global` policy; each service file supplies its own complete `endpoints` list.
Spring replaces lists rather than merging their entries, so a profile-specific
`endpoints` override must include every named policy used by that service.
Development and production currently inherit the same policies.

| Service | API group / policy | Capacity and refill tokens per 60 seconds |
|---------|--------------------|------------------------------------------|
| All servlet services | Default `global` (saved filters; import listing; export listing, detail, deletion and field metadata) | 100 |
| dashboard | Gene list/search/count: `search` | 30 |
| dashboard | Gene detail: `detail` | 60 |
| dashboard | Typeahead: `autocomplete` | 120 |
| dashboard | Legacy CSV export: `export` | 10 |
| auth-service | `login` | 10 |
| auth-service | `refresh` | 20 |
| auth-service | `logout` | 30 |
| auth-service | `change-password`, `service-token` (separate budgets) | 5 each |
| import-service | File/remote job starts: `import` | 5 |
| import-service | Job polling: `status` | 120 |
| export-service | Creation/retry: `export` | 10 |
| export-service | Download URL/file: `download` | 20 |
| export-service | Job polling: `status` | 120 |
| analytics-service | Cached chart/KPI fan-out: `analytics` | 120 |
| analytics-service | Filtered chart fan-out: `analysis` | 60 |
| analytics-service | Two-subset comparison: `compare` | 10 |
| audit-service | Admin audit-history queries: `search` | 30 |

Budgets are shared by all methods with the same policy key, per client IP and
service instance. Polling, typeahead and chart fan-out get larger budgets than
job creation, credential checks and comparisons. Gene count uses `search`, not
`export`, so previewing a count does not consume an export budget. Capacity is
the initial burst; refill is greedy, so these are not fixed-window maxima.
`global` is only the default annotation key, not a second cumulative cap.

Set `APP_RATE_LIMITER_ENABLED=false` to disable servlet throttling. Override any
policy with `APP_RATE_LIMITER_<POLICY>_CAPACITY`, `_TOKENS` and `_SECONDS`, replacing
hyphens with underscores (for example `APP_RATE_LIMITER_CHANGE_PASSWORD_CAPACITY`).
Overrides are resolved in each service process. Defaults and policy names must
remain consistent with its controller annotations; unknown names fail explicitly.
No registration endpoint exists, so the former unused `register` policy is removed.

The common starter enforces these limits locally using Bucket4j and returns the
standard HTTP 429 body on exhaustion. Buckets are retained in memory until restart;
limits are not distributed across replicas. Gateway Redis limits are unchanged.
The servlet remote address is the client key: deployments behind a gateway/proxy
must establish trusted-proxy handling to avoid sharing one proxy IP budget; the
aspect does not parse untrusted forwarding headers. Config publishing and service
restart/rollout remain deployment steps; editing these files alone does not update
the Config Server Git backend or running services.

### `.env` additions

Add these to your existing `.env` file:

```bash
# ── Gitea ────────────────────────────────────────────────────────────────────
GITEA_DB_PASSWORD=gitea_secure_password
GITEA_HTTP_PORT=3000
GITEA_SSH_PORT=222

# Gitea config-repo access (create this user/token in Gitea UI first)
GITEA_CONFIG_USER=config-user
GITEA_CONFIG_PASSWORD=your-gitea-token-or-password

# ── Config Server ────────────────────────────────────────────────────────────
CONFIG_SERVER_PORT=8888
CONFIG_ENCRYPT_KEY=ChangeMeInProduction-32CharKey!!

# ── Backend Profile ──────────────────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=dev
```