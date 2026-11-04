#!/usr/bin/env bash
# Lista as tags de versão publicadas de cada imagem no Docker Hub, da mais recente para a mais antiga.
# Serve para fixar versões no Compose sem depender de `latest` (regra inegociável do projeto).
#
#   bash scripts/probe-tags.sh                        # imagens padrão do projeto
#   bash scripts/probe-tags.sh grafana/loki@3.        # imagem@filtro (o filtro é um trecho do nome da tag)
#
# Imagens com muitas builds de desenvolvimento (Loki, Tempo) exigem o filtro, senão as
# 100 tags mais recentes são todas de desenvolvimento e nenhuma versão aparece.
# A saída também fica em logs/probe-tags.log (a pasta logs/ não é versionada).
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
mkdir -p logs
LOG=logs/probe-tags.log

IMAGENS_PADRAO=(
  "prom/prometheus@v3."
  "prom/alertmanager@v0."
  "grafana/grafana@13."
  "grafana/loki@3."
  "grafana/tempo@3."
  "grafana/tempo@2."
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

  for entrada in "${IMAGENS[@]}"; do
    imagem="${entrada%%@*}"
    filtro=""
    case "$entrada" in
      *@*) filtro="${entrada#*@}" ;;
    esac

    repositorio="$imagem"
    case "$imagem" in
      */*) : ;;
      *) repositorio="library/$imagem" ;;
    esac

    if [ -n "$filtro" ]; then
      echo "-- $imagem (filtro: $filtro)"
    else
      echo "-- $imagem"
    fi

    url="https://hub.docker.com/v2/repositories/${repositorio}/tags?page_size=100&ordering=last_updated"
    if [ -n "$filtro" ]; then
      url="${url}&name=${filtro}"
    fi

    resposta=$(curl -sS --max-time 30 "$url" 2>&1)

    if [ -z "$resposta" ]; then
      echo "   FALHA: sem resposta do Docker Hub"
      echo
      continue
    fi

    tags=$(printf '%s' "$resposta" \
      | tr ',' '\n' \
      | grep -o '"name":"[^"]*"' \
      | sed 's/^"name":"//; s/"$//' \
      | grep -E '^v?[0-9]+\.[0-9]+(\.[0-9]+)?$' \
      | head -12)

    if [ -z "$tags" ]; then
      echo "   FALHA: nenhuma tag de versão reconhecida (ajuste o filtro). Resposta (início):"
      printf '%s\n' "$resposta" | head -c 240 | sed 's/^/   /'
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
