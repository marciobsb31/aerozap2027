#!/usr/bin/env bash
# Roda no frontend exatamente o que a CI roda, na mesma ordem, e grava a evidência em
# logs/frontend-check.log. Serve para descobrir aqui o que a CI descobriria depois.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
mkdir -p logs

FALHAS=0
passo() {
  local nome="$1"
  shift
  echo
  echo "── $nome"
  if "$@"; then
    echo "✓ $nome"
  else
    echo "✗ $nome" >&2
    FALHAS=$((FALHAS + 1))
  fi
}

{
  echo "frontend-check — $(date -Is)"
  echo "node $(node -v 2>/dev/null || echo ausente) / npm $(npm -v 2>/dev/null || echo ausente)"

  cd frontend || exit 1

  if [ -f package-lock.json ]; then
    passo "dependências (npm ci)" npm ci
  else
    passo "dependências (npm install)" npm install
  fi

  passo "auditoria (falha só em crítica)" npm audit --audit-level=critical
  passo "cliente de API em sincronia com o contrato" npm run verificar:contrato
  passo "lint" npm run lint
  passo "testes unitários" npm test
  passo "build das bibliotecas" npm run build:bibliotecas
  passo "build de produção da aplicação" npm run build

  echo
  if [ "$FALHAS" -eq 0 ]; then
    echo "RESULTADO: tudo verde."
  else
    echo "RESULTADO: $FALHAS passo(s) falharam."
  fi
  # O bloco inteiro roda num subprocesso por causa do tee, então a contagem volta por arquivo:
  # sem isso o script terminaria sempre em zero e a CI chamaria de verde o que falhou.
  echo "$FALHAS" > ../logs/.frontend-check-falhas
} 2>&1 | tee logs/frontend-check.log

falhas="$(cat logs/.frontend-check-falhas 2>/dev/null || echo 1)"
rm -f logs/.frontend-check-falhas
[ "$falhas" -eq 0 ]
