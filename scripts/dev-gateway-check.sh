#!/usr/bin/env bash
# Verifica a borda de ponta a ponta: sobe a infraestrutura, o platform-core e o api-gateway,
# e prova os três critérios da AZ-015 — sem token é rejeitado, com token passa e encaminha,
# e o limite de taxa responde 429. Log: logs/dev-gateway-check.log
# Pré-requisito: bash scripts/build.sh (gera os jars).
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
mkdir -p logs

FALHAS=0
ok(){ echo "✓ $1"; }
falhou(){ echo "✗ $1" >&2; FALHAS=$((FALHAS+1)); }

# Codificação base64 para URL, sem preenchimento (formato do JWT).
base64url(){ openssl base64 -A | tr '+/' '-_' | tr -d '='; }

{
  echo "=== dev-gateway-check — $(date -Iseconds) ==="
  bash scripts/dev-up.sh || { echo "✗ infraestrutura não subiu"; exit 1; }

  set -a; [ -f .env ] && . ./.env; set +a
  # shellcheck source=scripts/lib/java-env.sh
  . scripts/lib/java-env.sh
  configurar_java || exit 1

  SEGREDO="${JWT_SECRET:-}"
  if [ -z "$SEGREDO" ]; then
    SEGREDO=$(openssl rand -hex 24)
    echo "• JWT_SECRET não está no .env; usando um segredo gerado só para esta verificação."
  fi
  EMISSOR="${JWT_EMISSOR:-https://aerozap.local/identidade}"
  PUBLICO="${JWT_PUBLICO:-aerozap-api}"
  PORTA_NUCLEO=8081
  PORTA_BORDA=8080
  LIMITE=5

  achar_jar(){ for f in "$1"/target/*-0*.jar; do [ -f "$f" ] && printf '%s' "$f" && return 0; done; return 1; }
  JAR_NUCLEO=$(achar_jar apps/platform-core) || { echo "✗ jar do platform-core não encontrado; rode bash scripts/build.sh"; exit 1; }
  JAR_BORDA=$(achar_jar apps/api-gateway) || { echo "✗ jar do api-gateway não encontrado; rode bash scripts/build.sh"; exit 1; }

  SERVER_PORT="$PORTA_NUCLEO" java -jar "$JAR_NUCLEO" > logs/platform-core.log 2>&1 &
  PID_NUCLEO=$!
  GATEWAY_PORT="$PORTA_BORDA" JWT_SECRET="$SEGREDO" JWT_EMISSOR="$EMISSOR" JWT_PUBLICO="$PUBLICO" \
    LIMITE_REQUISICOES="$LIMITE" LIMITE_JANELA=2m DESTINO_PLATFORM_CORE="http://localhost:$PORTA_NUCLEO/api" \
    java -jar "$JAR_BORDA" > logs/api-gateway.log 2>&1 &
  PID_BORDA=$!
  trap 'kill "$PID_NUCLEO" "$PID_BORDA" 2>/dev/null' EXIT

  esperar(){
    for _ in $(seq 1 45); do
      curl -fsS "http://localhost:$1/actuator/health" 2>/dev/null | grep -q UP && return 0
      kill -0 "$2" 2>/dev/null || return 1
      sleep 2
    done
    return 1
  }

  echo "• Aguardando platform-core em $PORTA_NUCLEO ..."
  esperar "$PORTA_NUCLEO" "$PID_NUCLEO" && ok "platform-core saudável" || {
    falhou "platform-core não subiu"; tail -n 30 logs/platform-core.log; exit 1; }

  echo "• Aguardando api-gateway em $PORTA_BORDA ..."
  esperar "$PORTA_BORDA" "$PID_BORDA" && ok "api-gateway saudável" || {
    falhou "api-gateway não subiu"; tail -n 30 logs/api-gateway.log; exit 1; }

  # --- token de teste assinado em HS256 ---
  CABECALHO=$(printf '%s' '{"alg":"HS256","typ":"JWT"}' | base64url)
  AGORA=$(date +%s)
  CORPO=$(printf '{"sub":"verificacao-az015","iss":"%s","aud":"%s","iat":%s,"exp":%s}' \
    "$EMISSOR" "$PUBLICO" "$AGORA" "$((AGORA + 600))" | base64url)
  ASSINATURA=$(printf '%s' "$CABECALHO.$CORPO" | openssl dgst -sha256 -hmac "$SEGREDO" -binary | base64url)
  TOKEN="$CABECALHO.$CORPO.$ASSINATURA"

  echo
  echo "— 1. Requisição sem token —"
  CODIGO=$(curl -s -o logs/gateway-sem-token.json -w '%{http_code}' \
    "http://localhost:$PORTA_BORDA/api/plataforma/v1/system/ping")
  [ "$CODIGO" = "401" ] && ok "sem token: 401" || falhou "sem token devolveu $CODIGO (esperado 401)"
  echo "  corpo: $(head -c 200 logs/gateway-sem-token.json)"

  echo
  echo "— 2. Requisição com token válido, encaminhada ao platform-core —"
  RESPOSTA=$(curl -s -D logs/gateway-cabecalhos.txt -H "Authorization: Bearer $TOKEN" \
    -H "X-Correlation-Id: verificacao-az015" \
    "http://localhost:$PORTA_BORDA/api/plataforma/v1/system/ping")
  echo "  corpo: $RESPOSTA"
  echo "$RESPOSTA" | grep -q '"data"' && ok "resposta do serviço chegou pela borda" || falhou "a borda não devolveu a resposta do serviço"
  grep -qi "x-correlation-id: verificacao-az015" logs/gateway-cabecalhos.txt \
    && ok "correlação preservada de ponta a ponta" || falhou "correlação não voltou no cabeçalho"

  echo
  echo "— 3. Limite de taxa (limite de $LIMITE por janela) —"
  VISTO_429=0
  for i in $(seq 1 $((LIMITE + 4))); do
    C=$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TOKEN" \
      "http://localhost:$PORTA_BORDA/api/plataforma/v1/system/ping")
    printf '  requisição %s → %s\n' "$i" "$C"
    [ "$C" = "429" ] && VISTO_429=1
  done
  [ "$VISTO_429" -eq 1 ] && ok "limite de taxa respondeu 429" || falhou "o limite de taxa não bloqueou"

  echo
  if [ "$FALHAS" -eq 0 ]; then
    echo "✓ Borda verificada: autenticação, encaminhamento, correlação e limite de taxa."
  else
    echo "✗ $FALHAS verificação(ões) falharam."
  fi
} 2>&1 | tee logs/dev-gateway-check.log

grep -q '^✗' logs/dev-gateway-check.log && exit 1 || exit 0
