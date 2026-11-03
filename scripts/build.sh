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

# Devolve o número principal da versão de um JDK (ex.: 21), ou vazio se não for um JDK válido.
jdk_major() {
  local out
  out="$("$1" -version 2>&1 | head -n 1)" || return 0
  echo "$out" | sed -n 's/.*version "\([0-9]*\).*/\1/p'
}

# Procura JDKs instalados nos locais comuns do Windows e escolhe o 21 (ou o primeiro >= 21).
escolher_jdk() {
  local base d bin major best_dir="" best_major=""
  for base in "$HOME/.jdks" "$HOME/.jdk" "/c/Program Files/Eclipse Adoptium" "/c/Program Files/Java" \
              "/c/Program Files/Microsoft" "/c/Program Files/Zulu" "/c/Program Files/BellSoft" \
              "/c/Program Files/Amazon Corretto" "/d/Program Files/Java"; do
    for d in "$base"/*; do
      bin=""
      [ -x "$d/bin/java.exe" ] && bin="$d/bin/java.exe"
      [ -z "$bin" ] && [ -x "$d/bin/java" ] && bin="$d/bin/java"
      [ -z "$bin" ] && continue
      major="$(jdk_major "$bin")"
      [ -z "$major" ] && continue
      [ "$major" -lt 21 ] && continue
      if [ -z "$best_dir" ] || [ "$major" -eq 21 ] && [ "$best_major" != "21" ]; then
        best_dir="$d"; best_major="$major"
      fi
    done
  done
  echo "$best_dir"
}

JH="${JAVA_HOME:-}"
if [ -n "$JH" ] && command -v cygpath >/dev/null 2>&1; then JH="$(cygpath -u "$JH" 2>/dev/null || echo "$JH")"; fi
if [ -z "$JH" ] || { [ ! -x "$JH/bin/java.exe" ] && [ ! -x "$JH/bin/java" ]; }; then
  [ -n "${JAVA_HOME:-}" ] && echo "• JAVA_HOME ($JAVA_HOME) não existe; procurando um JDK instalado."
  ACHADO="$(escolher_jdk)"
  if [ -z "$ACHADO" ]; then
    echo "✗ Nenhum JDK 21+ encontrado. Instale um JDK 21 (winget install EclipseAdoptium.Temurin.21.JDK) e rode de novo." | tee logs/build.log
    exit 1
  fi
  export JAVA_HOME="$ACHADO"
  echo "• Usando o JDK em $JAVA_HOME"
fi
export PATH="$JAVA_HOME/bin:$PATH"

ARGS=(-B -ntp verify)
if [ "${1:-}" = "--sem-docker" ]; then
  ARGS+=("-Dtest=!*IntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false")
fi

{
  echo "=== AeroZap build — $(date -Iseconds) ==="
  echo "JAVA_HOME=$JAVA_HOME"
  type -a java mvn 2>&1 | head -n 6
  java -version
  mvn -v
  mvn "${ARGS[@]}"
  echo "=== mvn exit=$? ==="
} 2>&1 | tee logs/build.log
exit "${PIPESTATUS[0]}"
