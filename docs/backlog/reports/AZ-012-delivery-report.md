# DELIVERY REPORT

Task: AZ-012

Agent: A6 (DevOps/SRE)

Branch / PR: `feature/AZ-012-ci-base` (empilhada sobre `feature/AZ-011-docker-compose-local`; abrir o PR da AZ-011 primeiro)

Status: READY_FOR_REVIEW, **sem execução no GitHub ainda** (a branch não foi enviada)

## Implementado
- `.github/workflows/ci.yml`: job Base (gitleaks no histórico completo, shellcheck, Compose válido, imagens com tag fixa, nenhum `.env` versionado), revisão de dependências em PR, detecção de diretórios alterados, um job por serviço Java, job do frontend e o resultado único **CI OK**.
- `.github/workflows/java-service.yml` e `frontend.yml`: fluxos reutilizáveis com lint e testes (Maven `verify` com Testcontainers; npm), CodeQL, auditoria, build e varredura de imagem (Trivy, falha em crítica). Pulam os passos de build enquanto não há `pom.xml`/`package.json`.
- `.github/dependabot.yml` (GitHub Actions), `.gitleaks.toml`, `scripts/ci-local.sh` e `docs/architecture/ci.md`.

## Arquivos alterados
`.github/workflows/ci.yml`, `.github/workflows/java-service.yml`, `.github/workflows/frontend.yml`, `.github/dependabot.yml`, `.gitleaks.toml`, `scripts/ci-local.sh`, `docs/architecture/ci.md`

## Contratos
Não.

## Testes
- Unitários: N/A
- Integração: **PENDENTE** (os workflows só executam no GitHub)
- Contrato: N/A
- Concorrência/idempotência: N/A
- Segurança: parcial (gitleaks, revisão de dependências, npm audit e Trivy configurados, ainda não exercitados)

## Como verificar
Local: `bash scripts/ci-local.sh`. No GitHub: enviar a branch, abrir o PR e conferir a aba Actions. O ambiente está rodando: **Não** (CI depende do GitHub).

## Evidências
- `actionlint` e `yamllint` sem erros nos três workflows e no Dependabot.
- `shellcheck -S warning` sem avisos nos scripts.
- Lógica dos passos de tag e `.env` testada localmente, inclusive um caso negativo (imagem sem tag é detectada).
- Falta: execução real no GitHub e o tempo medido.

## Dependências
AZ-010 (feita). Para o critério "pipeline roda por diretório" ser exercitado de verdade, é preciso código em `apps/` ou `frontend/` (AZ-014, AZ-016).

## Riscos
- Versões das ações não conferidas contra o GitHub (tags `@v4`, `@v3`, `@v2`, `trivy-action@0.28.0`); o Dependabot as mantém.
- `java-version: '25'` e o fluxo Java inteiro só serão validados com o primeiro serviço.
- Proteção de branch e code scanning são configurações manuais no GitHub (ver `docs/architecture/ci.md`).

## Decisões necessárias
Nenhuma.

## Checklist de pronto
- [ ] Critérios de aceite atendidos (faltam os que dependem de execução no GitHub e de código)
- [ ] Testes passando em CI
- [x] Documentação atualizada
- [x] Sem credenciais, sem dados reais de usuários
- [x] Dados de demonstração rotulados (não há)
- [x] Logs sem dados sensíveis
- [x] Plano de reversão: remover `.github/workflows/`, `.github/dependabot.yml` e `.gitleaks.toml`
