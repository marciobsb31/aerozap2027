#!/usr/bin/env bash
# Verifica a observabilidade de ponta a ponta e grava a evidência em logs/obs-verify.log.
# Confirma: serviços no ar, coleta de métricas, alerta disparado, logs no Loki e rastros no Tempo.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
mkdir -p logs
set -a; [ -f .env ] && . ./.env; set +a

GRAFANA=${GRAFANA_PORT:-3000}
PROM=${PROMETHEUS_PORT:-9090}
ALERTA=${ALERTMANAGER_PORT:-9093}
LOKI=${LOKI_PORT:-3100}
TEMPO=${TEMPO_PORT:-3200}
FALHAS=0

ok(){ echo "✓ $1"; }
falhou(){ echo "✗ $1" >&2; FALHAS=$((FALHAS+1)); }
# Tenta algumas vezes: logo depois do obs-up, Grafana e Loki ainda estão terminando de subir,
# e falhar na primeira tentativa diria "quebrado" quando o certo é "ainda não".
tenta(){
  for _ in 1 2 3 4 5; do
    if curl -fsS --max-time 10 "$1" 2>/dev/null; then
      return 0
    fi
    sleep 3
  done
  return 1
}

{
  echo "=== Verificação da observabilidade — $(date -Iseconds) ==="
  echo

  tenta "http://localhost:${PROM}/-/ready" >/dev/null && ok "Prometheus pronto" || falhou "Prometheus não responde em ${PROM}"
  tenta "http://localhost:${ALERTA}/-/ready" >/dev/null && ok "Alertmanager pronto" || falhou "Alertmanager não responde em ${ALERTA}"
  tenta "http://localhost:${GRAFANA}/api/health" | grep -q '"database"' && ok "Grafana pronto" || falhou "Grafana não responde em ${GRAFANA}"
  if tenta "http://localhost:${LOKI}/ready" >/dev/null \
     || tenta "http://localhost:${LOKI}/loki/api/v1/labels" | grep -q '"status":"success"'; then
    ok "Loki respondendo"
  else
    falhou "Loki não responde em ${LOKI}"
  fi
  tenta "http://localhost:${TEMPO}/ready" >/dev/null && ok "Tempo pronto" || falhou "Tempo não responde em ${TEMPO}"

  echo
  echo "— Alvos de coleta —"
  ALVOS=$(tenta "http://localhost:${PROM}/api/v1/targets?state=active")
  echo "$ALVOS" | tr ',' '\n' | grep -E '"(job|health)":' | paste - - 2>/dev/null | sed 's/^/  /' | head -20
  echo "$ALVOS" | grep -q '"health":"up"' && ok "pelo menos um alvo saudável" || falhou "nenhum alvo saudável"
  echo "$ALVOS" | grep -q '"job":"rabbitmq"' && ok "RabbitMQ sendo coletado" || falhou "RabbitMQ não aparece nos alvos"

  echo
  echo "— Plugin de métricas do RabbitMQ —"
  PLUGINS=$(docker exec aerozap-rabbitmq rabbitmq-plugins list -e 2>&1)
  echo "$PLUGINS" | grep -q "rabbitmq_prometheus" \
    && ok "rabbitmq_prometheus ativo" \
    || falhou "rabbitmq_prometheus não está ativo (o RabbitMQ precisa ser recriado por obs-up.sh)"

  echo
  echo "— Alertas —"
  ALERTAS=$(tenta "http://localhost:${ALERTA}/api/v2/alerts")
  echo "$ALERTAS" | grep -q 'AlertaDeTeste' \
    && ok "alerta de teste chegou ao Alertmanager (caminho de alertas íntegro)" \
    || falhou "o alerta de teste não chegou ao Alertmanager (pode levar até 1 minuto após subir)"

  echo
  echo "— Logs no Loki —"
  ROTULOS=$(tenta "http://localhost:${LOKI}/loki/api/v1/label/service_name/values")
  echo "  $ROTULOS"
  echo "$ROTULOS" | grep -q '"status":"success"' \
    && ok "Loki respondendo consultas (sem log ainda é resposta válida)" \
    || falhou "Loki não respondeu à consulta de rótulos"
  if echo "$ROTULOS" | grep -q 'platform-core'; then
    ok "há logs do platform-core no Loki"
  else
    echo "• Ainda não há logs do platform-core. Rode o serviço com o perfil observabilidade:"
    echo "    SPRING_PROFILES_ACTIVE=observabilidade bash scripts/dev-service-check.sh"
  fi

  echo
  echo "— Rastros no Tempo —"
  if tenta "http://localhost:${TEMPO}/api/search/tags" | grep -q 'service.name'; then
    ok "há rastros no Tempo (service.name já conhecido)"
  else
    echo "• Ainda não há rastros. Faça uma requisição ao serviço com o perfil observabilidade ativo."
  fi

  echo
  if [ "$FALHAS" -eq 0 ]; then
    echo "✓ Observabilidade verificada."
  else
    echo "✗ $FALHAS verificação(ões) falharam."
  fi
} 2>&1 | tee logs/obs-verify.log

echo
echo "Evidência em logs/obs-verify.log"
grep -q '^✗' logs/obs-verify.log && exit 1 || exit 0
