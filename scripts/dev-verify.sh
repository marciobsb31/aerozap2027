#!/usr/bin/env bash
# Verifica de fato cada serviço (além do health check) e imprime a evidência.
set -euo pipefail
cd "$(dirname "$0")/.."
set -a; . ./.env; set +a
C=(docker compose --env-file .env -f infrastructure/compose/docker-compose.yml)
ok(){ echo "✓ $1"; }; fail(){ echo "✗ $1" >&2; exit 1; }

"${C[@]}" ps
echo
"${C[@]}" exec -T postgres psql -U "${POSTGRES_USER:-aerozap}" -d "${POSTGRES_DB:-aerozap}" -tAc "select 1" | grep -q 1 && ok "PostgreSQL responde a consultas" || fail "PostgreSQL"
"${C[@]}" exec -T rabbitmq rabbitmq-diagnostics -q check_running >/dev/null && ok "RabbitMQ em execução" || fail "RabbitMQ"
"${C[@]}" exec -T redis redis-cli ping | grep -q PONG && ok "Redis responde PONG (com senha)" || fail "Redis"
curl -fsS "http://localhost:${S3_PORT:-9000}/health" >/dev/null && ok "Object storage ativo" || fail "Object storage"
echo "Tudo certo."
