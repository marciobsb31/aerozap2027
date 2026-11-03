#!/usr/bin/env bash
# Lista as tags de versão publicadas de cada imagem no Docker Hub, da mais recente para a mais antiga.
# Serve para fixar versões no Compose sem depender de `latest` (regra inegociável do projeto).
#
#   bash scripts/probe-tags.sh                      # imagens padrão do projeto
#   bash scripts/probe-tags.sh grafana/grafana      # imagens específicas
#
# A saída também fica em logs/probe-tags.log (a pasta logs/ não é versionada).
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
mkdir -p logs
LOG=logs/probe-tags.log

IMAGENS_PADRAO=(
  "prom/prometheus"
  "prom/alertmanager"
  "grafana/grafana"
  "grafana/loki"
  "grafana/tempo"
)

if [ "$#" -gt 0 ]; then
  IMAGENS=("$@")
else
  IMAGENS=("${IMAGENS_PADRAO[@]}")
fi

{
  echo "== Tags publicadas no Docker Hub =="
  echo "Data: $(date -Is)"
  echo

  for imagem in "${IMAGENS[@]}"; do
    repositorio="$imagem"
    case "$imagem" in
      */*) : ;;
      *) repositorio="library/$imagem" ;;
    esac

    echo "-- $imagem"
    resposta=$(curl -sS --max-time 30 \
      "https://hub.docker.com/v2/repositories/${repositorio}/tags?page_size=100&ordering=last_updated" 2>&1)

    if [ -z "$resposta" ]; then
      echo "   FALHA: sem resposta do Docker Hub"
      echo
      continue
    fi

    tags=$(printf '%s' "$resposta" \
      | tr ',' '\n' \
      | grep -o '"name":"[^"]*"' \
      | sed 's/^"name":"//; s/"$//' \
      | grep -E '^v?[0-9]+\.[0-9]+(\.[0-9]+)?(-[A-Za-z0-9.]+)?$' \
      | head -15)

    if [ -z "$tags" ]; then
      echo "   FALHA: nenhuma tag de versão reconhecida. Resposta (início):"
      printf '%s\n' "$resposta" | head -c 300 | sed 's/^/   /'
      echo
      continue
    fi

    printf '%s\n' "$tags" | sed 's/^/   /'
    echo
  done

  echo "== Fim =="
} 2>&1 | tee "$LOG"

echo
echo "Salvo em $LOG"
