# OPS-002 Implementation Plan

## Architecture (brief)

- **One folder per compose service** under `devops/kubernetes/`, each containing only the objects it owns
  (`deployment.yaml` | `job.yaml`, `service.yaml`, `configmap.yaml`, `secret.yaml`, `pvc.yaml`).
- **Shared Spring configuration** (compose anchor `x-common-spring-env`) lives in `common/configmap.yaml` +
  `common/secret.yaml` and is injected with `envFrom` → DRY, one place to change Eureka / Kafka / Zipkin endpoints.
- **Infrastructure secrets are owned by the infrastructure component** (`postgres-secret`, `redis-secret`,
  `gitea-db-secret`). Application secrets hold the service's **own** DB credentials (prepares ARCH-002
  database-per-service, where each service will get a dedicated DB user).
- **DNS parity with compose**: Service names = compose service names → zero change in `gitea/config-repo` / nginx.
- **Health-gated ordered rollout**: the script applies a folder, then `kubectl rollout status` (Deployments) or
  `kubectl wait --for=condition=complete` (Jobs) before moving on.
- `enableServiceLinks: false` on every pod: prevents Kubernetes from injecting `KAFKA_PORT`, `REDIS_PORT`,
  `POSTGRES_PORT`… environment variables that break Confluent images and may override Spring properties.

## Tasks

| #  | Task                                                                                                    | Status                                        |
|----|---------------------------------------------------------------------------------------------------------|-----------------------------------------------|
| 1  | Analyse `docker-compose.yml`, Dockerfiles, `.env.example`, config-repo env references                   | done                                          |
| 2  | Write `overview.md`, `plan.md`, `journal.md`                                                            | done                                          |
| 3  | Namespace + shared `common` ConfigMap / Secret                                                          | not-started                                   |
| 4  | Data stores: `postgres`, `postgres-replica`, `redis`                                                    | not-started                                   |
| 5  | Observability & messaging: `zipkin`, `zookeeper`, `kafka`, `kafka-init-topics`, `kafka-ui`              | not-started                                   |
| 6  | Platform: `discovery-server`, `gitea-db`, `gitea-server`, `config-server`, `api-gateway`                | not-started                                   |
| 7  | Business services: `auth-service`, `analytics-service`, `dashboard`, `import-service`, `export-service` | not-started                                   |
| 8  | Frontend: `frontend` (nginx)                                                                            | not-started                                   |
| 9  | `devops/scripts/deploy-minikube.sh` — ordered, health-gated deployment (+ `--build`)                    | not-started                                   |
| 10 | `devops/scripts/undeploy-minikube.sh` — reverse-order teardown (`--purge` for PVCs)                     | not-started                                   |
| 11 | `devops/kubernetes/README.md` — prerequisites, usage, access URLs, troubleshooting                      | not-started                                   |
| 12 | Update `documentation/implementation/README.md` ticket catalog                                          | not-started                                   |
| 13 | Static validation (`bash -n`, YAML parse)                                                               | not-started                                   |
| 14 | Live validation on Minikube (`minikube start` → `deploy-minikube.sh --build`)                           | not-started (blocked: Minikube not installed) |

## Status

- [x] Requirements analysed
- [x] Ticket documentation created
- [ ] Kubernetes manifests written for every compose service
- [ ] Ordered deployment script written
- [ ] Teardown script written
- [ ] README / catalog updated
- [ ] Static validation (shell syntax + YAML parsing)
- [ ] Deployed and smoke-tested on Minikube — **blocked** (Minikube not installed, explicitly out of scope for now)

---

## Detailed Checklist

### Common

- [x] `namespace.yaml` — `bio-dashboard`
- [x] `common/configmap.yaml` — profiles, config-server import, Eureka, Redis host/port, Kafka, Zookeeper discovery,
  Zipkin, `LOG_PATH`, JWT expires, health probes flag
- [x] `common/secret.yaml` — `APP_JWT_SECRET`

### Data stores

- [x] `postgres` — PVC 10Gi, init script enabling replication in `pg_hba.conf`, `wal_level=replica`, `pg_isready` probes
- [x] `postgres-replica` — PVC 10Gi, `pg_basebackup` bootstrap script from ConfigMap, reuses `postgres-secret`
- [x] `redis` — PVC 1Gi, `--requirepass` from `redis-secret`, `redis-cli ping` probes

### Messaging & observability

- [x] `zipkin` — NodePort 30941
- [x] `zookeeper` — tcp probe 2181
- [x] `kafka` — listeners `PLAINTEXT://kafka:29092`, `PLAINTEXT_HOST://localhost:9092`
- [x] `kafka-init-topics` — Job, topic list in ConfigMap, idempotent (`--if-not-exists`), `backoffLimit`
- [x] `kafka-ui` — NodePort 30090

### Platform

- [x] `discovery-server` — NodePort 30761
- [x] `gitea-db` — PVC 2Gi
- [x] `gitea-server` — PVC 5Gi, NodePort 30300 (HTTP) / 30222 (SSH), manual gate in the deploy script
- [x] `config-server` — Git credentials + `ENCRYPT_KEY` in Secret
- [x] `api-gateway` — NodePort 30080

### Business services

- [ ] `auth-service`, `analytics-service`, `dashboard`, `import-service`, `export-service`
    - [ ] `envFrom` common ConfigMap/Secret + own ConfigMap/Secret
    - [ ] Redis password via `secretKeyRef` → `redis-secret` (single source of truth)
    - [ ] Startup / readiness / liveness probes on actuator health groups
    - [ ] `emptyDir` for `/app/logs` (stdout remains the primary log sink)
    - [ ] PVC for `/app/bio-import` (import) and `/app/bio-export` (export)

### Frontend

- [ ] `frontend` — nginx image, NodePort 30000, upstream `api-gateway:8080` unchanged

### Scripts

- [ ] `deploy-minikube.sh` — prerequisites check, optional `--build`, ordered apply + wait, Gitea gate, access summary
- [ ] `undeploy-minikube.sh` — reverse order, keeps PVCs by default, `--purge` deletes namespace

## Risks & Edge Cases

| Risk                                                                                       | Mitigation                                                                                                |
|--------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| Minikube default resources (2 CPU / 4 GiB) too small for ~18 pods incl. 7 JVMs             | README recommends `minikube start --cpus=6 --memory=12g --disk-size=40g`                                  |
| Images tagged `:latest` would force `imagePullPolicy: Always` and fail (no registry)       | Images tagged `:local`, `imagePullPolicy: IfNotPresent`, built inside Minikube's Docker daemon            |
| Gitea `config-repo` is empty on a fresh cluster → config-server & all Spring services fail | Deploy script pauses after Gitea with setup instructions (`--yes` to skip)                                |
| Non-root `bioapp` user writing to PVCs (`/app/bio-import`, `/app/bio-export`)              | Minikube hostPath provisioner creates `0777` directories; revisit with `fsGroup` for real clusters        |
| Kubernetes service-link env vars (`KAFKA_PORT=tcp://…`) break Confluent images             | `enableServiceLinks: false` on every pod                                                                  |
| Replica bootstraps before primary has enabled replication                                  | Replica waits for `pg_isready` on primary; primary rollout is awaited first by the script                 |
| Eureka registering un-resolvable pod hostnames                                             | `prefer-ip-address: true` already set in config-repo; also forced via `EUREKA_INSTANCE_PREFER_IP_ADDRESS` |
| Placeholder secrets deployed as-is                                                         | Documented; secrets must be edited locally (never committed) or created with `kubectl create secret`      |
| Fresh Minikube PostgreSQL is empty (compose volume data is not migrated)                   | Flyway recreates schemas; data import must be re-run (documented)                                         |

