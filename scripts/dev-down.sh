#!/usr/bin/env bash
# Derruba o ambiente local. Use --apagar-dados para remover também os volumes.
set -euo pipefail
cd "$(dirname "$0")/.."
ARGS=()
if [ "${1:-}" = "--apagar-dados" ]; then ARGS+=(--volumes); echo "• Os dados locais serão apagados."; fi
docker compose --env-file .env -f infrastructure/compose/docker-compose.yml down "${ARGS[@]}"
