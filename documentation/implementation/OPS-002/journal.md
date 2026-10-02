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
