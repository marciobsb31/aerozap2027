# AeroZap — Equipe de agentes

Equipe de agentes de IA que opera como uma organização de engenharia: responsabilidades claras, contratos entre módulos, revisão técnica e integração contínua. Liderança: **A0 CTO Master**.

Base: *Manual de Engenharia, Governança e Prompts dos Agentes v1.0* e *AeroZap — Documentação Técnica Completa v1.0* (DTC, na raiz do projeto). Regras comuns: [`../AGENTS.md`](../AGENTS.md).

## 1. Agentes

| ID | Arquivo | Agente | Responsabilidade | Ativação |
|---|---|---|---|---|
| A0 | [00-cto-master.md](00-cto-master.md) | CTO Master | Arquitetura, coordenação, ADRs, aprovação | Onda 1, contínuo |
| A1 | [01-backend-architect.md](01-backend-architect.md) | Backend Architect | Java/Spring, APIs, identidade, planos, franquia, billing | Onda 1, contínuo |
| A2 | [02-frontend-architect.md](02-frontend-architect.md) | Frontend Architect | Angular, Web, Admin, design system | Onda 1, contínuo |
| A3 | [03-ai-conversation-engineer.md](03-ai-conversation-engineer.md) | AI & Conversation Engineer | Linguagem natural, contexto, avaliação de LLM | Onda 1, contínuo |
| A4 | [04-flight-engine-specialist.md](04-flight-engine-specialist.md) | Flight Engine Specialist | Provedores, busca, normalização, alertas | Onda 1 pontual, Onda 2 contínuo |
| A5 | [05-whatsapp-integration-engineer.md](05-whatsapp-integration-engineer.md) | WhatsApp Integration Engineer | WhatsApp, webhooks, áudio, notificações | Onda 2 |
| A6 | [06-devops-sre-engineer.md](06-devops-sre-engineer.md) | DevOps & SRE Engineer | Infra, CI/CD, observabilidade | Onda 1 pontual, Onda 2 contínuo |
| A7 | [07-qa-security-engineer.md](07-qa-security-engineer.md) | QA & Security Engineer | Testes, segurança, portão de qualidade | Onda 1, contínuo |
| A8 | [08-product-ux-engineer.md](08-product-ux-engineer.md) | Product & UX Engineer | PRD, jornadas, métricas, decisões comerciais | Onda 1 pontual, Onda 3 contínuo |

Cada arquivo tem: identidade, missão, leitura obrigatória, propriedade, responsabilidades, regras, limites, entradas/saídas, definição de pronto e prompt principal. O cabeçalho (`name`, `description`) segue o formato de subagentes do Claude Code.

## 2. Propriedade dos serviços (DTC §4.1)

| Serviço / ativo | Dono | Colabora |
|---|---|---|
| API Gateway / BFF | A1 | A0 |
| Identity Service | A1 | A5 (vínculo telefone) |
| Subscription & Billing | A1 | A8 (planos), A2 (checkout) |
| Usage / Entitlement | A1 | A4 (reserva/confirmação), A7 |
| Admin / Audit (backend) | A1 | A2 (UI) |
| Conversation / AI | A3 | A5, A1 |
| Flight Search | A4 | A1 (entitlement), A3 |
| Provider Integration | A4 | A6 (secrets) |
| Price Monitoring | A4 | A1 (API de alertas) |
| WhatsApp Integration | A5 | A3, A6 |
| Notification | A5 | A4, A1 |
| AeroZap Web / Admin | A2 | A1, A8 |
| Infra, CI/CD, observabilidade | A6 | Todos |
| Testes transversais e segurança | A7 | Todos |
| PRD, jornadas, métricas | A8 | A0 |
| Arquitetura, ADRs, contratos | A0 | Todos |

## 3. Contratos compartilhados

Todos exigem **aprovação do CTO** e aviso aos consumidores.

| Contrato | Quem propõe | Consumidores |
|---|---|---|
| REST / OpenAPI (`docs/api-contracts/`) | A1 | A2, A3, A4, A5 |
| Eventos (`libs/java/event-contracts`) | A0 (A1 mantém) | A1, A3, A4, A5 |
| `SearchIntent` | A3 | A1, A4, A5 |
| `FlightOffer`, `FlightProvider` | A4 | A1, A2, A3, A5 |
| `WhatsAppProvider` | A5 | A3, A1 |

## 4. Ondas de ativação

Não ative os nove agentes no primeiro dia: aumenta conflitos, custo de tokens e retrabalho. Ativar por onda, de acordo com as fases do roadmap do DTC §18.

| Onda | Fases do DTC | Agentes | Foco |
|---|---|---|---|
| **1 — Fundação** | 0, 1, 2 | Contínuos: A0, A1, A2, A3, A7. Pontuais: A8 (PRD e decisões em aberto), A6 (estrutura do repositório, Compose e CI), A4 (avaliação de provedores e contratos, sem código de produção) | Decisões, repositório, identidade, planos, franquia, design system, interpretação de linguagem natural, testes |
| **2 — Integrações** | 3, 4, 5 | + A4, A5, A6 contínuos | Pesquisa real, normalização, WhatsApp, alertas, infraestrutura |
| **3 — Produto** | 6, 7, 8 | + A8 contínuo | Web e Admin completos, billing, carga e segurança, produção |

A ordem das 18 etapas do manual permanece como referência de planejamento. Tarefas independentes podem rodar em paralelo depois que seus contratos forem definidos.

| Etapa | Entrega | Responsável principal |
|---|---|---|
| 01 | Arquitetura e ADRs | A0 |
| 02 | Estrutura do repositório | A0 + A6 |
| 03 | Autenticação e usuários | A1 |
| 04 | Design System e layout | A2 |
| 05 | Contratos de pesquisa | A0 + A1 + A4 |
| 06 | Integração com provedores | A4 |
| 07 | Normalização das ofertas | A4 |
| 08 | Flight Finder | A2 |
| 09 | IA conversacional | A3 |
| 10 | Integração WhatsApp | A5 |
| 11 | Planos e franquias | A1 |
| 12 | Pagamentos e assinaturas | A1 |
| 13 | Monitoramento de preços | A4 + A1 |
| 14 | Administração | A1 + A2 |
| 15 | Observabilidade e deploy | A6 |
| 16 | Testes completos | A7 |
| 17 | Validação de produto | A8 |
| 18 | Release | A0 |

## 5. Como usar

1. **Início:** envie ao A0 a mensagem de [`INICIALIZACAO.md`](INICIALIZACAO.md).
2. **Delegação:** o A0 abre TASKs com [`templates/task.md`](templates/task.md). Cada agente entrega com [`templates/delivery-report.md`](templates/delivery-report.md). Decisões estruturais usam [`templates/adr.md`](templates/adr.md).
3. **Como subagentes no Claude Code:** copie ou crie links dos arquivos `agentes/0X-*.md` para `.claude/agents/`. O cabeçalho `name`/`description` já está no formato esperado. Este diretório continua sendo a fonte da verdade.
4. **Sem subagentes:** peça ao Claude para "agir como A1" e informe o arquivo.

## 6. O que foi ajustado em relação ao manual v1.0

Mudanças feitas ao cruzar o manual com o DTC e com boas práticas. Todas podem ser revertidas.

| # | Ajuste | Motivo |
|---|---|---|
| 1 | Pasta chamada `agentes/` (o manual usava `agents/`) | Pedido do dono do projeto |
| 2 | `AGENTS.md` na raiz reúne regras comuns, precedência de documentos e decisões em aberto; `CLAUDE.md` o importa | Evita repetir regras em nove arquivos e faz o Claude Code carregá-las |
| 3 | O DTC passa a ser o baseline técnico de todos os agentes | O manual não cobria serviços, fluxo assíncrono, Outbox, LGPD, milhas nem o app Admin |
| 4 | Lista de decisões em aberto (logo, Java, trimestre, preços, WhatsApp, provedores, milhas, gateway, escala, retenção) | O manual tratava como "regra aprovada" itens que o DTC marca como hipótese |
| 5 | **Reserva de franquia** (reservar → confirmar/liberar) | O fluxo do DTC verifica o saldo no início e debita no fim: duas requisições simultâneas pela última consulta passariam |
| 6 | Nova decisão: o que é "pesquisa concluída com sucesso" (parcial, zero resultados) | Lacuna entre "falha não consome" e "degradação controlada" |
| 7 | Propriedade explícita de cada serviço do DTC (Flight Search, Provider Integration, Price Monitoring, Notification, Conversation/AI, Admin/Audit) | O manual deixava vários sem dono |
| 8 | A6 e A4 convocados pontualmente já na Onda 1; A8 na Descoberta | A etapa 02 do manual já depende de A6; a etapa 05 depende de A4; as decisões em aberto dependem de A8 |
| 9 | WhatsApp: Cloud API oficial como padrão de produção; Evolution API só com ADR e aceitação de risco | Modos não oficiais do Evolution podem violar termos e causar bloqueio do número |
| 10 | `SearchIntent` usa `passengers` como objeto (`adults`, `children`, `infants`) | Manual e DTC divergiam; o objeto é mais completo |
| 11 | Estrutura de pastas reconciliada (DTC §17 + `docs/` do manual), a formalizar em ADR-0001 | Manual e DTC divergiam |
| 12 | Adicionados: LGPD, consentimento de notificações, milhas, Admin separado, Outbox/DLQ, SLOs, custo por provedor e por IA | Presentes no DTC, ausentes no manual |
| 13 | Limite de duas tarefas `IN_PROGRESS` por agente, tabela de severidade, regra de "entrega rodando e verificável" | Reduzir conflitos e tornar a revisão objetiva |
| 14 | A7 tem poder de bloquear o DONE; testes automatizados não usam provedores reais | Qualidade e custo/termos |
