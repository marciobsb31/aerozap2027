#!/usr/bin/env bash
# Coleta o estado e os últimos logs dos contêineres do AeroZap em logs/obs-logs.log.
# Use quando algum serviço não ficar saudável: o log do contêiner diz o motivo.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
mkdir -p logs

{
  echo "=== Estado dos contêineres — $(date -Is) ==="
  docker ps -a --filter "name=aerozap-" --format "{{.Names}}\t{{.Status}}\t{{.Image}}"

  echo
  for nome in $(docker ps -a --filter "name=aerozap-" --format "{{.Names}}"); do
    echo "---------------- $nome (últimas 40 linhas) ----------------"
    docker logs --tail 40 "$nome" 2>&1
    echo
  done
  echo "=== Fim ==="
} 2>&1 | tee logs/obs-logs.log

echo
echo "Salvo em logs/obs-logs.log"
