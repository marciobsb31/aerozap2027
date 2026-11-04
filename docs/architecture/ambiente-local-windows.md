# Ambiente local no Windows: manter o disco do sistema livre

O projeto fica no `D:`, mas as ferramentas, por padrão, escrevem no perfil do usuário, no `C:`.
Em pouco tempo isso enche o disco do sistema. Esta página reúne o que cada ferramenta grava e
como apontá-la para o mesmo disco do projeto.

| O que grava | Onde por padrão | Tamanho típico | Como mover |
|---|---|---:|---|
| Disco virtual do Docker (imagens, volumes, cache de build) | `C:\Users\<você>\AppData\Local\Docker` | 20–60 GB | ajuste na interface do Docker Desktop (abaixo) |
| Repositório local do Maven | `C:\Users\<você>\.m2\repository` | 1–3 GB | `MAVEN_REPO_LOCAL` no `.env` |
| Cache do npm (front-end, a partir da AZ-016) | `C:\Users\<você>\AppData\Local\npm-cache` | 0,5–2 GB | `NPM_CONFIG_CACHE` no `.env` ou `npm config set cache` |
| Compilados e logs do projeto | `target/`, `logs/` dentro do projeto | — | já ficam no disco do projeto |

Para medir o que está ocupando espaço hoje, o script `.scratch/espaco.sh` (não versionado)
imprime o espaço livre de cada disco e o tamanho de cada pasta de cache.

## Docker: mover o disco virtual

É o maior ganho e é um ajuste de tela. O Docker move os dados sozinho e derruba os contêineres
durante a operação, que pode levar alguns minutos.

1. Docker Desktop → **Settings** → **Resources** → **Advanced**
2. Em **Disk image location**, clique em **Browse**
3. Escolha uma pasta no disco do projeto, por exemplo `D:\Docker`
4. **Apply & restart**

Confira o espaço livre do destino antes: o disco virtual atual precisa caber inteiro lá.

## Maven: repositório local

No `.env` (nunca versionado), use barras normais:

```
MAVEN_REPO_LOCAL=D:/Aerozap/.cache/maven-repo
```

O `scripts/build.sh` cria a pasta e passa `-Dmaven.repo.local` ao Maven; `.cache/` já está no
`.gitignore`. Depois do primeiro build bem-sucedido, a pasta antiga
(`C:\Users\<você>\.m2\repository`) pode ser apagada — nada mais escreve nela.

Qualquer caminho serve, inclusive fora do projeto (`D:/maven-repo`). Ficar dentro de
`D:\Aerozap\.cache` tem uma vantagem prática: tudo do projeto, inclusive as dependências
baixadas, vive numa única pasta — fácil de copiar, apagar ou medir.

## npm: cache

A partir da AZ-016, com o front-end, acrescente no `.env`:

```
NPM_CONFIG_CACHE=D:/Aerozap/.cache/npm
```

O npm lê `NPM_CONFIG_CACHE` do ambiente. Para valer em qualquer terminal, sem depender do
`.env`, use `npm config set cache D:/Aerozap/.cache/npm`.

## O que não vale a pena mover

Arquivos de configuração pequenos — `~/.testcontainers.properties`, `~/.docker/config.json`,
`~/.gitconfig` — somam poucos KB. Mover traz risco sem ganho.
