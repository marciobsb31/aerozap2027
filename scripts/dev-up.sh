#!/usr/bin/env bash
# Sobe o ambiente local do AeroZap e espera todos os health checks ficarem saudáveis.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  cp .env.example .env
  echo "• .env criado a partir de .env.example (valores locais fictícios; troque se quiser)."
fi

if ! docker info >/dev/null 2>&1; then
  echo "✗ O Docker não está em execução. Abra o Docker Desktop e tente de novo." >&2
  exit 1
fi

COMPOSE=(docker compose --env-file .env -f infrastructure/compose/docker-compose.yml)

echo "• Baixando imagens e subindo os serviços (pode demorar na primeira vez)..."
"${COMPOSE[@]}" up -d --wait

echo
"${COMPOSE[@]}" ps
echo
echo "✓ Ambiente saudável."
echo "  PostgreSQL ...... localhost:${POSTGRES_PORT:-5432}"
echo "  RabbitMQ ........ localhost:${RABBITMQ_PORT:-5672}  (painel: http://localhost:${RABBITMQ_MANAGEMENT_PORT:-15672})"
echo "  Redis ........... localhost:${REDIS_PORT:-6379}"
echo "  Object storage .. http://localhost:${S3_PORT:-9000}  (console: http://localhost:${S3_CONSOLE_PORT:-9001})"
echo "  Para derrubar: bash scripts/dev-down.sh"
