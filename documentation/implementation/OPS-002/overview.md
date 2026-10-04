# OPS-002 — Migration from Docker Compose to Kubernetes (Minikube)

## Description

The platform (ARCH-001 microservices + infrastructure) is currently orchestrated with `docker-compose.yml` and started
sequentially by `devops/scripts/run-all-docker.sh`. This ticket migrates the local runtime to **Kubernetes**, targeting
a
single-node **Minikube** cluster, while keeping Docker Compose as a working fallback.

Every compose service is translated into native Kubernetes objects:

- **Deployment** (or **Job** for one-shot tasks) — workload definition, probes, resources.
- **Service** — stable in-cluster DNS name (identical to the compose service name so that the Gitea `config-repo`
  files, JDBC URLs, Kafka bootstrap servers, Zipkin endpoint and nginx upstream keep working unchanged).
- **ConfigMap** — non-sensitive configuration (ports, hostnames, profiles, feature flags, scripts).
- **Secret** — sensitive configuration (passwords, JWT secret, encryption key, Git credentials).
- **PersistentVolumeClaim** — for stateful components (PostgreSQL primary/replica, Redis, Gitea, Gitea DB, import/export
  working directories).

A deployment script deploys the services **one by one, in dependency order**, waiting for each rollout to become ready
before moving to the next (Kubernetes equivalent of compose `depends_on: condition: service_healthy`).

## Scope

| Area              | Artifact                                                                                   |
|-------------------|--------------------------------------------------------------------------------------------|
| Manifests         | `devops/kubernetes/<service>/{deployment,service,configmap,secret,pvc}.yaml`               |
| Shared config     | `devops/kubernetes/common/{configmap,secret}.yaml` (compose `x-common-spring-env` anchor)  |
| Namespace         | `devops/kubernetes/namespace.yaml` → `bio-dashboard`                                       |
| One-shot tasks    | `devops/kubernetes/kafka-init-topics/job.yaml` (compose `kafka-init-topics`)               |
| Deployment script | `devops/scripts/deploy-minikube.sh` — ordered, health-gated deployment                     |
| Teardown script   | `devops/scripts/undeploy-minikube.sh` — reverse-order removal (PVCs kept unless `--purge`) |
| Documentation     | `devops/kubernetes/README.md`, this ticket folder                                          |

### Service mapping (compose → Kubernetes)

| Compose service     | K8s workload             | K8s Service (DNS)   | Port(s)      | Exposure                 |
|---------------------|--------------------------|---------------------|--------------|--------------------------|
| `postgres`          | Deployment + PVC         | `postgres`          | 5432         | ClusterIP                |
| `postgres-replica`  | Deployment + PVC         | `postgres-replica`  | 5432         | ClusterIP                |
| `redis`             | Deployment + PVC         | `redis`             | 6379         | ClusterIP                |
| `zipkin`            | Deployment               | `zipkin`            | 9411         | NodePort `30941`         |
| `zookeeper`         | Deployment               | `zookeeper`         | 2181         | ClusterIP                |
| `kafka`             | Deployment               | `kafka`             | 29092 / 9092 | ClusterIP                |
| `kafka-init-topics` | Job                      | —                   | —            | —                        |
| `kafka-ui`          | Deployment               | `kafka-ui`          | 8080         | NodePort `30090`         |
| `discovery-server`  | Deployment               | `discovery-server`  | 8761         | NodePort `30761`         |
| `gitea-db`          | Deployment + PVC         | `gitea-db`          | 5432         | ClusterIP                |
| `gitea-server`      | Deployment + PVC         | `gitea-server`      | 3000 / 22    | NodePort `30300`/`30222` |
| `config-server`     | Deployment               | `config-server`     | 8888         | ClusterIP                |
| `api-gateway`       | Deployment               | `api-gateway`       | 8080         | NodePort `30080`         |
| `auth-service`      | Deployment               | `auth-service`      | 8081         | ClusterIP                |
| `analytics-service` | Deployment               | `analytics-service` | 8082         | ClusterIP                |
| `backend`           | Deployment (`dashboard`) | `dashboard`         | 8084         | ClusterIP                |
| `import-service`    | Deployment + PVC         | `import-service`    | 8083         | ClusterIP                |
| `export-service`    | Deployment + PVC         | `export-service`    | 8085         | ClusterIP                |
| `frontend`          | Deployment               | `frontend`          | 80           | NodePort `30000`         |

> The compose service `backend` is renamed `dashboard` in Kubernetes to match its `spring.application.name` and its
> Eureka registration (`lb://dashboard` in `api-gateway.yml`). No other component resolves it by DNS.

## Acceptance Criteria

- [ ] `devops/kubernetes/` contains one folder per compose service with, at minimum, a `deployment.yaml` (or `job.yaml`)
  and a `service.yaml`, plus a `configmap.yaml` and a `secret.yaml` whenever the service has configuration / sensitive
  data.
- [ ] All resources live in the `bio-dashboard` namespace and carry the standard `app.kubernetes.io/*` labels.
- [ ] Kubernetes Service names equal compose service names (except `backend` → `dashboard`), so no change is required in
  `gitea/config-repo` nor in `devops/docker/nginx/nginx.conf`.
- [ ] No real secret is committed: `secret.yaml` files only contain the placeholders already published in
  `.env.example`.
- [ ] Every long-running workload defines readiness and liveness probes (Spring Boot services use
  `/actuator/health/readiness` and `/actuator/health/liveness`, guarded by a `startupProbe`).
- [ ] Every container defines CPU / memory `requests` and `limits`.
- [ ] Stateful components use a `PersistentVolumeClaim` and the `Recreate` strategy (no two pods on the same volume).
- [ ] `devops/scripts/deploy-minikube.sh` deploys services one by one in dependency order, waits for each rollout (or
  Job
  completion) before continuing, and stops with a non-zero exit code on failure.
- [ ] The script can build the application images directly inside Minikube's Docker daemon (`--build`).
- [ ] The script pauses after Gitea is up so that the operator can initialise `config-repo` (one-time step documented in
  `backend-config/README.md`), unless `--yes` is passed.
- [ ] `devops/scripts/undeploy-minikube.sh` removes the stack in reverse order.
- [ ] `docker-compose.yml` and `run-all-docker.sh` remain untouched and functional.
- [ ] **Not validated on a live cluster in this ticket** — Minikube is not installed yet (see `journal.md`).

## Out of Scope

- Helm chart / Kustomize overlays (candidate for a follow-up `OPS-003`).
- Ingress controller, TLS, HorizontalPodAutoscaler, NetworkPolicies.
- StatefulSets / operators for PostgreSQL, Kafka and Redis (single-replica Deployments are enough for Minikube).
- External secret management (Vault, Sealed Secrets, SOPS).
- CI/CD pipeline and image registry push.

## References

- `docker-compose.yml` — source of truth for the migrated topology
- `devops/scripts/run-all-docker.sh` — existing startup order
- `backend-config/README.md` — Gitea `config-repo` one-time setup
- `documentation/implementation/ARCH-001/` — microservices topology
- `documentation/implementation/OPS-001/` — health/readiness endpoints reused by Kubernetes probes

