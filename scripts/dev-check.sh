#!/usr/bin/env bash
# Sobe o ambiente, verifica cada serviço e grava toda a saída em logs/dev-check.log
# (a pasta logs/ não é versionada). Em caso de falha, anexa o estado e os logs dos contêineres.
cd "$(dirname "$0")/.."
mkdir -p logs
LOG=logs/dev-check.log
COMPOSE=(docker compose --env-file .env -f infrastructure/compose/docker-compose.yml)
{
  echo "=== AeroZap dev-check — $(date -Iseconds) ==="
  docker --version; docker compose version
  echo; echo "=== dev-up ==="
  bash scripts/dev-up.sh; UP=$?
  echo; echo "=== dev-verify ==="
  bash scripts/dev-verify.sh; VER=$?
  if [ $UP -ne 0 ] || [ $VER -ne 0 ]; then
    echo; echo "=== FALHA (up=$UP verify=$VER): estado e logs ==="
    "${COMPOSE[@]}" ps -a
    "${COMPOSE[@]}" logs --tail=40 --no-color
  fi
  echo; echo "=== fim (up=$UP verify=$VER) ==="
} 2>&1 | tee "$LOG"
echo
echo "Log salvo em $LOG"
