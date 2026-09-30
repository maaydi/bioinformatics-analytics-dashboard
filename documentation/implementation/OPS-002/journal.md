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
