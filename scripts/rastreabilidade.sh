#!/usr/bin/env bash
# Confere a matriz de rastreabilidade contra o PRD e o disco, e regrava o documento legível.
#
# Com --conferir não escreve nada e falha se algo estiver fora do lugar — é o modo da CI.
set -Eeuo pipefail
cd "$(dirname "$0")/.."

if ! command -v node > /dev/null 2>&1; then
  echo "Node não encontrado; é ele que roda o verificador da matriz." >&2
  exit 1
fi

node scripts/lib/matriz.mjs verificar

if [[ "${1:-}" == "--conferir" ]]; then
  node scripts/lib/matriz.mjs publicar --conferir
else
  node scripts/lib/matriz.mjs publicar
fi
