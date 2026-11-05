#!/usr/bin/env bash
# Gera os tipos TypeScript do cliente de API a partir do contrato OpenAPI.
#
# O arquivo gerado é versionado de propósito: quem clona o repositório compila sem rodar o gerador,
# e a CI compara o gerado com o contrato (`--verificar`) para que ninguém mude o contrato sem
# atualizar o cliente — ou o contrário.
#
# O gerador é chamado por `npx` com versão fixa, e não instalado como dependência, porque o
# openapi-typescript 7 declara par com TypeScript 5 enquanto o Angular 22 usa TypeScript 6.
# Instalá-lo no mesmo árvore forçaria `--legacy-peer-deps` no projeto inteiro para conveniência de
# uma ferramenta que só roda quando o contrato muda.
set -Eeuo pipefail

VERSAO_DO_GERADOR="openapi-typescript@7.13.0"
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CONTRATO="$RAIZ/docs/api-contracts/openapi/aerozap-v0.yaml"
SAIDA="$RAIZ/frontend/projects/api-client/src/lib/gerado/openapi.ts"

verificar=false
if [[ "${1:-}" == "--verificar" ]]; then
  verificar=true
fi

if [[ ! -f "$CONTRATO" ]]; then
  echo "Contrato não encontrado: $CONTRATO" >&2
  exit 1
fi

cabecalho() {
  cat <<'AVISO'
/* eslint-disable */
/**
 * ARQUIVO GERADO — NÃO EDITE À MÃO.
 *
 * Origem: docs/api-contracts/openapi/aerozap-v0.yaml
 * Gerador: scripts/gerar-api-client.sh (openapi-typescript)
 *
 * Para atualizar: mude o contrato e rode `bash scripts/gerar-api-client.sh`.
 */

AVISO
}

destino_temporario="$(mktemp -d)"
trap 'rm -rf "$destino_temporario"' EXIT
gerado="$destino_temporario/openapi.ts"

# O contrato referencia os esquemas por caminho relativo (`../schemas`). Montamos a mesma árvore no
# diretório temporário com os esquemas já achatados, para que os `$ref` continuem resolvendo.
mkdir -p "$destino_temporario/openapi"
node "$RAIZ/scripts/lib/achatar-defs.mjs" \
  "$RAIZ/docs/api-contracts/schemas" "$destino_temporario/schemas"
cp "$CONTRATO" "$destino_temporario/openapi/$(basename "$CONTRATO")"

echo "Gerando tipos a partir de ${CONTRATO#"$RAIZ"/}…"
cabecalho > "$gerado"
npx --yes "$VERSAO_DO_GERADOR" "$destino_temporario/openapi/$(basename "$CONTRATO")" >> "$gerado"

if [[ "$verificar" == true ]]; then
  if [[ ! -f "$SAIDA" ]]; then
    echo "FALHA: ${SAIDA#"$RAIZ"/} não existe. Rode: bash scripts/gerar-api-client.sh" >&2
    exit 1
  fi
  if ! diff -u "$SAIDA" "$gerado" > "$destino_temporario/diferenca.txt"; then
    echo "FALHA: o cliente gerado está fora de sincronia com o contrato OpenAPI." >&2
    echo "Rode: bash scripts/gerar-api-client.sh" >&2
    echo >&2
    head -60 "$destino_temporario/diferenca.txt" >&2
    exit 1
  fi
  echo "OK: cliente gerado em sincronia com o contrato."
  exit 0
fi

mkdir -p "$(dirname "$SAIDA")"
cp "$gerado" "$SAIDA"
echo "Escrito: ${SAIDA#"$RAIZ"/}"
