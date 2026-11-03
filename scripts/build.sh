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

# JAVA_HOME apontando para um JDK que não existe mais (comum após atualizar/remover o JDK) quebra o Maven.
# Nesse caso ele é ignorado e o Java do PATH é usado.
if [ -n "${JAVA_HOME:-}" ]; then
  JH="$JAVA_HOME"
  if command -v cygpath >/dev/null 2>&1; then JH="$(cygpath -u "$JAVA_HOME" 2>/dev/null || echo "$JAVA_HOME")"; fi
  if [ ! -x "$JH/bin/java" ] && [ ! -x "$JH/bin/java.exe" ]; then
    echo "• JAVA_HOME ($JAVA_HOME) não existe; ignorando e usando o Java do PATH."
    unset JAVA_HOME
  fi
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
