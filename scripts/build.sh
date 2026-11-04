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

# shellcheck source=scripts/lib/java-env.sh
. scripts/lib/java-env.sh
# shellcheck source=scripts/lib/maven-env.sh
. scripts/lib/maven-env.sh
# Sem pipe: o JAVA_HOME e o PATH precisam valer neste shell.
configurar_java > logs/build.log 2>&1
JAVA_RC=$?
cat logs/build.log
[ "$JAVA_RC" -eq 0 ] || exit 1

# Sem pipe: o MAVEN_REPO_ARGS precisa valer neste shell.
MAVEN_REPO_ARGS=()
configurar_repositorio_maven || exit 1

ARGS=(-B -ntp verify)
if [ "${1:-}" = "--sem-docker" ]; then
  ARGS+=("-Dtest=!*IntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false")
fi

{
  echo "=== AeroZap build — $(date -Iseconds) ==="
  echo "JAVA_HOME=$JAVA_HOME"
  echo "Repositório local do Maven: ${MAVEN_REPO_LOCAL:-padrão (~/.m2/repository)}"
  type -a java mvn 2>&1 | head -n 6
  java -version
  mvn -v
  mvn "${ARGS[@]}" "${MAVEN_REPO_ARGS[@]}"
  echo "=== mvn exit=$? ==="
} 2>&1 | tee -a logs/build.log
exit "${PIPESTATUS[0]}"
