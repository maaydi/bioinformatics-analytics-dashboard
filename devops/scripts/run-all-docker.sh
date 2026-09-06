#!/usr/bin/env bash
# build-all.sh — full production build (backend JAR + Angular dist)
# Usage: ./devops/scripts/build-all.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "==> Start Application services deployment..."
cd "$ROOT_DIR"

if ! docker compose config --services > /dev/null 2>&1; then
    echo "Error : No docker compose file found in current folder"
    exit 1
fi

SERVICES=(
  "postgres"
  "postgres-replica"
  "redis"
  "zipkin"
  "zookeeper"
  "kafka"
  "kafka-init-topics"
  "kafka-ui"
  "discovery-server"
  "gitea-db"
  "gitea-server"
  "config-server"
  "api-gateway"
  "auth-service"
  "analytics-service"
  "backend"
  "import-service"
  "frontend"
)

for service in "${SERVICES[@]}"; do
  echo "Starting $service..."
  docker compose up -d --no-recreate "$service"

  container_id=$(docker compose ps -q "$service")

  # Verify if healthcheck exists on this container
  has_health=$(docker inspect --format='{{if .State.Health}}true{{else}}false{{end}}' "$container_id" 2>/dev/null || echo "false")

  if [ "$has_health" = "true" ]; then
    echo "Waiting for $service healthcheck to pass..."

    until [ "$(docker inspect --format='{{.State.Health.Status}}' "$container_id")" = "healthy" ]; do
      status=$(docker inspect --format='{{.State.Health.Status}}' "$container_id")

      if [ "$status" = "unhealthy" ]; then
        echo "Error: $service failed healthcheck and marked unhealthy."
        exit 1
      fi

      sleep 2
    done

    echo "$service is healthy!"
  fi
done

echo "Deployment completed successfully."