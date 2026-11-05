#!/usr/bin/env bash
# Derruba só a observabilidade e devolve o RabbitMQ à configuração do ambiente base.
# Use --apagar-dados para remover também as métricas, logs e rastros guardados.
set -euo pipefail
cd "$(dirname "$0")/.."
ARGS=()
if [ "${1:-}" = "--apagar-dados" ]; then
  ARGS+=(--volumes)
  echo "• As métricas, logs e rastros locais serão apagados."
fi
docker compose --env-file .env --project-directory . \
  -f infrastructure/compose/docker-compose.yml \
  -f infrastructure/monitoring/docker-compose.observabilidade.yml \
  stop prometheus alertmanager grafana loki tempo
docker compose --env-file .env --project-directory . \
  -f infrastructure/compose/docker-compose.yml \
  -f infrastructure/monitoring/docker-compose.observabilidade.yml \
  rm -f prometheus alertmanager grafana loki tempo
if [ "${#ARGS[@]}" -gt 0 ]; then
  docker volume rm -f aerozap_prometheus-data aerozap_grafana-data aerozap_loki-data aerozap_tempo-data >/dev/null 2>&1 || true
  echo "• Volumes da observabilidade removidos."
fi
echo "✓ Observabilidade derrubada. O ambiente base continua no ar (bash scripts/dev-up.sh para conferir)."
