#!/usr/bin/env bash
# build-all.sh — full production build (backend JAR + Angular dist)
# Usage: ./devops/scripts/build-all.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "==> Building Spring Boot backend services..."
cd "$ROOT_DIR/backend"
mvn dependency:go-offline -q
mvn clean package -DskipTests -q

echo "==> Installing frontend dependencies..."
cd "$ROOT_DIR/frontend"
npm ci --prefer-offline --silent

echo "==> Building Angular frontend (production)..."
npm run build:prod
echo "    Angular dist: frontend/dist/"

echo "==> Building Docker images..."
cd "$ROOT_DIR"
docker compose build

echo ""
echo "================================================"
echo "  Build complete. Run with:"
echo "    docker compose up"
echo "================================================"
