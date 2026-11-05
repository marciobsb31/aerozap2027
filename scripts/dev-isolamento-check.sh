#!/usr/bin/env bash
# Prova de ponta a ponta o isolamento entre usuários (RF-007): sobe a infraestrutura, o platform-core
# e o api-gateway, cadastra duas pessoas pela borda e tenta alcançar os dados de uma com o token da
# outra. Usa só dados sintéticos. Log: logs/dev-isolamento-check.log
# Pré-requisito: bash scripts/build.sh (gera os jars).
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1
mkdir -p logs

FALHAS=0
ok(){ echo "✓ $1"; }
falhou(){ echo "✗ $1" >&2; FALHAS=$((FALHAS+1)); }
# Campo de texto de um JSON, sem depender de jq.
campo(){ sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p"; }
# Sujeito do token, sem validar: aqui interessa o conteúdo.
sujeito(){
  local carga
  carga=$(printf '%s' "$1" | cut -d. -f2 | tr '_-' '/+')
  while [ $(( ${#carga} % 4 )) -ne 0 ]; do carga="$carga="; done
  printf '%s' "$carga" | openssl base64 -d -A | campo sub
}

{
  echo "=== dev-isolamento-check — $(date -Iseconds) ==="
  bash scripts/dev-up.sh || { echo "✗ infraestrutura não subiu"; exit 1; }

  set -a; [ -f .env ] && . ./.env; set +a
  # shellcheck source=scripts/lib/java-env.sh
  . scripts/lib/java-env.sh
  configurar_java || exit 1

  SEGREDO="${JWT_SECRET:-$(openssl rand -hex 24)}"
  PORTA_NUCLEO=8081
  PORTA_BORDA=8080
  BORDA="http://localhost:$PORTA_BORDA/api/plataforma"

  achar_jar(){ for f in "$1"/target/*-0*.jar; do [ -f "$f" ] && printf '%s' "$f" && return 0; done; return 1; }
  JAR_NUCLEO=$(achar_jar apps/platform-core) || { echo "✗ jar do platform-core não encontrado; rode bash scripts/build.sh"; exit 1; }
  JAR_BORDA=$(achar_jar apps/api-gateway) || { echo "✗ jar do api-gateway não encontrado; rode bash scripts/build.sh"; exit 1; }

  SERVER_PORT="$PORTA_NUCLEO" JWT_SECRET="$SEGREDO" java -jar "$JAR_NUCLEO" > logs/platform-core.log 2>&1 &
  PID_NUCLEO=$!
  GATEWAY_PORT="$PORTA_BORDA" JWT_SECRET="$SEGREDO" LIMITE_HABILITADO=false \
    DESTINO_PLATFORM_CORE="http://localhost:$PORTA_NUCLEO/api" java -jar "$JAR_BORDA" > logs/api-gateway.log 2>&1 &
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
  esperar "$PORTA_NUCLEO" "$PID_NUCLEO" || { falhou "platform-core não subiu"; tail -n 30 logs/platform-core.log; exit 1; }
  esperar "$PORTA_BORDA" "$PID_BORDA" || { falhou "api-gateway não subiu"; tail -n 30 logs/api-gateway.log; exit 1; }
  ok "platform-core e api-gateway de pé"

  registrar(){
    curl -s -X POST "$BORDA/v1/auth/registrar" -H 'Content-Type: application/json' \
      -d "{\"email\":\"$1\",\"senha\":\"uma senha longa o bastante\",\"nome\":\"Teste\",\"idioma\":\"pt-BR\",\"versaoDosTermos\":\"2026-11-01\"}"
  }
  codigo(){ curl -s -o /dev/null -w '%{http_code}' "$@"; }

  MARCA=$(date +%s)
  EMAIL_ANA="ana-$MARCA@exemplo.test"
  EMAIL_BIA="bia-$MARCA@exemplo.test"

  echo
  echo "— 1. Cadastro e login pela borda, sem token —"
  RESPOSTA_ANA=$(registrar "$EMAIL_ANA")
  RESPOSTA_BIA=$(registrar "$EMAIL_BIA")
  ACESSO_ANA=$(printf '%s' "$RESPOSTA_ANA" | campo accessToken)
  ACESSO_BIA=$(printf '%s' "$RESPOSTA_BIA" | campo accessToken)
  REFRESH_ANA=$(printf '%s' "$RESPOSTA_ANA" | campo refreshToken)
  [ -n "$ACESSO_ANA" ] && [ -n "$ACESSO_BIA" ] && ok "duas contas criadas" || { falhou "cadastro falhou"; exit 1; }
  ID_BIA=$(sujeito "$ACESSO_BIA")
  [ -n "$ID_BIA" ] && ok "identificador da outra pessoa conhecido, como um atacante poderia ter" || falhou "sem sujeito no token"
  [ "$(codigo -X POST "$BORDA/v1/auth/entrar" -H 'Content-Type: application/json' \
      -d "{\"email\":\"$EMAIL_ANA\",\"senha\":\"uma senha longa o bastante\"}")" = "200" ] \
    && ok "login pela borda" || falhou "login pela borda não funcionou"

  echo
  echo "— 2. Com o token da Ana, tentando alcançar a Bia —"
  PERFIL=$(curl -s "$BORDA/v1/auth/eu" -H "Authorization: Bearer $ACESSO_ANA")
  echo "$PERFIL" | grep -q "$EMAIL_ANA" && ok "o token da Ana lê o perfil da Ana" || falhou "perfil da Ana não veio: $PERFIL"
  FORJADO=$(curl -s "$BORDA/v1/auth/eu" -H "Authorization: Bearer $ACESSO_ANA" -H "X-Usuario-Id: $ID_BIA")
  if echo "$FORJADO" | grep -q "$EMAIL_BIA"; then
    falhou "CRÍTICO: cabeçalho forjado devolveu o perfil de outra pessoa"
  elif echo "$FORJADO" | grep -q "$EMAIL_ANA"; then
    ok "cabeçalho X-Usuario-Id forjado foi ignorado"
  else
    falhou "resposta inesperada ao forjar: $FORJADO"
  fi
  MINUSCULO=$(curl -s "$BORDA/v1/auth/eu" -H "Authorization: Bearer $ACESSO_ANA" -H "x-usuario-id: $ID_BIA")
  echo "$MINUSCULO" | grep -q "$EMAIL_BIA" && falhou "CRÍTICO: forjar em minúsculas funcionou" || ok "forjar em minúsculas também não funciona"
  CONSULTA=$(curl -s "$BORDA/v1/auth/eu?userId=$ID_BIA&user_id=$ID_BIA" -H "Authorization: Bearer $ACESSO_ANA")
  echo "$CONSULTA" | grep -q "$EMAIL_BIA" && falhou "CRÍTICO: parâmetro devolveu outra pessoa" || ok "identificador na query é ignorado"

  echo
  echo "— 3. Sem token, ou com token inválido —"
  [ "$(codigo "$BORDA/v1/auth/eu")" = "401" ] && ok "perfil sem token: 401" || falhou "perfil sem token não deu 401"
  [ "$(codigo "$BORDA/v1/auth/eu" -H "X-Usuario-Id: $ID_BIA")" = "401" ] && ok "só o cabeçalho, sem token: 401" || falhou "cabeçalho sem token passou"
  [ "$(codigo "$BORDA/v1/usage/saldo")" = "401" ] && ok "saldo sem token: 401" || falhou "saldo sem token não deu 401"
  [ "$(codigo "$BORDA/v1/auth/eu" -H "Authorization: Bearer ${ACESSO_ANA%?}x")" = "401" ] && ok "token adulterado: 401" || falhou "token adulterado passou"

  echo
  echo "— 4. Saldo de cada um —"
  SALDO=$(curl -s "$BORDA/v1/usage/saldo" -H "Authorization: Bearer $ACESSO_ANA" -H "X-Usuario-Id: $ID_BIA")
  PERIODO_FORJADO=$(printf '%s' "$SALDO" | campo periodoId)
  PERIODO_BIA=$(curl -s "$BORDA/v1/usage/saldo" -H "Authorization: Bearer $ACESSO_BIA" | campo periodoId)
  [ -n "$PERIODO_FORJADO" ] && [ "$PERIODO_FORJADO" != "$PERIODO_BIA" ] \
    && ok "forjar o cabeçalho não devolve o saldo da Bia" || falhou "saldo cruzado: $SALDO"

  echo
  echo "— 5. Rotas internas fora de alcance —"
  for caminho in \
    "/internal/usage/saldo" \
    "/api/plataforma/../../internal/usage/saldo" \
    "/api/plataforma/%2e%2e/%2e%2e/internal/usage/saldo" \
    "/api/plataforma/..%2f..%2finternal/usage/saldo"; do
    C=$(codigo --path-as-is "http://localhost:$PORTA_BORDA$caminho" -H "Authorization: Bearer $ACESSO_ANA" -H "X-Usuario-Id: $ID_BIA")
    case "$C" in
      4??) ok "$caminho → $C" ;;
      *) falhou "$caminho respondeu $C" ;;
    esac
  done

  echo
  echo "— 6. Refresh reutilizado pela borda —"
  SEGUNDO=$(curl -s -X POST "$BORDA/v1/auth/renovar" -H 'Content-Type: application/json' \
    -d "{\"refreshToken\":\"$REFRESH_ANA\"}" | campo refreshToken)
  [ -n "$SEGUNDO" ] && ok "renovação sem token de acesso funciona" || falhou "renovação falhou"
  [ "$(codigo -X POST "$BORDA/v1/auth/renovar" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH_ANA\"}")" = "401" ] \
    && ok "refresh reutilizado: 401" || falhou "refresh reutilizado foi aceito"
  [ "$(codigo -X POST "$BORDA/v1/auth/renovar" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$SEGUNDO\"}")" = "401" ] \
    && ok "a família inteira caiu" || falhou "o token novo da família continuou valendo"

  echo
  if [ "$FALHAS" -ne 0 ]; then
    echo "✗ $FALHAS verificação(ões) falharam."
  else
    echo "✓ Isolamento verificado de ponta a ponta."
  fi
} 2>&1 | tee logs/dev-isolamento-check.log

grep -q '^✗' logs/dev-isolamento-check.log && exit 1 || exit 0
