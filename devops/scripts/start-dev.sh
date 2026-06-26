#!/usr/bin/env bash
# start-dev.sh — one-command local development startup with interactive restarts
# Usage: ./devops/scripts/start-dev.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Global tracking variables for background process IDs
BACKEND_PID=""
FRONTEND_PID=""

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
if [ -z "${JAVA_25_HOME:-}" ]; then
  echo "ERROR: JAVA_25_HOME is not defined"
  exit 1
fi

export JAVA_HOME="$JAVA_25_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

# =========================================================
# Helper: Force Clear Ports
# =========================================================
kill_port() {
  local port=$1
  if command -v lsof &>/dev/null; then
    lsof -t -i:"$port" | xargs kill -9 &>/dev/null || true
  elif command -v fuser &>/dev/null; then
    fuser -k "$port"/tcp &>/dev/null || true
  fi
}

# =========================================================
# Service Management Functions
# =========================================================

restart_postgres() {
  echo "==> [Postgres] Restarting container..."
  docker compose -f "$ROOT_DIR/docker-compose.yml" stop postgres || true
  docker compose -f "$ROOT_DIR/docker-compose.yml" up postgres -d

  echo "==> [Postgres] Waiting for ready state..."
  until docker compose -f "$ROOT_DIR/docker-compose.yml" exec postgres \
    pg_isready -U "${POSTGRES_USER:-bio_user}" -d "${POSTGRES_DB:-bioinformatics_db}" &>/dev/null; do
    sleep 1
  done
  echo "    PostgresSQL is ready."
}

restart_redis() {
  echo "==> [Redis] Restarting container..."
  docker compose -f "$ROOT_DIR/docker-compose.yml" down redis -v || true
  docker compose -f "$ROOT_DIR/docker-compose.yml" up redis -d

  echo "==> [Redis] Waiting for ready state..."
  until docker compose -f "$ROOT_DIR/docker-compose.yml" exec redis \
    redis-cli -a "${REDIS_PASSWORD}" --no-auth-warning ping 2>/dev/null | grep -q "PONG"; do
    sleep 1
  done
  echo "    Redis is ready."
}

start_backend() {
  echo "==> [Backend] Building Spring Boot application..."
  cd "$ROOT_DIR/backend"
  mvn clean install -DskipTests

  echo "==> [Backend] Starting Spring Boot application..."
  mvn spring-boot:run -DskipTests &
  BACKEND_PID=$!

  echo "==> [Backend] Waiting for port 8080..."
  until curl -s http://localhost:8080/actuator/health &>/dev/null; do
    sleep 2
  done
  echo "    Backend is ready."
}

restart_backend() {
  echo "==> [Backend] Stopping current process..."
  if [ -n "${BACKEND_PID:-}" ]; then
    kill "$BACKEND_PID" 2>/dev/null || true
    wait "$BACKEND_PID" 2>/dev/null || true
  fi
  kill_port 8080  # Ensure the Java subprocess releases the port
  start_backend
}

start_frontend() {
  echo "==> [Frontend] Starting Angular development server..."
  cd "$ROOT_DIR/frontend"
  npx ng serve --proxy-config proxy.conf.json &
  FRONTEND_PID=$!
}

restart_frontend() {
  echo "==> [Frontend] Stopping current process..."
  if [ -n "${FRONTEND_PID:-}" ]; then
    kill "$FRONTEND_PID" 2>/dev/null || true
    wait "$FRONTEND_PID" 2>/dev/null || true
  fi
  kill_port 4200  # Ensure the Node/Angular subprocess releases the port
  start_frontend
}

print_menu() {
  echo ""
  echo "================================================"
  echo "      Frontend : http://localhost:4200"
  echo "      Backend  : http://localhost:8080"
  echo "      Database : localhost:5432"
  echo "      Redis    : localhost:6379"
  echo "================================================"
  echo " Interactive Hotkeys:"
  echo "  [p] Restart Postgres"
  echo "  [r] Restart Redis"
  echo "  [b] Rebuild & Restart Backend"
  echo "  [f] Restart Frontend"
  echo "  [Ctrl+C] Exit & Stop all services"
  echo "================================================"
  echo -n "Waiting for input..."
}

# =========================================================
# Cleanup Handler
# =========================================================
cleanup() {
  # CRITICAL: Clear traps immediately to prevent recursive loop triggers
  trap - EXIT INT

  echo ""
  echo "==> Stopping all background services..."

  # Terminate known PIDs
  kill ${BACKEND_PID:-} ${FRONTEND_PID:-} 2>/dev/null || true

  # Force clear ports
  kill_port 4200
  kill_port 8080

  # Tear down docker containers
  docker compose -f "$ROOT_DIR/docker-compose.yml" down postgres redis 2>/dev/null || true

  echo "==> Clean exit complete."
  exit 0
}
# Catch both normal exit and user interrupt
trap cleanup EXIT INT

# =========================================================
# Initial Startup Sequence
# =========================================================
restart_postgres
restart_redis
start_backend

echo "==> [Frontend] Running initial npm install..."
cd "$ROOT_DIR/frontend"
npm install --silent
start_frontend

print_menu

# =========================================================
# Interactive Keyboard Input Loop
# =========================================================
while true; do
  # Read 1 character silently (-s) without requiring Enter
  if read -n 1 -r -s key; then
    case "$key" in
      p|P)
        restart_postgres
        print_menu
        ;;
      r|R)
        restart_redis
        print_menu
        ;;
      b|B)
        restart_backend
        print_menu
        ;;
      f|F)
        restart_frontend
        print_menu
        ;;
      k|K)
        cleanup
        ;;
      *)
        # Ignore unsupported keys
        ;;
    esac
  fi
done