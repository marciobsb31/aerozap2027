#!/usr/bin/env bash
# Sobe a infraestrutura local, inicia o platform-core e confere o health check. Log: logs/dev-service-check.log
# Pré-requisito: bash scripts/build.sh (gera o jar em apps/platform-core/target).
cd "$(dirname "$0")/.." || exit 1
mkdir -p logs

{
  echo "=== dev-service-check — $(date -Iseconds) ==="
  bash scripts/dev-up.sh || { echo "✗ infraestrutura não subiu"; exit 1; }

  # O perfil pedido na linha de comando soma aos do .env; sem isto, o .env o sobrescreveria.
  PERFIL_PEDIDO="${SPRING_PROFILES_ACTIVE:-}"
  set -a; . ./.env; set +a
  if [ -n "$PERFIL_PEDIDO" ]; then
    export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:+$SPRING_PROFILES_ACTIVE,}$PERFIL_PEDIDO"
  fi
  # shellcheck source=scripts/lib/java-env.sh
  . scripts/lib/java-env.sh
  configurar_java || exit 1
  echo "• java: $(java -version 2>&1 | head -n 1)"
  JAR=""
  for f in apps/platform-core/target/platform-core-*.jar; do [ -f "$f" ] && JAR="$f" && break; done
  if [ -z "$JAR" ]; then echo "✗ jar não encontrado; rode: bash scripts/build.sh --sem-docker"; exit 1; fi

  PORT="${SERVER_PORT:-8081}"
  java -jar "$JAR" > logs/platform-core.log 2>&1 &
  PID=$!
  trap 'kill "$PID" 2>/dev/null' EXIT

  echo "• Aguardando o serviço em http://localhost:$PORT ..."
  OK=0
  for _ in $(seq 1 45); do
    if curl -fsS "http://localhost:$PORT/actuator/health" 2>/dev/null | grep -q UP; then OK=1; break; fi
    if ! kill -0 "$PID" 2>/dev/null; then echo "✗ o processo java terminou sozinho."; break; fi
    sleep 2
  done

  if [ "$OK" -ne 1 ]; then
    echo "✗ o serviço não ficou saudável. Últimas linhas do log:"; tail -n 40 logs/platform-core.log; exit 1
  fi
  echo "✓ /actuator/health = UP"
  echo "• /api/v1/system/ping:"; curl -fsS -H "X-Correlation-Id: verificacao-1" "http://localhost:$PORT/api/v1/system/ping"; echo
  echo "• /v3/api-docs (início):"; curl -fsS "http://localhost:$PORT/v3/api-docs" | head -c 200; echo
  echo "✓ platform-core saudável com PostgreSQL e RabbitMQ reais."
  case ",${SPRING_PROFILES_ACTIVE:-}," in
    *,observabilidade,*)
      # Dá tempo de o Prometheus coletar e de o lote de logs e rastros sair antes de encerrar.
      echo "• Aguardando 30 s para os sinais chegarem à observabilidade..."
      sleep 30
      ;;
  esac
} 2>&1 | tee logs/dev-service-check.log
exit "${PIPESTATUS[0]}"
