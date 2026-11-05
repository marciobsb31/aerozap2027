#!/usr/bin/env bash
# Repositório local do Maven fora do disco do sistema.
#
# Por padrão o Maven guarda tudo em ~/.m2/repository, que no Windows fica no C:.
# Definindo MAVEN_REPO_LOCAL (no ambiente ou no .env), o cache passa para o caminho
# escolhido — normalmente o mesmo disco do projeto. Use barras normais: D:/Aerozap-dev/maven-repo
#
# Uso:
#   . scripts/lib/maven-env.sh
#   configurar_repositorio_maven
#   mvn "${MAVEN_REPO_ARGS[@]}" verify

# Preenche MAVEN_REPO_ARGS com -Dmaven.repo.local=... quando houver caminho configurado
# e exporta MAVEN_REPO_LOCAL com o caminho resolvido.
# shellcheck disable=SC2034  # MAVEN_REPO_ARGS é consumido por quem chama (scripts/build.sh).
configurar_repositorio_maven() {
  MAVEN_REPO_ARGS=()
  local valor="${MAVEN_REPO_LOCAL:-}"

  if [ -z "$valor" ] && [ -f .env ]; then
    valor=$(sed -n 's/^[[:space:]]*MAVEN_REPO_LOCAL[[:space:]]*=[[:space:]]*//p' .env \
      | tail -n 1 | tr -d '\r' | sed 's/^"\(.*\)"$/\1/; s/^'"'"'\(.*\)'"'"'$/\1/')
  fi

  if [ -z "$valor" ]; then
    echo "• Repositório local do Maven: padrão (~/.m2/repository). Para movê-lo, defina MAVEN_REPO_LOCAL no .env."
    return 0
  fi

  if ! mkdir -p "$valor" 2>/dev/null; then
    echo "✗ Não foi possível criar o repositório local do Maven em: $valor"
    echo "  Verifique se o disco existe e se há permissão de escrita."
    return 1
  fi

  MAVEN_REPO_ARGS=("-Dmaven.repo.local=$valor")
  MAVEN_REPO_LOCAL="$valor"
  export MAVEN_REPO_LOCAL
  echo "• Repositório local do Maven: $valor"
}
