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

The gateway routes `/api/v1/admin/audit/**` directly to `lb://audit-service`,
without rewriting the audit controller's path. The route uses the shared Redis
rate limiter and `auditServiceCircuitBreaker`; requests still require authentication
and the audit controller's `ADMIN` role. Publish the updated `api-gateway.yml`
to the config repository and restart the gateway to activate the route.

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

### Compose wiring for shared infrastructure

The root `.env.example` includes every `APP_RATE_LIMITER_*` override referenced by
these files. Copy the relevant additions into an existing local `.env`; Compose
passes that file to each business service. The `common-application-env` anchor
provides the limiter switch and `COMMON_KAFKA_BOOTSTRAP_SERVERS` (default:
`kafka:29092`, the Docker-internal listener) to dashboard, auth, analytics, import,
export and audit. Spring Kafka and topic initialization use the same broker address. Each service already
sets a distinct `SPRING_APPLICATION_NAME`, which identifies its shared audit events;
no duplicate service-local publisher implementation is needed.

All six services wait for `kafka-init-topics` to complete successfully, including
creation of `audit.events.log` with six partitions. Topic initialization fails
immediately if a creation command fails, rather than reporting false success.
Publishing remains asynchronous and best-effort; this startup dependency is not
a durable-delivery guarantee.

Optional `APP_AUDIT_POOL_CORE_SIZE`, `APP_AUDIT_POOL_MAX_SIZE`,
`APP_AUDIT_POOL_QUEUE_CAPACITY` and `APP_AUDIT_POOL_THREAD_NAME_PREFIX` overrides
are documented as comments in `.env.example`. Leave them unset to retain each
service's Config Server pool sizes/prefix. Uncommenting them in `.env` overrides
the corresponding setting for all six business services. Compose deliberately
uses the `prod` Spring profile for container network settings; the example's
`SPRING_PROFILES_ACTIVE=dev` is for locally run applications.

### Kubernetes wiring for shared infrastructure

All six business Deployments import
`devops/kubernetes/common/application-configmap.yaml`
(`bio-common-application-config`) in addition to `bio-common-config`.
It supplies `COMMON_KAFKA_BOOTSTRAP_SERVERS=kafka:29092` for the shared audit
publisher/consumer and `APP_RATE_LIMITER_ENABLED=true` for servlet throttling.
Spring Kafka and the topic Job use the same in-cluster listener; do not use
`kafka:9092`, whose broker metadata advertises `localhost:9092`.
Infrastructure workloads, including the reactive gateway, do not import the
business-service ConfigMap.

Each service ConfigMap sets its own `SPRING_APPLICATION_NAME`, which becomes the
audit event's source service. Audit pool settings and the complete named
rate-limit policies remain in the Config Server files above. Kubernetes does not
load the root `.env`: add `APP_RATE_LIMITER_*` or `APP_AUDIT_POOL_*` overrides to a
service's ConfigMap (or the shared application ConfigMap for a cross-service
override). Keep service-specific rate-limit endpoint lists in Config Server
rather than duplicating them in Kubernetes.

`devops/kubernetes/audit-service/` provides the Deployment, ClusterIP Service on
`8089`, ConfigMap and placeholder database Secret. It uses PostgreSQL
primary/replica routing, the `audit-logger` consumer group, manual acknowledgements,
Redis credentials from `redis-secret`, shared JWT configuration, health probes,
resource requests/limits and ephemeral logs, consistent with the other services.
Replace placeholder Secrets locally before deployment; never commit live values.
Publish the audit service configuration to Gitea before starting the pod.

Build the JAR and local image from the repository root, then load it into Minikube:

```bash
mvn -f ./backend/pom.xml -pl services/audit-service -am package -DskipTests
docker build -t bio-dashboard/audit-service:local \
  -f devops/docker/backend/services/audit-service/Dockerfile backend/services/audit-service
minikube image load bio-dashboard/audit-service:local
```

Kubernetes does not implement Compose's `depends_on`. With the existing namespace,
PostgreSQL primary/replica, Redis, Kafka, discovery and Config Server ready, provision
topics before rolling out any business service:

```bash
kubectl apply -f devops/kubernetes/common/
# On later runs, delete the immutable Job before re-applying it.
kubectl -n bio-dashboard delete job kafka-init-topics --ignore-not-found
kubectl apply -f devops/kubernetes/kafka-init-topics/
kubectl -n bio-dashboard wait --for=condition=complete job/kafka-init-topics --timeout=600s
kubectl apply -f devops/kubernetes/audit-service/
kubectl -n bio-dashboard rollout status deployment/audit-service --timeout=300s
```

Apply the updated manifests for auth, analytics, dashboard, import and export as
well. Updating an environment-backed ConfigMap does not restart existing pods:
after applying changes, explicitly roll out the six business Deployments to pick
them up. Check that the topic Job completed and `audit.events.log` exists with six
partitions before sending audited requests. Publication remains asynchronous and
best-effort, not a durable-delivery guarantee.

The existing servlet budgets remain per pod and use the immediate peer's IP;
requests behind the gateway/proxy can share that peer's budget unless trusted-proxy
handling is deliberately configured. Scaling replicas does not create a shared
servlet budget. The gateway's separate Redis-backed route limits retain their
existing Redis Secret wiring and behavior.

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