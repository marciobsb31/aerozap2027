# Backlog AeroZap

Gerado a partir de `docs/architecture/plano-de-implementacao.md` (03/11/2026). **61 tarefas**, 9 fases. As Fases 0 e 1 têm TASK completa em `tasks/`; as demais são detalhadas pelo CTO ao liberar cada fase (o critério de sair de BACKLOG para READY é: contratos e dependências definidos).

Regra: no máximo duas tarefas `IN_PROGRESS` por agente. Atualize o estado aqui ao mover uma tarefa.

| Estado | Significado |
|---|---|
| BACKLOG | Ainda não iniciada |
| READY | Pronta para execução |
| IN_PROGRESS | Em desenvolvimento |
| BLOCKED | Bloqueada por dependência ou decisão |
| REVIEW | Aguardando revisão |
| REWORK | Precisa de ajustes |
| DONE | Aprovada e integrada |

## Fase 0 — Descoberta e decisões (marco M0)

| ID | Tarefa | Dono | Depende de | Estado | TASK |
|---|---|---|---|---|---|
| AZ-001 | Diagnóstico do repositório e inventário do que existe | A0 | — | REVIEW | [AZ-001](tasks/AZ-001.md) |
| AZ-002 | Decisões em aberto: opções, prós, contras e recomendação (`docs/decisions/decisoes-em-aberto.md`) | A8 + A0 | AZ-001 | REVIEW | [AZ-002](tasks/AZ-002.md) |
| AZ-003 | PRD v1: jornadas, histórias, critérios de aceite, métricas | A8 | AZ-002 | REVIEW | [AZ-003](tasks/AZ-003.md) |
| AZ-004 | Relatório de avaliação de provedores de voos (credenciais, cobertura, limites, preço, termos, exibição, cache, links) | A4 | — | REVIEW | [AZ-004](tasks/AZ-004.md) |
| AZ-005 | Avaliação do WhatsApp: Cloud API oficial contra Evolution API, janelas, templates, custo | A5 | — | REVIEW | [AZ-005](tasks/AZ-005.md) |
| AZ-006 | Avaliação do gateway de pagamento: recorrência, estorno, inadimplência, webhooks | A1 | — | REVIEW | [AZ-006](tasks/AZ-006.md) |
| AZ-007 | ADR-0001 estrutura do repositório · ADR-0002 deployables iniciais · ADR-0003 versões Java/Angular · ADR-0004 reserva de franquia | A0 + A6 + A1 | AZ-001 | DONE | [AZ-007](tasks/AZ-007.md) |

## Fase 1 — Fundação (marco M1)

| ID | Tarefa | Dono | Depende de | Estado | TASK |
|---|---|---|---|---|---|
| AZ-010 | Estrutura do repositório, convenções, branches, templates de PR | A6 + A0 | AZ-007 | REVIEW | [AZ-010](tasks/AZ-010.md) |
| AZ-011 | Docker Compose local: PostgreSQL, RabbitMQ, Redis, object storage; **um comando sobe tudo** | A6 | AZ-010 | REVIEW | [AZ-011](tasks/AZ-011.md) |
| AZ-012 | Pipeline CI base: lint, testes, Testcontainers, SAST, dependências, segredos, build de imagem | A6 | AZ-010 | REVIEW | [AZ-012](tasks/AZ-012.md) |
| AZ-013 | Observabilidade base: métricas, dashboards, logs estruturados, tracing, alertas mínimos | A6 | AZ-011 | IN_PROGRESS | [AZ-013](tasks/AZ-013.md) |
| AZ-014 | Esqueleto padrão de serviço Java: Actuator, Flyway, OpenAPI, Problem Details, correlation ID, Outbox, filas e DLQ | A1 | AZ-010, AZ-011 | REVIEW | [AZ-014](tasks/AZ-014.md) |
| AZ-015 | API Gateway/BFF base: autenticação de entrada, roteamento, rate limit | A1 | AZ-014 | REVIEW | [AZ-015](tasks/AZ-015.md) |
| AZ-016 | Design system, layout, i18n pt/es e `api-client` gerado do OpenAPI | A2 | AZ-010 | BACKLOG | [AZ-016](tasks/AZ-016.md) |
| AZ-017 | Contratos v0: envelope de resposta, erros, `SearchIntent`, `FlightOffer`, `FlightProvider`, eventos base | A0 + A1 + A3 + A4 | AZ-003 | REVIEW | [AZ-017](tasks/AZ-017.md) |
| AZ-018 | Estratégia de testes, matriz de rastreabilidade e harness de concorrência | A7 | AZ-012 | BACKLOG | [AZ-018](tasks/AZ-018.md) |
| AZ-019 | Interpretador de linguagem natural v0 e conjunto dourado pt/es (independente do backend, contra `SearchIntent`) | A3 | AZ-017 | REVIEW | [AZ-019](tasks/AZ-019.md) |

## Fase 2 — Identidade, planos e franquia (marco M2)

| ID | Tarefa | Dono | Depende de | Estado | TASK |
|---|---|---|---|---|---|
| AZ-020 | Identity: cadastro, login, refresh token rotacionado, papéis, recuperação de acesso | A1 | AZ-014 | BACKLOG | — |
| AZ-021 | Vínculo telefone-conta com verificação de posse do número | A1 | AZ-020 | BACKLOG | — |
| AZ-022 | Entitlement: planos, `usage_period`, `usage_event`, **reserva → confirmação/liberação** (ADR), idempotência, reserva órfã expirada | A1 | AZ-020 | BACKLOG | — |
| AZ-023 | Planos e preços **configuráveis** (sem valor fixo no código) | A1 | AZ-022 | BACKLOG | — |
| AZ-024 | Web: cadastro, login, perfil, preferências de idioma, saldo e consumo | A2 | AZ-016, AZ-020 | BACKLOG | — |
| AZ-025 | Testes: concorrência da última consulta, repetição, virada de trimestre, isolamento entre usuários (BOLA), token expirado | A7 | AZ-022 | BACKLOG | — |

## Fase 3 — Pesquisa (marco M3)

| ID | Tarefa | Dono | Depende de | Estado | TASK |
|---|---|---|---|---|---|
| AZ-030 | Porta `FlightProvider` e adaptador **simulado, rotulado DEMO**, para desenvolver sem credencial | A4 | AZ-017 | BACKLOG | — |
| AZ-031 | Adaptador do **provedor real nº 1** (conforme AZ-004) e teste real autorizado | A4 | AZ-004, credencial | BACKLOG | — |
| AZ-032 | Flight Search Service: `search_id`, estados, Outbox, workers, integração com reserva/confirmação de franquia | A4 + A1 | AZ-022, AZ-014 | BACKLOG | — |
| AZ-033 | Normalização, consolidação e deduplicação preservando origem e condições | A4 | AZ-030 | BACKLOG | — |
| AZ-034 | Recomendação: mais barato, melhor custo-benefício, mais rápido (algoritmo documentado) | A4 | AZ-033 | BACKLOG | — |
| AZ-035 | Resiliência por provedor: timeout, retry com jitter, circuit breaker, bulkhead, cota, degradação com fontes que falharam | A4 | AZ-031 ou AZ-030 | BACKLOG | — |
| AZ-036 | API de pesquisa (`POST/GET /searches`) e histórico paginado por cursor | A1 + A4 | AZ-032 | BACKLOG | — |
| AZ-037 | Flight Finder: formulário, estados `PROCESSING`, filtros, ordenação, cartões de oferta, comparação, histórico | A2 | AZ-016, AZ-036 | BACKLOG | — |
| AZ-038 | Integração da pesquisa por linguagem natural na Web | A3 + A2 | AZ-019, AZ-036 | BACKLOG | — |
| AZ-039 | Testes: contrato, resiliência, oferta duplicada, preço inválido, provedor lento, regra de consumo | A7 | AZ-035 | BACKLOG | — |

## Fase 4 — WhatsApp (marco M4)

| ID | Tarefa | Dono | Depende de | Estado | TASK |
|---|---|---|---|---|---|
| AZ-040 | Porta `WhatsAppProvider`, adaptador Cloud API, webhook com assinatura, resposta rápida e fila | A5 | AZ-014, AZ-005 | BACKLOG | — |
| AZ-041 | Deduplicação por id externo, fora de ordem, falha de fila, DLQ | A5 | AZ-040 | BACKLOG | — |
| AZ-042 | Conversation Service: contexto com expiração, refinamentos, perguntas de esclarecimento | A3 | AZ-019, AZ-040 | BACKLOG | — |
| AZ-043 | Pipeline de áudio: validação, storage privado com URL assinada, transcrição, confirmação por baixa confiança, retenção | A5 + A3 | AZ-042 | BACKLOG | — |
| AZ-044 | Respostas no WhatsApp (resumo + link para a Web), franquia esgotada com upsell, continuidade da pesquisa na Web, copy pt/es | A5 + A8 | AZ-036, AZ-042 | BACKLOG | — |
| AZ-045 | Proteção contra prompt injection e testes de avaliação | A3 + A7 | AZ-042 | BACKLOG | — |
| AZ-046 | Testes de webhook adulterado, replay, janela e templates, E2E WhatsApp↔Web | A7 | AZ-044 | BACKLOG | — |

## Fase 5 — Alertas (marco M5)

| ID | Tarefa | Dono | Depende de | Estado | TASK |
|---|---|---|---|---|---|
| AZ-050 | Modelo e API de alertas (`price_alerts`, `alert_runs`, `price_observations`) | A4 + A1 | AZ-036 | BACKLOG | — |
| AZ-051 | Scheduler distribuído sem execução duplicada, workers com concorrência limitada, frequência adaptativa e agrupamento | A4 | AZ-050 | BACKLOG | — |
| AZ-052 | Notification Service: fila, retentativa, deduplicação, cooldown, opt-in, templates | A5 | AZ-044 | BACKLOG | — |
| AZ-053 | Criar e gerir alertas por conversa | A3 | AZ-050 | BACKLOG | — |
| AZ-054 | Web: gestão de alertas, histórico de preço | A2 | AZ-050 | BACKLOG | — |
| AZ-055 | Testes: idempotência, worker reiniciado, consentimento, cota de provedor | A7 | AZ-051 | BACKLOG | — |

## Fase 6 — Web e Admin (marco M6)

| ID | Tarefa | Dono | Depende de | Estado | TASK |
|---|---|---|---|---|---|
| AZ-060 | Backend Admin/Audit: métricas, usuários, planos, provedores, WhatsApp, IA, auditoria | A1 | AZ-023 | BACKLOG | — |
| AZ-061 | AeroZap Admin: telas do DTC §10.3 com permissões e MFA | A2 | AZ-060 | BACKLOG | — |
| AZ-062 | Calendário de preços, comparação avançada e refinamentos da Web | A2 | AZ-051 | BACKLOG | — |
| AZ-063 | Gestão de provedores e prompts versionados no Admin | A1 + A3 | AZ-060 | BACKLOG | — |
| AZ-064 | Testes de permissões administrativas e E2E | A7 | AZ-061 | BACKLOG | — |
| AZ-065 | Validação de produto e usabilidade; ajustes de UX e acessibilidade | A8 | AZ-062 | BACKLOG | — |

## Fase 7 — Billing (marco M7)

| ID | Tarefa | Dono | Depende de | Estado | TASK |
|---|---|---|---|---|---|
| AZ-070 | Checkout e recorrência no gateway escolhido (sem dados de cartão no AeroZap) | A1 | AZ-006, AZ-023 | BACKLOG | — |
| AZ-071 | Webhooks de pagamento idempotentes, cancelamento no fim do período, inadimplência, estorno, conciliação | A1 | AZ-070 | BACKLOG | — |
| AZ-072 | Web: planos, checkout, assinatura e histórico de pagamentos | A2 | AZ-070 | BACKLOG | — |
| AZ-073 | Testes: falha de pagamento não libera Premium, evento repetido não duplica, ciclo completo | A7 | AZ-071 | BACKLOG | — |

## Fase 8 — Escala e produção (marco M8)

| ID | Tarefa | Dono | Depende de | Estado | TASK |
|---|---|---|---|---|---|
| AZ-080 | Metas de escala definidas e **testes de carga** progressivos (ramp-up, pico, soak, recuperação) com provedores simulados | A7 + A6 | M7 | BACKLOG | — |
| AZ-081 | Revisão de segurança (OWASP, API Top 10) e revisão LGPD com jurídico | A7 + A0 | M7 | BACKLOG | — |
| AZ-082 | Backup, **restauração testada**, DR, runbooks | A6 | M7 | BACKLOG | — |
| AZ-083 | Ambiente de produção, IaC, deploy progressivo e **rollback ensaiado** | A6 | AZ-082 | BACKLOG | — |
| AZ-084 | Release: checklist final, aprovação do CTO e do Product Owner | A0 | AZ-080–083 | BACKLOG | — |
