# OPS-002 — Implementation Journal

---

## 2026-09-30

### Step 1 — Analysis

- Read `docker-compose.yml` (19 services, 8 named volumes), `devops/scripts/run-all-docker.sh` (startup order),
  all Dockerfiles under `devops/docker/`, `.env.example`, `devops/docker/nginx/nginx.conf`.
- Listed every `${ENV_VAR}` referenced by `gitea/config-repo/*.yml` and by the services' embedded `application.yaml`
  to make sure each variable previously injected through compose `env_file: .env` is provided by a ConfigMap or a
  Secret in Kubernetes (e.g. `SPRING_DATASOURCE_*` for import/export services, `APP_JWT_SECRET`, `LOG_PATH`).
- Findings:
  - Hostnames in config-repo (`postgres`, `postgres-replica`, `redis`, `kafka`, `zipkin`, `discovery-server`,
    `gitea-server`) → Kubernetes Service names must be identical.
  - `eureka.instance.prefer-ip-address: true` is already set everywhere → pods register with their IP (required in
    K8s).
  - All Spring services permit `/actuator/health/**` and enable health probe groups → usable for K8s probes.
  - Compose `postgres` patches `pg_hba.conf` in a background sub-shell; replaced by a cleaner
    `/docker-entrypoint-initdb.d` init script.
  - Dockerfiles run as non-root `bioapp`; images have no tag → must be tagged explicitly (`:local`) for Minikube.
  - The `backend` compose service is `dashboard` for Spring/Eureka → renamed `dashboard` in Kubernetes.
- No ambiguity blocking the work → no `analyse.md` required. Assumptions recorded in `overview.md` / `plan.md`.

### Step 2 — Ticket documentation

- Created `overview.md` (description, service mapping, acceptance criteria, out of scope) and `plan.md`
  (architecture, tasks, checklist, risks).

### Step 3 — Namespace & shared configuration

- `devops/kubernetes/namespace.yaml` → namespace `bio-dashboard`.
- `devops/kubernetes/common/configmap.yaml` (`bio-common-config`) → port of the compose `x-common-spring-env` anchor
  (profile, config-server import, Eureka, Redis host/port, Kafka, Zookeeper discovery, Zipkin, `LOG_PATH`,
  JWT lifetimes, `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED`).
- `devops/kubernetes/common/secret.yaml` (`bio-common-secret`) → `APP_JWT_SECRET` placeholder.
- Note: the IDE workspace index referenced an older draft of `devops/kubernetes/` (e.g. `kafka/deployment.yaml~`), but
  no such file existed on disk (`find devops/kubernetes` only returned the files created in this step, untracked in
  Git). All manifests are therefore written from scratch.

### Step 4 — Data stores

- `postgres/` → `configmap.yaml` (`POSTGRES_DB`, `PGDATA` sub-directory + `postgres-init-scripts` with
  `01-enable-replication.sh`), `secret.yaml` (`POSTGRES_USER`/`POSTGRES_PASSWORD`), `pvc.yaml` (10Gi),
  `deployment.yaml` (`wal_level=replica`, `pg_isready` probes, `Recreate`), `service.yaml` (ClusterIP 5432).
- `postgres-replica/` → `configmap.yaml` (primary host/port + `start-replica.sh` bootstrap script ported from compose),
  `pvc.yaml` (10Gi), `deployment.yaml` (startup probe up to 10 min for `pg_basebackup`), `service.yaml`.
  No own Secret: credentials are read from `postgres-secret` (single source of truth for rotation).
- `redis/` → `configmap.yaml` (`redis.conf`), `secret.yaml` (`REDIS_PASSWORD`), `pvc.yaml` (1Gi),
  `deployment.yaml` (`--requirepass` from Secret, `redis-cli ping` probes), `service.yaml`.

### Step 5 — Messaging & observability

- `zipkin/` → ConfigMap (in-memory storage, heap), Deployment (`/health` probes), Service NodePort `30941`.
- `zookeeper/` → ConfigMap (client port, tick time, `ruok` whitelist), Deployment (`ruok` readiness), Service 2181.
- `kafka/` → ConfigMap (explicit `KAFKA_LISTENERS`, advertised `kafka:29092` kept identical to compose),
  Deployment (startup TCP probe, `kafka-topics --list` readiness), Service exposing 29092 and 9092.
- `kafka-init-topics/` → ConfigMap (topic list `topic:partitions:rf` + idempotent `create-topics.sh`) and a **Job**
  (`backoffLimit: 6`, `activeDeadlineSeconds: 600`) replacing the compose one-shot container.
- `kafka-ui/` → ConfigMap, Deployment (image pinned to `v0.7.2` instead of `latest`), Service NodePort `30090`.
- `enableServiceLinks: false` on every pod — mandatory for Confluent images (`KAFKA_PORT=tcp://…` injection).

### Step 6 — Platform services

- `discovery-server/` → ConfigMap (standalone Eureka: no self-registration), Deployment (image
  `bio-dashboard/discovery-server:local`, actuator liveness/readiness + startup probe), Service NodePort `30761`.
- `gitea-db/` → ConfigMap (`POSTGRES_DB=gitea`, `PGDATA`), Secret (`gitea-db-secret`), PVC 2Gi, Deployment, Service.
- `gitea-server/` → ConfigMap (`GITEA__*` settings), PVC 5Gi, Deployment (image pinned `gitea/gitea:1.22`, DB
  credentials via `secretKeyRef` → `gitea-db-secret`), Service NodePort `30300` (HTTP) / `30222` (SSH).
- `config-server/` → ConfigMap (Git URI, Eureka), Secret (Git username/token, `ENCRYPT_KEY`), Deployment, Service 8888.
  Deliberately **not** wired to `bio-common-config` (its `SPRING_CONFIG_IMPORT` would make config-server import from
  itself).
- `api-gateway/` → ConfigMap, Deployment (`bio-common-config` + `bio-common-secret` + Redis password from
  `redis-secret`), Service NodePort `30080`. No own Secret (JWT secret is shared).

### Step 7 — Business services

- `auth-service/`, `analytics-service/`, `dashboard/`, `import-service/`, `export-service/` → each has
  `configmap.yaml` (port, application name, JDBC URLs, service-specific flags), `secret.yaml` (service-owned DB
  credentials — `SPRING_DATASOURCE_*` and/or `COMMON_DATASOURCE_*`), `deployment.yaml`, `service.yaml` (ClusterIP).
- Common wiring for every Spring pod: `envFrom` `bio-common-config` → `bio-common-secret` → own ConfigMap → own
  Secret (later sources win), Redis password via `secretKeyRef` → `redis-secret`, `emptyDir` on `/app/logs`,
  startup (5–6 min budget) + readiness + liveness probes on actuator health groups.
- `dashboard` = compose `backend` (renamed, see Step 1); memory limit 1.5Gi (Dockerfile uses `MaxRAMPercentage=75`).
- `import-service/pvc.yaml` (`import-uploads`, 10Gi → `/app/bio-import`) and `export-service/pvc.yaml`
  (`export-data`, 5Gi → `/app/bio-export`); both Deployments use `Recreate` (RWO volumes).
- Variables previously inherited implicitly through compose `env_file: .env` are now explicit (`APP_BATCH_CHUNK_SIZE`,
  `UNIPROT_API_BASE_URL`, `APP_IMPORT_CONFIG_TEMP_DIR`, `APP_EXPORT_TEMP_DIR`, multipart sizes).

## 2026-10-26

### Business-service verification

- Reviewed all five business-service manifest sets against Compose, Dockerfiles, Spring configuration, and the
  OPS-002 checklist. No business-manifest changes were needed.
- Verified common and service-owned ConfigMap/Secret injection, both Redis password variables referencing
  `redis-secret`, matching Service selectors/ports, local image tags/pull policies, resource bounds,
  `enableServiceLinks: false`, actuator probes, and `/app/logs` backed by `emptyDir`.
- Confirmed import/export PVC references, mount paths, `ReadWriteOnce` access, and `Recreate` rollout strategy.
- Reconciled the plan's stale task statuses with the existing manifest implementation. Completion here means
  manifests are implemented and statically checked, not that the applications have run successfully on Kubernetes.

### Step 8 — Frontend manifests

- Added `frontend/deployment.yaml`: one nginx container using `bio-dashboard/frontend:local` with
  `imagePullPolicy: IfNotPresent`, port 80 named `http`, and `enableServiceLinks: false`.
- HTTP startup/readiness/liveness probes use `/`; CPU/memory requests are `50m`/`64Mi`, limits `250m`/`128Mi`.
- Added `frontend/service.yaml`: NodePort `30000` → port 80 → named container port `http`.
- Reused the image's nginx configuration without changes: `/api/` proxies to `api-gateway:8080/api/`.
  Deploy after the gateway Service exists and the gateway is ready; nginx resolves its upstream at startup.
- The frontend Dockerfile is runtime-only: run `npm ci` and `npm run build:prod` in `frontend/` first,
  then build image `bio-dashboard/frontend:local` with `devops/docker/frontend/Dockerfile` from the repository-root
  context. The image must be available in Minikube before rollout. No frontend ConfigMap, Secret, or PVC is needed.

### Validation performed

- PyYAML parsed all **74 Kubernetes YAML files / 75 unique resources** successfully.
- Programmatic assertions passed for all five business services and frontend: selectors, named ports, image
  conventions, probes, resources, referenced ConfigMaps/Secrets and their keys, and referenced PVCs.
- Business-service assertions additionally checked `envFrom` order, Redis credential sources, log volumes, and
  import/export storage. Frontend checks confirmed NodePort `30000`, gateway upstream port 8080, and uniqueness
  of all seven configured NodePorts.
- IDE diagnostics reported no errors in the new frontend manifests.
- At this initial static-review stage, Kubernetes schema validation, frontend/image builds, deployment, and live
  smoke tests had **not** been performed. The subsequent live rollout is recorded below.

### Live Minikube rollout

- Reused the existing Docker-driver `minikube` profile and explicitly targeted Kubernetes context `minikube`,
  namespace `bio-dashboard`; no unrelated Kubernetes context or workload was changed.
- Increased the running Minikube Docker container's actual allocation from 2 CPUs / approximately 10 GiB to
  6 CPUs / 12 GiB without stopping Kubernetes. This was a runtime Docker update, not a persisted Minikube profile
  resize. The backing filesystem had sufficient free space for the seven PVCs (43 GiB requested in total).
- Rebuilt the backend reactor with Maven (`clean package -DskipTests`); test execution was skipped.
  Installed locked frontend dependencies and built the Angular production bundle. Retried the frontend build
  with `CI=true` and `NG_CLI_ANALYTICS=false` for noninteractive operation.
- Built all nine runtime application images as `bio-dashboard/<service>:local` and loaded them into Minikube's
  containerd image store with `minikube image load`.
- Generated runtime DB, Redis, JWT, Git, and encryption credentials directly in Kubernetes, keeping application
  DB credentials consistent with PostgreSQL. No generated secret values were written to repository files or
  printed. Checked-in `secret.yaml` files remain placeholders and must not be reapplied over runtime Secrets.
- All 75 resources passed `kubectl apply --dry-run=server` validation before deployment.
- Initialized Gitea noninteractively using runtime ConfigMap overrides (`INSTALL_LOCK=true`, registration
  disabled), created a bootstrap administrator using the CLI, and seeded a private
  `bioinformatics/config-repo` repository on branch `main` with all 18 source YAML configuration files.
  Config Server authenticated with credentials from `config-server-secret`.
- Resolved two observed startup dependencies:
  - Kafka's controller could not reach its advertised `kafka:29092` address because the Service withheld unready
    endpoints. Added `publishNotReadyAddresses: true` to `kafka/service.yaml`; Kafka then became ready.
  - Auth initially failed shared JPA validation with missing `public.cross_reference`. Starting dashboard applied
    its public-schema migrations and allowed auth to recover on restart. Dashboard must precede both auth and
    analytics on a fresh database.
- The initial topic Job exceeded its deadline. Recreated only that idempotent Job after the broker was healthy;
  the retry completed successfully and retained the existing topics.
- All **18 Deployments** reached **1/1 ready/available**; `kafka-init-topics` completed **1/1**.
- Windows refused the initial localhost port-forward binding on 30000. Left a localhost-only forward running on
  **http://localhost:18080** → `svc/frontend:80`; the Service's NodePort remains 30000.
  Restart access if needed with
  `kubectl --context minikube -n bio-dashboard port-forward --address=127.0.0.1 svc/frontend 18080:80`.

### Live smoke-test results

- Frontend `/`, SPA deep-link `/genes`, and a production JavaScript asset returned HTTP **200**.
- A single deliberately nonexistent username produced the expected JSON HTTP **401** (`Invalid credentials`)
  through **nginx → gateway → auth → database**. An unauthenticated protected gene API request also returned **401**.
- Verified all five Flyway histories were successful and current on **both primary and replica**:
  `public=21`, `auth=1`, `analytics=2`, `import_batch=1`, `export_data=2`.
- Confirmed `pg_is_in_recovery()` was **false** on primary and **true** on replica.
- Import and export containers each created and removed a temporary file on their own PVC successfully.
- Smoke-test evidence is in the local temporary log `bio-minikube-smoke-18080.log`; rollout/build logs are also
  temporary local files, not repository artifacts.
- No Compose dataset was migrated. Authenticated business workflows, real imports/exports, UI browser tests,
  and load testing were not performed. Deployment/teardown scripts and the Kubernetes README remain pending;
  this run used PowerShell/Python terminal orchestration rather than the planned Bash deployment script.
