# Root Project Structure

```text
uniprot-dashboard/
│── README.md
│── .gitignore
│── docker-compose.yml
│── .env
│── docs/
│
├── backend/                 # Maven reactor
│   ├── pom.xml
│   ├── libs/
│   │   ├── shared-models/
│   │   └── common-starter/  # Explicit conditional auto-configurations; producer-only audit
│   ├── infrastructure/
│   │   ├── discovery-server/
│   │   ├── config-server/
│   │   └── api-gateway/
│   └── services/
│       ├── gene-service/    # com.bioinformatics.geneservice; former backend/dashboard
│       ├── auth-service/
│       ├── analytics-service/
│       ├── import-service/
│       ├── export-service/
│       └── audit-service/   # Owns audit_log persistence/search; separate from catalog
│
├── frontend/               # Angular app
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig.json
│   └── src/
│       ├── app/
│       │   ├── core/
│       │   ├── shared/
│       │   ├── layout/
│       │   ├── features/
│       │   │   ├── dashboard/
│       │   │   ├── genes/
│       │   │   ├── analytics/
│       │   │   ├── import-admin/
│       │   │   └── settings/
│       │   ├── app-routing.module.ts
│       │   └── app.module.ts
│       ├── assets/
│       └── environments/
│
└── scripts/
    ├── start-dev.sh
    ├── build-all.sh
    └── import-data.sh
```

---

# Why This Structure Is Good

## Clean separation

* `backend/` = Java world
* `frontend/` = Node/Angular world

## One Git repo

Single versioning for fullstack app.

## Easy onboarding

```bash
git clone ...
cd uniprot-dashboard
```

Ready to run.

## ARCH-002 Module and Data Ownership

| Component | Module/artifact | Runtime ID / Java root | Persistence and event boundary |
|-----------|-----------------|------------------------|--------------------------------|
| Catalog owner | `backend/services/gene-service` / `gene-service` | `gene-service` / `com.bioinformatics.geneservice` | Sole owner of catalog tables in `gene_data`; publishes catalog outbox events. Former module was `backend/dashboard`. |
| Shared audit producer | `backend/libs/common-starter` | Shared library / `com.bioinformatics.common.audit` | Produces audit events only; it has no audit entity, repository, migration, or database credential. |
| Audit owner | `backend/services/audit-service` / `audit-service` | `audit-service` / `com.bioinformatics.auditservice` | Sole owner of `audit_log` persistence, migrations, search, and retention. |
| Cross-service gene contracts | `backend/libs/gene-model-lib` / `gene-model-lib` (planned) | Library / `com.bioinformatics.gene.model.v1` | Reused DTOs and Feign/event contracts only; internal types remain service-local. |

Analytics persists refresh aggregates in its own `analytics` schema and consumes catalog ingestion-completion events;
it does not read `gene_data` directly on the target path. Import, auth, and export services use their own schemas.
Only `gene-service` receives catalog runtime access. Audit producers and audit-service do not receive catalog access;
producer services do not receive audit-storage credentials. Public `/api/**` and internal `/internal/v1/**` paths
remain unchanged by the module rename.

---

# Backend Structure (Spring Boot)

Inside:

```text
controller/
service/
repository/
entity/
dto/
mapper/
specification/
batch/
config/
```

## Example

```text
controller/GeneController.java
service/GeneService.java
repository/GeneRepository.java
entity/GeneEntry.java
dto/GeneSearchRequest.java
```

---

# Frontend Structure (Angular)

Use feature-based architecture.

```text
features/
  dashboard/
  genes/
  analytics/
  gene-detail/
```

---

## Example

```text
features/genes/
  genes-page.component.ts
  genes-table.component.ts
  gene-filter.component.ts
  gene.service.ts
```

---

# Dev Workflow

## Run backend

```bash
cd backend
mvn -pl services/gene-service -am install -DskipTests
cd services/gene-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8084
```

Runs on:

```text
http://localhost:8084
```

The frontend normally calls the gateway on `8080`, not gene-service directly.
Config Server source files live in `backend-config`; container definitions and
Kubernetes manifests live in `devops`. Each service declares its own infrastructure
starters. Only gene/import/export/analytics temporarily opt into legacy catalog
beans with `app.legacy-catalog.enabled`; audit production requires `app.audit.enabled`.

---

## Run frontend

```bash
cd frontend
npm install
ng serve
```

Runs on:

```text
http://localhost:4200
```

Angular proxies API calls to backend.

---

# Angular Proxy Config

`frontend/proxy.conf.json`

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false
  }
}
```

Then:

```bash
ng serve --proxy-config proxy.conf.json
```

---

# Production Build Strategy (Best Practice)

Build Angular and serve static files from Spring Boot.

## Build Angular

```bash
cd frontend
ng build
```

Build output is packaged into the separate Nginx frontend image; the backend does not copy Angular assets into a
dashboard module:

```text
devops/docker/frontend/Dockerfile
```

The frontend is served separately by Nginx; API calls go through the gateway.

---

# Add Scripts at Root

## `build-all.sh`

```bash
cd frontend && npm install && ng build
cd ../backend && ./mvnw clean package
```

---

# Docker Compose Root

```yaml
version: "3.8"

services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: uniprot
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
```

Later add backend container.

---

# Recommended Git Ignore

```text
frontend/node_modules/
frontend/dist/

backend/target/

.idea/
.vscode/
.env
```

---

# Better Angular App Structure

```text
core/
  interceptors/
  guards/
  services/

shared/
  components/
  pipes/
  directives/

features/
  dashboard/
  genes/
  analytics/
```

---

# Real Enterprise Variant

If the application grows:

```text
apps/
  frontend/
  backend/

packages/
  shared-types/
  ui-components/
```

For initial development, keep the structure simple.

---

# Recommended Project Setup

For this UniProt dashboard project:

## Root Folder Name

```text
bio-explorer/
```

or

```text
uniprot-insight/
```

More descriptive than a generic name.

---

# Recommended Configuration

```text
bio-explorer/
  frontend/
  backend/
  docker-compose.yml
  README.md
```

Clean, modern, and maintainable.
