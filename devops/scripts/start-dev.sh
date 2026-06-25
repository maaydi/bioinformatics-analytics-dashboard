#!/usr/bin/env bash
# start-dev.sh — one-command local development startup
# Usage: ./devops/scripts/start-dev.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# =========================================================
# Load .env
# =========================================================
if [ -f "$ROOT_DIR/.env" ]; then
  echo "==> Loading .env..."
  set -a
  source "$ROOT_DIR/.env"
  set +a
fi

# =========================================================
# Java 25 / Maven setup
# =========================================================

# Example:
# define java 25 in .env file
# export JAVA_25_HOME=/usr/lib/jvm/jdk-25

if [ -z "${JAVA_25_HOME:-}" ]; then
  echo "ERROR: JAVA_25_HOME is not defined"
  exit 1
fi

export JAVA_HOME="$JAVA_25_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> Starting PostgresSQL via Docker Compose..."
docker compose -f "$ROOT_DIR/docker-compose.yml" up postgres -d

echo "==> Waiting for PostgresSQL to be ready..."
until docker compose -f "$ROOT_DIR/docker-compose.yml" exec postgres \
    pg_isready -U "${POSTGRES_USER:-bio_user}" -d "${POSTGRES_DB:-bioinformatics_db}" &>/dev/null; do
    sleep 1
done
echo "    PostgresSQL is ready."

echo "==> Starting Redis via Docker Compose..."
docker compose -f "$ROOT_DIR/docker-compose.yml" up redis -d

echo "==> Waiting for Redis to be ready..."
until docker compose -f "$ROOT_DIR/docker-compose.yml" exec redis \
    redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning ping 2>/dev/null | grep -q "PONG"; do
    sleep 1
done
echo "    Redis is ready."

echo "==> Build Spring Boot application..."
cd "$ROOT_DIR/backend"
mvn clean install -DskipTests


echo "==> Starting Spring Boot backend (foreground — Ctrl+C to stop)..."
cd "$ROOT_DIR/backend"
mvn spring-boot:run -DskipTests &
BACKEND_PID=$!

echo "==> Waiting for backend on port 8080..."
until curl -s http://localhost:8080/actuator/health &>/dev/null; do
    sleep 2
done
echo "    Backend is ready."

echo "==> Starting Angular frontend..."
cd "$ROOT_DIR/frontend"
npm install --silent
npx ng serve --proxy-config proxy.conf.json &
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
