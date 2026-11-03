#!/usr/bin/env bash
# Compila e testa todos os módulos Java e grava a saída em logs/build.log (pasta não versionada).
# Requisitos: JDK 21+, Maven e, para os testes de integração, Docker em execução (Testcontainers).
# Opção: --sem-docker pula os testes *IntegrationTest (só compila e roda os testes unitários).
cd "$(dirname "$0")/.." || exit 1
mkdir -p logs

if ! command -v mvn >/dev/null 2>&1; then
  echo "✗ Maven (mvn) não encontrado no PATH. Instale o Maven 3.9+ (no Windows: winget install Apache.Maven) e abra um novo terminal." | tee logs/build.log
  exit 1
fi

ARGS=(-B -ntp verify)
if [ "${1:-}" = "--sem-docker" ]; then
  ARGS+=("-Dtest=!*IntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false")
fi

{
  echo "=== AeroZap build — $(date -Iseconds) ==="
  java -version
  mvn -v
  mvn "${ARGS[@]}"
  echo "=== mvn exit=$? ==="
} 2>&1 | tee logs/build.log
exit "${PIPESTATUS[0]}"
