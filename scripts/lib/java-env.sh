#!/usr/bin/env bash
# Funções compartilhadas: localizam um JDK 21+ instalado e configuram JAVA_HOME e PATH.
# Uso: . scripts/lib/java-env.sh && configurar_java || exit 1

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
      if [ -z "$best_dir" ] || { [ "$major" -eq 21 ] && [ "$best_major" != "21" ]; }; then
        best_dir="$d"; best_major="$major"
      fi
    done
  done
  echo "$best_dir"
}

# Garante um JAVA_HOME válido (descarta um inválido) e coloca o java dele na frente do PATH.
configurar_java() {
  local jh="${JAVA_HOME:-}" achado
  if [ -n "$jh" ] && command -v cygpath >/dev/null 2>&1; then jh="$(cygpath -u "$jh" 2>/dev/null || echo "$jh")"; fi
  if [ -z "$jh" ] || { [ ! -x "$jh/bin/java.exe" ] && [ ! -x "$jh/bin/java" ]; }; then
    [ -n "${JAVA_HOME:-}" ] && echo "• JAVA_HOME (${JAVA_HOME}) não existe; procurando um JDK instalado."
    achado="$(escolher_jdk)"
    if [ -z "$achado" ]; then
      echo "✗ Nenhum JDK 21+ encontrado. Instale um JDK 21 (winget install EclipseAdoptium.Temurin.21.JDK) e rode de novo."
      return 1
    fi
    export JAVA_HOME="$achado"
    echo "• Usando o JDK em $JAVA_HOME"
  fi
  export PATH="$JAVA_HOME/bin:$PATH"
}
