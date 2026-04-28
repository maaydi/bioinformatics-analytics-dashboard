#!/usr/bin/env bash
# start-dev.sh — one-command local development startup
# Usage: ./devops/scripts/start-dev.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "==> Starting PostgreSQL via Docker Compose..."
docker compose -f "$ROOT_DIR/docker-compose.yml" up postgres -d

echo "==> Waiting for PostgreSQL to be ready..."
until docker compose -f "$ROOT_DIR/docker-compose.yml" exec postgres \
    pg_isready -U "${POSTGRES_USER:-bio_user}" -d "${POSTGRES_DB:-bioinformatics_db}" &>/dev/null; do
    sleep 1
done
echo "    PostgreSQL is ready."

echo "==> Starting Spring Boot backend (foreground — Ctrl+C to stop)..."
cd "$ROOT_DIR/backend"
./mvnw spring-boot:run &
BACKEND_PID=$!

echo "==> Waiting for backend on port 8080..."
until curl -s http://localhost:8080/actuator/health &>/dev/null; do
    sleep 2
done
echo "    Backend is ready."

echo "==> Starting Angular frontend..."
cd "$ROOT_DIR/frontend"
npm install --silent
ng serve --proxy-config proxy.conf.json &
FRONTEND_PID=$!

echo ""
echo "================================================"
echo "  Frontend: http://localhost:4200"
echo "  Backend:  http://localhost:8080"
echo "  Database: localhost:5432"
echo "================================================"
echo "Press Ctrl+C to stop all services."

trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; docker compose -f \"$ROOT_DIR/docker-compose.yml\" stop postgres" EXIT

wait
