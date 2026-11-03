# AZ-001 — Diagnóstico técnico e arquitetura proposta

**Autor:** A0 CTO Master · **Data:** 03/11/2026 · **Status:** para aprovação do Product Owner
**Relacionados:** `plano-de-implementacao.md`, `docs/decisions/decisoes-em-aberto.md`, ADR-0001 a ADR-0004.

## 1. Diagnóstico técnico

### 1.1 O que existe
| Item | Situação |
|---|---|
| Código (Java, Angular, Docker, CI) | **Nenhum.** Projeto do zero |
| Repositório Git | **Não é um repositório Git.** Sem histórico, sem remoto |
| Documentação | DTC v1.0 (PDF), manual dos agentes, `AGENTS.md`, `CLAUDE.md`, `agentes/`, plano de implementação |
| PRD | Em elaboração (AZ-003, A8) |
| Logo original | **Não fornecida** |
| Credenciais (provedores, Meta, gateway, LLM) | **Nenhuma** |
| Ambientes | Nenhum definido |

### 1.2 O que precisa ser construído
Tudo: plataforma de serviços (identidade, franquia, assinaturas, pesquisa, provedores, IA, WhatsApp, alertas, administração), duas aplicações Angular (Web e Admin), infraestrutura local e de produção, pipelines, observabilidade e testes.

### 1.3 Inconsistências e lacunas encontradas na documentação
| # | Achado | Tratamento |
|---|---|---|
| 1 | Fluxo de franquia do DTC §5.2 verifica o saldo no passo 3 e debita no passo 9: duas requisições simultâneas pela última consulta passam na verificação | ADR-0004 (reserva → confirmação/liberação) |
| 2 | "Pesquisa concluída com sucesso" indefinida para resultado parcial e zero resultados | Decisão do Product Owner; política configurável |
| 3 | Estruturas de pastas diferentes no manual e no DTC | ADR-0001 |
| 4 | `passengers` como número (manual) ou objeto (DTC) | Objeto do DTC; contrato `SearchIntent` em AZ-017 |
| 5 | Manual ativa A6 só na segunda onda, mas a estrutura do repositório e o Compose dependem dele no início | A6 pontual na Onda 1 (já refletido no plano) |
| 6 | Trimestre Free, preços e gateway tratados como definidos no manual, mas são hipóteses no DTC | Lista de decisões em aberto |
| 7 | Evolution API listada como opção equivalente à Cloud API | Avaliação AZ-005 e recomendação por ADR |
| 8 | Diretório inicial não é um repositório Git | Inicializar em AZ-010; hospedagem do remoto é decisão do Product Owner |

## 2. Arquitetura proposta

Microsserviços orientados a domínio, **começando com poucos deployables** e fronteiras de módulo rígidas (ADR-0002). Processamento assíncrono para pesquisa, áudio, alertas e notificações. Estado de negócio no PostgreSQL; efêmero no Redis; mensageria no RabbitMQ com Transactional Outbox.

```text
        WhatsApp            AeroZap Web           AeroZap Admin
           │                     │                     │
           ▼                     └──────────┬──────────┘
   ┌───────────────┐                        ▼
   │   messaging   │◄──────────────►  api-gateway (BFF)
   │ WhatsApp +    │                        │
   │ Notification  │         ┌──────────────┼───────────────┐
   └──────┬────────┘         ▼              ▼               ▼
          │          platform-core      search        conversation-ai
          │          Identity           Flight Search  Contexto, IA,
          │          Subscription &     Provider       transcrição
          │          Billing            Integration
          │          Usage/Entitlement  Normalização
          │          Admin/Audit        Alertas (workers)
          │                │              │                │
          └────────────────┴──────┬───────┴────────────────┘
                                  ▼
                   RabbitMQ · PostgreSQL · Redis · Object storage
```

### 2.1 Princípios aplicados
1. Cada módulo tem seu schema; **nenhum acesso a tabela de outro módulo**.
2. Operação com efeito de negócio é **idempotente** (chave de idempotência + UNIQUE no banco).
3. Eventos publicados via **Transactional Outbox**; consumidores toleram entrega repetida; DLQ com reprocessamento controlado.
4. A requisição HTTP ou o webhook valida, registra e responde; o trabalho pesado vai para workers.
5. A IA interpreta; o backend valida e executa.
6. Integrações externas atrás de **portas e adaptadores** (`FlightProvider`, `WhatsAppProvider`, provedor de LLM, transcrição, gateway de pagamento).
7. Segredos fora do Git; `user_id` sempre vindo do token.

### 2.2 Fluxo de pesquisa (resumo do DTC §5.2 com a reserva da ADR-0004)
1. Gateway autentica e identifica o usuário e o canal.
2. `search` pede **reserva** de consulta a `platform-core` (idempotente por `search_id`).
3. `search` grava `PENDING` e publica `SearchRequested` via Outbox.
4. Worker consulta provedores em paralelo, com limites, e normaliza.
5. `search` persiste e marca `COMPLETED` (ou `FAILED`).
6. Evento `SearchCompleted` ou `SearchFailed` leva `platform-core` a **confirmar** ou **liberar** a reserva, uma única vez.
7. `messaging` entrega a resposta no canal de origem; a Web acompanha por polling ou SSE.

## 3. Mapa de serviços e responsabilidades

| Módulo | Deployable inicial | Dono | Schema / dados | Comunicação |
|---|---|---|---|---|
| API Gateway / BFF | `api-gateway` | A1 | — (Redis para rate limit) | REST síncrono |
| Identity | `platform-core` | A1 | `identity` | REST |
| Subscription & Billing | `platform-core` | A1 | `subscription` | REST; webhooks de pagamento |
| Usage / Entitlement | `platform-core` | A1 | `usage` | REST (reserva/confirma/libera); eventos |
| Admin / Audit | `platform-core` | A1 | `admin_audit` | REST |
| Flight Search | `search` | A4 | `search` | REST + eventos |
| Provider Integration | `search` | A4 | `provider` | Chamadas externas com resiliência |
| Normalização / Aggregator | `search` | A4 | `search` | Interno |
| Price Monitoring (workers) | `search` (separável) | A4 | `monitoring` | Scheduler + fila |
| Conversation / AI | `conversation-ai` | A3 | `conversation` | Eventos + REST |
| WhatsApp Integration | `messaging` | A5 | `messaging` | Webhooks + fila |
| Notification | `messaging` | A5 | `notification` | Fila |
| AeroZap Web / Admin | `frontend/` | A2 | — | REST via gateway |

Contratos compartilhados que precisam estar aprovados antes dos consumidores (AZ-017): envelope e erros, `SearchIntent`, `FlightOffer`, `FlightProvider`, eventos base (`SearchRequested`, `SearchCompleted`, `SearchFailed`, `UsageReserved`, `UsageConfirmed`, `UsageReleased`, `PriceTargetReached`, `MessageReceived`).

## 4. Dependências
- **Internas:** esqueleto de serviço (AZ-014) → identidade e franquia → pesquisa → WhatsApp → alertas → billing → produção. Detalhe no plano §5.
- **Externas, com prazo:** credencial de provedor, Meta Business com templates, conta do gateway, chave de LLM e de transcrição, logo, revisão jurídica.
- **Tecnológicas:** Java LTS e Spring Boot compatíveis com Testcontainers, Resilience4j, Flyway e OpenAPI (spike em AZ-014); Angular LTS vigente.

## 5. Backlog inicial
`docs/backlog/` (índice das 61 tarefas e TASKs das Fases 0 e 1).

## 6. Plano de execução
`plano-de-implementacao.md`: ondas de ativação, fases 0 a 8, marcos M0 a M8.

## 7. Critérios de aceite
**M0 (Decisões):** decisões do Product Owner registradas; PRD v1 aprovado; relatórios de provedores, WhatsApp e gateway entregues; ADRs 0001 a 0004 aprovadas; pedidos de acesso externo enviados.
**M1 (Fundação):** `docker compose up` sobe o ambiente; CI verde com varredura de segurança; esqueleto de serviço com health check, Flyway, OpenAPI, Problem Details e Outbox; contratos v0 aprovados; design system com i18n pt/es.

## 8. Riscos principais
Resumo; tabela completa no plano §7.
1. Acesso comercial a provedores de voos (prazo e direito de exibição).
2. Políticas do WhatsApp para alertas proativos e para chatbots.
3. Custo de IA e de provedores com uso Premium.
4. Consumo duplicado de franquia sob concorrência.
5. Complexidade operacional de microsserviços cedo demais.
6. Conformidade com a LGPD para áudio, transcrição e conversas.

## 9. Decisões que dependem do Product Owner
Ver `docs/decisions/decisoes-em-aberto.md`, cada uma com opções e recomendação.
