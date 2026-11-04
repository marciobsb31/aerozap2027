# Pipeline de CI (AZ-012)

**Dono:** A6 (DevOps/SRE) · **Arquivos:** `.github/workflows/`, `.github/dependabot.yml`, `.gitleaks.toml`, `scripts/ci-local.sh`

## O que roda e quando
Gatilhos: pull request, push em `main`/`develop` e execução manual. Execuções antigas da mesma branch são canceladas.

| Job | Roda | O que faz | Falha quando |
|---|---|---|---|
| **Base** | sempre | gitleaks (histórico completo), shellcheck, `docker compose config`, imagens com tag fixa, nenhum `.env` versionado | segredo encontrado, script com aviso, Compose inválido, tag `latest` |
| **Revisão de dependências** | só em PR | `dependency-review-action` | dependência nova com vulnerabilidade **alta** ou pior. **Hoje tolerada** (`continue-on-error`) até o grafo de dependências ser ativado no GitHub |
| **Detectar diretórios** | sempre | `paths-filter` decide o que mudou | — |
| **api-gateway, platform-core, search, conversation-ai, messaging** | se `apps/<serviço>`, `libs/java` ou `pom.xml` mudou | Maven `verify` (lint, testes unitários e de integração com Testcontainers), CodeQL (SAST), build da imagem se houver Dockerfile | teste falha. A varredura de imagem (Trivy) entra com o primeiro Dockerfile. Os achados do CodeQL aparecem em *code scanning*; para bloqueá-los, exija o resultado do CodeQL na proteção de branch |
| **Frontend** | se `frontend/` ou `libs/typescript` mudou | `npm ci`, `npm audit`, lint, testes, build, CodeQL | vulnerabilidade **crítica**, lint, teste ou build falha |
| **CI OK** | sempre | resultado único dos jobs acima | qualquer job com falha ou cancelado |

Jobs pulados (nada mudou naquele diretório) contam como sucesso em **CI OK**. Use **CI OK** como verificação obrigatória na proteção de branch.

## Estado atual
Ainda não há `pom.xml` nem `package.json`. Os fluxos de Java e Angular **já existem**, mas pulam os passos de build com um aviso e terminam verdes. Eles passam a trabalhar de verdade quando o primeiro serviço (AZ-014) e o frontend (AZ-016) chegarem. **Esses passos ainda não foram exercitados.**

## Cache e tempo
- Maven: cache de `~/.m2` por `actions/setup-java` (`cache: maven`). Node: cache do npm por `actions/setup-node`.
- Limites de tempo por job: base 10 min, serviços e frontend 30 min.
- **Duração real: não medida.** Será registrada após as primeiras execuções no GitHub e atualizada aqui.

## Verificar
- Local, as mesmas checagens do job Base: `bash scripts/ci-local.sh`.
- No GitHub: aba **Actions** do repositório, ou o status de um pull request.

## Configurações manuais no GitHub (não dá para versionar)
1. **Settings > Branches:** proteger `main` e `develop`: exigir pull request e a verificação **CI OK**.
2. **Settings > Advanced Security** (ou Code security): **ativar o Dependency graph**, o Dependabot alerts e o code scanning. Ao ativar o grafo, remova o `continue-on-error` do job de revisão de dependências em `ci.yml`.

## Limitações conhecidas
- As ações são fixadas por versão principal (`@v4` etc.), não por hash. O Dependabot atualiza as versões toda semana. Os números de versão das ações não foram conferidos contra o GitHub neste ambiente.
- **Primeira execução real (03/11/2026):** a ação `aquasecurity/trivy-action@0.28.0` não existia e quebrava todos os jobs de serviço; a varredura de imagem foi retirada até o primeiro Dockerfile. A revisão de dependências falhou por falta do grafo de dependências, e o job de scripts por um aviso do shellcheck (corrigido).
- O fluxo Java usa o **Temurin 25** (ADR-0003, spike concluído em 04/11/2026) e ainda não foi exercitado de ponta a ponta na CI; é a execução na CI que comprova o funcionamento sobre um JDK 25.
- O Dependabot cobre só `github-actions` por enquanto. `maven`, `npm` e `docker` entram quando houver `pom.xml`, `package.json` e Dockerfiles.
- O gitleaks ignora `.env.example` (valores fictícios, ver `.gitleaks.toml`).
