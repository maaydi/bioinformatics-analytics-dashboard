---
name: backend-maven-module
description: 'Use when running Maven commands for a specific backend module in the bioinformatics project; gives the correct project-root invocation and module path conventions.'
---

# Backend Maven module workflow

Use this skill whenever you need to run Maven for a single module inside the backend folder instead of building the
entire multi-module project.

## Rule

Always run the command from the repository root:

```bash
cd /home/medali/VscodeProjects/bioinformatics-analytics-dashboard
mvn -f ./backend/pom.xml -pl <SERVICE_PATH> -am <MAVEN_COMMAND>
```

## Parameters

- <SERVICE_PATH>: the module path relative to `backend/`
- <MAVEN_COMMAND>: the Maven goal, such as `test`, `compile`, `package`, `verify`, or `clean test`

## Common module paths

- `dashboard`
- `libs/common-starter`
- `services/auth-service`
- `services/analytics-service`
- `services/import-service`
- `services/export-service`
- `infrastructure/discovery-server`
- `infrastructure/config-server`
- `infrastructure/api-gateway`

## Examples

```bash
cd /home/medali/VscodeProjects/bioinformatics-analytics-dashboard
mvn -f ./backend/pom.xml -pl dashboard -am test

mvn -f ./backend/pom.xml -pl libs/common-starter -am compile

mvn -f ./backend/pom.xml -pl services/auth-service -am test
```

## Why this pattern matters

- `-f ./backend/pom.xml` targets the correct Maven reactor for this project.
- `-pl <SERVICE_PATH>` selects the specific module to build.
- `-am` also builds required upstream modules, which keeps dependencies consistent.
- Using the repo root avoids wrong-path issues and ensures the multi-module reactor resolves correctly.

## Best practice

Use the module path that matches the backend project structure exactly. Do not run Maven from inside `backend/` with a
bare `mvn` command unless the task is intentionally scoped to a single module and the POM is already correct.

## Quick reminder

When the user asks to "run Maven for a specific module inside backend", use:

```bash
mvn -f ./backend/pom.xml -pl <SERVICE_PATH> -am <MAVEN_COMMAND>
```

