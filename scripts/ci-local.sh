#!/usr/bin/env bash
# Roda localmente as mesmas verificações base do CI (job "Base"). Não precisa de Docker em execução.
cd "$(dirname "$0")/.." || exit 1
FALHAS=0
passo(){ echo; echo "▶ $1"; }
erro(){ echo "✗ $1" >&2; FALHAS=$((FALHAS+1)); }

passo "Lint dos scripts de shell"
if command -v shellcheck >/dev/null 2>&1; then
  shellcheck -S warning scripts/*.sh scripts/lib/*.sh && echo "✓ shellcheck" || erro "shellcheck"
else
  echo "• shellcheck não instalado; o CI executa este passo."
fi

passo "Compose válido"
TMP_ENV=$(mktemp)
cp .env.example "$TMP_ENV"
docker compose --env-file "$TMP_ENV" -f infrastructure/compose/docker-compose.yml config --quiet && echo "✓ compose config" || erro "compose config"
rm -f "$TMP_ENV"

passo "Imagens com versão fixada"
F=infrastructure/compose/docker-compose.yml
if grep -nE '^\s*image:' "$F" | grep -vE 'image:\s*\S+:[0-9A-Za-z][^ :]*\s*$'; then erro "imagem sem tag de versão"; else echo "✓ todas com tag"; fi
if grep -n ':latest' "$F"; then erro "tag latest"; fi

passo "Nenhum .env versionado"
if ! ARQUIVOS=$(git ls-files 2>/dev/null); then erro "não é um repositório Git"
elif echo "$ARQUIVOS" | grep -E '(^|/)\.env($|\.[^e])'; then erro ".env versionado"
else echo "✓ nenhum"; fi

echo
if [ "$FALHAS" -eq 0 ]; then echo "✓ Verificações base OK."; else echo "✗ $FALHAS verificação(ões) falharam." >&2; exit 1; fi
