#!/usr/bin/env bash
# build-module.sh — Rebuild and redeploy one Docker Compose service.
# Usage: ./devops/scripts/build-module.sh <module>
#
# Supported modules:
#   frontend
#   dashboard (or backend)
#   discovery-server
#   config-server
#   api-gateway
#   auth-service
#   analytics-service
#   import-service
#   export-service

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MODULE_NAME="${1:-}"

if [[ -z "$MODULE_NAME" || $# -ne 1 ]]; then
  echo "Usage: $0 <module>" >&2
  exit 64
fi

declare -A MAVEN_MODULE_PATHS=(
  [dashboard]="dashboard"
  [backend]="dashboard"
  [discovery-server]="infrastructure/discovery-server"
  [config-server]="infrastructure/config-server"
  [api-gateway]="infrastructure/api-gateway"
  [auth-service]="services/auth-service"
  [analytics-service]="services/analytics-service"
  [import-service]="services/import-service"
  [export-service]="services/export-service"
)

declare -A COMPOSE_SERVICES=(
  [dashboard]="backend"
  [backend]="backend"
  [discovery-server]="discovery-server"
  [config-server]="config-server"
  [api-gateway]="api-gateway"
  [auth-service]="auth-service"
  [analytics-service]="analytics-service"
  [import-service]="import-service"
  [export-service]="export-service"
)

stop_service() {
  local service_name="$1"

  echo "==> Stopping $service_name service..."
  docker compose rm --force --stop "$service_name"
}

cd "$ROOT_DIR"

if [[ "$MODULE_NAME" == "frontend" ]]; then
  COMPOSE_SERVICE="frontend"

  stop_service "$COMPOSE_SERVICE"

  echo "==> Installing frontend dependencies..."
  cd "$ROOT_DIR/frontend"
  npm ci --prefer-offline --silent

  echo "==> Building Angular frontend (production)..."
  npm run build:prod
  echo "    Angular dist: frontend/dist/"
else
  MAVEN_MODULE_PATH="${MAVEN_MODULE_PATHS[$MODULE_NAME]:-}"
  COMPOSE_SERVICE="${COMPOSE_SERVICES[$MODULE_NAME]:-}"

  if [[ -z "$MAVEN_MODULE_PATH" || -z "$COMPOSE_SERVICE" ]]; then
    echo "Unsupported module: $MODULE_NAME" >&2
    echo "Supported modules: frontend, dashboard, discovery-server, config-server, api-gateway, auth-service, analytics-service, import-service, export-service." >&2
    exit 64
  fi

  stop_service "$COMPOSE_SERVICE"

  echo "==> Building Maven module $MAVEN_MODULE_PATH..."
  mvn -f ./backend/pom.xml -pl "$MAVEN_MODULE_PATH" -am clean package -DskipTests -q
fi

cd "$ROOT_DIR"
echo "==> Building Docker image for $COMPOSE_SERVICE..."
docker compose build "$COMPOSE_SERVICE"

echo "==> Starting $COMPOSE_SERVICE service..."
docker compose up --detach --no-deps "$COMPOSE_SERVICE"

echo "==> $COMPOSE_SERVICE has been rebuilt and started."
