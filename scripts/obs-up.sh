#!/usr/bin/env bash
# Sobe a observabilidade (Prometheus, Alertmanager, Grafana, Loki e Tempo) junto do ambiente local.
# O ambiente base (PostgreSQL, RabbitMQ, Redis, armazenamento) sobe com scripts/dev-up.sh e
# continua funcionando sem esta parte: a observabilidade é opcional e consome memória.
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

set -a; . ./.env; set +a

COMPOSE=(docker compose --env-file .env
  -f infrastructure/compose/docker-compose.yml
  -f infrastructure/monitoring/docker-compose.observabilidade.yml)

echo "• Subindo observabilidade (a primeira vez baixa as imagens)..."
# O RabbitMQ é recriado para ligar o plugin de métricas (rabbitmq_prometheus).
"${COMPOSE[@]}" up -d --wait

echo
"${COMPOSE[@]}" ps
echo
echo "✓ Observabilidade no ar."
echo "  Grafana ......... http://localhost:${GRAFANA_PORT:-3000}  (usuário ${GRAFANA_USER:-aerozap})"
echo "  Prometheus ...... http://localhost:${PROMETHEUS_PORT:-9090}"
echo "  Alertmanager .... http://localhost:${ALERTMANAGER_PORT:-9093}"
echo "  Loki ............ http://localhost:${LOKI_PORT:-3100}"
echo "  Tempo ........... http://localhost:${TEMPO_PORT:-3200}   (OTLP em 4318)"
echo
echo "  Painel pronto: Grafana > Dashboards > AeroZap > saude dos servicos e filas."
echo "  Para o serviço enviar logs e rastros, rode-o com o perfil observabilidade:"
echo "    SPRING_PROFILES_ACTIVE=observabilidade bash scripts/dev-service-check.sh"
echo "  Para verificar: bash scripts/obs-verify.sh      Para derrubar: bash scripts/obs-down.sh"
