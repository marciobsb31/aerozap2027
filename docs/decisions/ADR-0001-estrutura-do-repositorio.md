# ADR-0001 — Estrutura do repositório

Data: 2026-11-03

Status: APROVADA em 2026-11-03 (Product Owner e CTO)

Autor: A0 (com A6)

Aprovador: A0 (CTO) e Product Owner

## Contexto
O manual propõe `backend/`, `frontend/`, `infrastructure/`, `tests/`; o DTC §17 propõe `apps/`, `frontend/`, `libs/`, `infrastructure/`, `docs/`. O diretório atual não é um repositório Git e não tem código.

## Opções consideradas
1. **Estrutura do manual** (`backend/` único): simples, mas mistura serviços de ciclos diferentes e dificulta pipelines por diretório.
2. **Estrutura do DTC** (monorepo com `apps/<serviço>`): serviços independentes, pipelines por diretório, bibliotecas pequenas.
3. **Um repositório por serviço**: autonomia máxima, custo alto de coordenação para uma equipe de agentes em início de projeto.

## Decisão
Opção 2, com a pasta `docs/` do manual (`prd`, `architecture`, `api-contracts`, `decisions`, `runbooks`) e `tests/` para E2E, carga e segurança transversais:

```text
aerozap/
├── AGENTS.md  CLAUDE.md
├── agentes/
├── apps/<deployable>/        # api-gateway, platform-core, search, conversation-ai, messaging
├── frontend/                 # workspace Angular: aerozap-web, aerozap-admin
├── libs/
│   ├── java/                 # event-contracts, observability-starter (sem regra de negócio de domínio)
│   └── typescript/           # design-system, api-client
├── infrastructure/           # docker, compose, monitoring, kubernetes (somente se justificado)
├── tests/                    # e2e, load, security
└── docs/                     # prd, architecture, api-contracts, decisions, runbooks, backlog
```

Convenções técnicas propostas:
- Backend: **Maven multi-módulo** com POM pai para versões. Alternativa aceitável: Gradle multi-projeto.
- Frontend: workspace Angular CLI com projetos e bibliotecas (sem Nx no início); gerenciador de pacotes npm.
- Git: inicializar o repositório em AZ-010; branches `main`, `develop`, `feature/AZ-xxx-...`, `bugfix/...`, `hotfix/...`; PR obrigatório; commits prefixados com o ID da tarefa. **Remoto definido pelo Product Owner em 2026-11-03:** `https://github.com/marciobsb31/aerozap2027` (GitHub).
- Pipelines por diretório (`apps/*`, `frontend/`, `libs/*`).

## Consequências
Positivas: ciclos de build e deploy independentes por serviço, fronteiras visíveis, um só lugar para contratos. Negativas: exige disciplina de dependências e pipeline por caminho. Nenhuma biblioteca compartilhada pode carregar regra de negócio de um domínio para outro.

## Plano de implementação e de reversão
AZ-010 cria a estrutura, `git init` e a primeira ADR de convenções. Reversão: mover diretórios é barato enquanto não houver código.

## Validação
`docker compose up` e a pipeline base funcionam a partir dessa estrutura (AZ-011, AZ-012).


## Nota de implementação (2026-11-04) — bibliotecas TypeScript dentro do workspace Angular

Esta ADR previa `libs/typescript/` para `design-system` e `api-client`. Na AZ-016 as duas ficaram
em **`frontend/projects/`**.

**Motivo.** O Angular CLI e os workspaces do npm resolvem projetos a partir da raiz do workspace.
Uma biblioteca fora dela quebra `ng build`, `ng test` e a resolução de estilos, e obriga a manter
dois `package.json` e dois `node_modules` para o mesmo frontend. O custo é diário; o ganho da
separação por diretório seria apenas de arrumação.

**O que a ADR pedia continua valendo**: são projetos separados, com build próprio (ng-packagr),
teste próprio e superfície pública explícita (`public-api.ts`), e nenhuma regra de negócio de um
domínio atravessa para outro por elas. O filtro da CI passou a disparar o fluxo do frontend também
em `docs/api-contracts/**`, porque o `api-client` é gerado dali.

`libs/typescript/` fica para biblioteca TypeScript que não seja do frontend Angular; seu README
aponta para o novo lugar.

**`aerozap-admin`** (previsto no desenho desta ADR) não foi criado: um projeto vazio faria a CI
construir e testar nada. Entra no marco que precisar dele.

**Reversão**: mover as duas pastas e ajustar `angular.json`, `tsconfig.json` e o filtro da CI —
barato enquanto o frontend é novo, e cada vez menos barato depois.
