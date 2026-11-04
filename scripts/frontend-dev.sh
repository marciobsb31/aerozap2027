#!/usr/bin/env bash
# Sobe a aerozap-web em modo de desenvolvimento.
# Instala as dependências na primeira vez e abre o servidor em http://localhost:4200.
set -Eeuo pipefail
cd "$(dirname "$0")/../frontend"

if ! command -v node > /dev/null 2>&1; then
  echo "Node não encontrado. Instale o Node 26 (LTS, o mesmo da CI) e tente de novo." >&2
  exit 1
fi

# A faixa é a que o Angular 22 declara em engines. Versões fora dela não são "provavelmente ok":
# o CLI recusa a subir, e descobrir isso no meio do build é pior do que descobrir aqui.
if ! node -e '
  const [a, b] = process.versions.node.split(".").map(Number);
  const ok = (a === 22 && b >= 22) || (a === 24 && b >= 15) || a >= 26;
  process.exit(ok ? 0 : 1);
'; then
  echo "Node $(node -v) não é suportado pelo Angular 22." >&2
  echo "Use 22.22.3+, 24.15+ ou 26+ — o recomendado é o Node 26 LTS, que é o da CI." >&2
  echo "No Windows: winget install OpenJS.NodeJS.LTS" >&2
  exit 1
fi

maior="$(node -p 'process.versions.node.split(".")[0]')"
if [ "$maior" -lt 26 ]; then
  echo "Aviso: Node $(node -v). A CI usa o Node 26 LTS; o 22 só recebe correções de segurança"
  echo "até abril de 2027. Funciona, mas vale atualizar."
fi

if [ ! -d node_modules ]; then
  echo "Instalando dependências (só na primeira vez)…"
  npm ci
fi

echo
echo "A aerozap-web vai abrir em http://localhost:4200"
echo "A API esperada é o gateway em http://localhost:8080 (scripts/dev-up.sh)."
echo "Sem o gateway no ar, a tela inicial mostra 'Plataforma sem resposta' — é o comportamento certo."
echo
exec npm start -- --host 0.0.0.0 --port "${FRONTEND_PORT:-4200}"
