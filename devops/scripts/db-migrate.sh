#!/usr/bin/env bash
# db-migrate.sh — manually run Flyway migrations against a target database
# Usage: ./devops/scripts/db-migrate.sh [info|migrate|repair|validate]

set -euo pipefail

COMMAND="${1:-info}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Load .env if present
if [ -f "$ROOT_DIR/.env" ]; then
    # shellcheck disable=SC1091
    source "$ROOT_DIR/.env"
fi

DB_URL="${SPRING_DATASOURCE_URL:-jdbc:postgresql://localhost:5432/bioinformatics_db}"
DB_USER="${SPRING_DATASOURCE_USERNAME:-bio_user}"
DB_PASS="${SPRING_DATASOURCE_PASSWORD:-}"

echo "==> Flyway $COMMAND on $DB_URL"
cd "$ROOT_DIR/backend"
./mvnw flyway:"$COMMAND" \
    -Dflyway.url="$DB_URL" \
    -Dflyway.user="$DB_USER" \
    -Dflyway.password="$DB_PASS" \
    -Dflyway.locations="classpath:db/migration"
