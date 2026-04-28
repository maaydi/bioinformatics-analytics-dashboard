# Root Project Structure

```text
uniprot-dashboard/
│── README.md
│── .gitignore
│── docker-compose.yml
│── .env
│── docs/
│
├── backend/                 # Spring Boot app
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   └── src/
│       ├── main/
│       │   ├── java/com/example/uniprot/
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── dto/
│       │   │   ├── entity/
│       │   │   ├── mapper/
│       │   │   ├── repository/
│       │   │   ├── service/
│       │   │   ├── specification/
│       │   │   ├── batch/
│       │   │   └── UniprotApplication.java
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── db/migration/
│       │       └── data/
│       └── test/
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
./mvnw spring-boot:run
```

Runs on:

```text
http://localhost:8080
```

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

Copy output to:

```text
backend/src/main/resources/static/
```

Then Spring Boot serves both frontend + API.

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
    image: postgres:16
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
