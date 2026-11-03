# AeroZap — Plano de Implementação

**Versão:** 1.0 (03/11/2026) · **Autor:** preparado antes da inicialização da equipe · **Aprovação:** Product Owner e A0 (CTO Master)
**Base:** `AeroZap_Documentacao_Tecnica_Completa.pdf` (DTC), `AGENTS.md` e `agentes/`.

Este plano diz **o que construir, em que ordem, por quem, com que critério de saída e o que o Product Owner verá funcionando em cada marco**. O CTO o refina e o transforma em TASKs (`agentes/templates/task.md`). Onde este plano recomenda algo que ainda depende de decisão, está marcado como **proposta**.

Não há datas: prazos dependem de acessos externos (provedores, Meta, gateway) e do ritmo de revisão. O CTO estima após a Fase 0.

---

## 1. Estratégia

1. **Fatia vertical primeiro.** Chegar cedo a uma pesquisa de ponta a ponta (conta → franquia → pesquisa → resposta) e só depois ampliar. Cada marco termina com algo que roda e que o Product Owner consegue verificar.
2. **Núcleo antes de periferia.** Identidade, franquia, pesquisa e provedores são o núcleo (DTC §21). WhatsApp é o canal prioritário, mas depende do núcleo.
3. **Consistência antes de escala.** Idempotência, isolamento, observabilidade e recuperação vêm antes de otimizar (DTC §21).
4. **Começar enxuto.** Poucos deployables no início, divididos por métrica real (ver §8, proposta).
5. **Itens com prazo externo começam na Fase 0**, porque dependem de terceiros e não de código (ver §3).
6. **Nada é "funcional" sem teste real autorizado.** Enquanto não houver credencial de provedor, a pesquisa roda com um adaptador simulado, **rotulado DEMO** em toda a interface.

## 2. Mapa de marcos

| Marco | Fase DTC | O Product Owner verá funcionando |
|---|---|---|
| **M0 Decisões** | 0 | Lista de decisões respondida, PRD v1, relatório de provedores, ADRs 0001–0004 |
| **M1 Fundação** | 1 | Um comando sobe o ambiente completo; CI verde; painel de saúde; esqueleto de serviço e design system |
| **M2 Conta e franquia** | 2 | Cadastro e login na Web; saldo Free de 5 consultas; teste de concorrência provando que a última consulta não é debitada duas vezes |
| **M3 Pesquisa Web** | 3 | Pesquisa na Web com filtros, cartões e histórico; fonte real se houver credencial, senão DEMO rotulado |
| **M4 WhatsApp** | 4 | Pesquisa por texto e por áudio no WhatsApp (número de teste) e continuação na Web |
| **M5 Alertas** | 5 | Alerta de preço criado por conversa ou Web e notificação entregue sem duplicidade |
| **M6 Web e Admin** | 6 | Painel administrativo: usuários, planos, provedores, WhatsApp, IA, auditoria e métricas |
| **M7 Premium** | 7 | Assinatura completa: checkout, recorrência, cancelamento, inadimplência |
| **M8 Produção** | 8 | Testes de carga, revisão de segurança e LGPD, backup restaurado, deploy com rollback, release |

Possível **beta fechado** após o M5 com usuários Free (5 consultas por trimestre), antes do billing. É uma opção de negócio, a decidir pelo Product Owner.

## 3. O que o Product Owner precisa providenciar

Itens com prazo externo. Quanto antes começarem, menos a equipe fica bloqueada.

| Item | Necessário para | Até |
|---|---|---|
| Arquivo da **logo original** | UI final (A2, A8) | Fase 1 |
| Decisões em aberto de `AGENTS.md` §3, com as recomendações de A0/A8 | Fases 1 a 7 | Fim da Fase 0 |
| **Credenciais de teste** de pelo menos um provedor de voos (a confirmar pela avaliação de A4); alguns exigem aprovação de parceiro | Marco M3 | Iniciar na Fase 0 |
| **Conta Meta Business / WhatsApp Business**, número de teste, verificação da empresa e aprovação de templates | Marco M4 | Iniciar na Fase 0 |
| **Chave de API de LLM** e de **transcrição de áudio**, com limite de gasto | A3 (Fase 1 em diante) | Fase 1 |
| **Conta no gateway de pagamento** (sandbox) | Marco M7 | Fase 5 |
| **Revisão jurídica** de LGPD, termos de uso e política de privacidade | Marco M8 (iniciar na Fase 4) | Fase 4 |
| Registro de domínio, ambiente de hospedagem e orçamento de infraestrutura | M8 (homologação antes) | Fase 3 |

## 4. Plano por fase

Legenda: **Dep.** = depende de. Todos os critérios de aceite completos ficam na TASK; aqui está o resumo. Cada tarefa termina com DELIVERY REPORT e validação de A7.

### Fase 0 — Descoberta e decisões (marco M0)

Agentes: A0, A8; convocados A4, A5, A1. Sem código de produção.

| ID | Tarefa | Dono | Dep. |
|---|---|---|---|
| AZ-001 | Diagnóstico do repositório e inventário do que existe | A0 | — |
| AZ-002 | Decisões em aberto: opções, prós, contras e recomendação (`docs/decisions/decisoes-em-aberto.md`) | A8 + A0 | AZ-001 |
| AZ-003 | PRD v1: jornadas, histórias, critérios de aceite, métricas | A8 | AZ-002 |
| AZ-004 | Relatório de avaliação de provedores de voos (credenciais, cobertura, limites, preço, termos, exibição, cache, links) | A4 | — |
| AZ-005 | Avaliação do WhatsApp: Cloud API oficial contra Evolution API, janelas, templates, custo | A5 | — |
| AZ-006 | Avaliação do gateway de pagamento: recorrência, estorno, inadimplência, webhooks | A1 | — |
| AZ-007 | ADR-0001 estrutura do repositório · ADR-0002 deployables iniciais · ADR-0003 versões Java/Angular · ADR-0004 reserva de franquia | A0 + A6 + A1 | AZ-001 |

**Saída:** decisões do Product Owner registradas; PRD v1 aprovado; relatório de provedores entregue; solicitações de acesso externo já enviadas.

### Fase 1 — Fundação (marco M1)

Agentes: A0, A6, A1, A2, A7, A3 (contratos e avaliação).

| ID | Tarefa | Dono | Dep. |
|---|---|---|---|
| AZ-010 | Estrutura do repositório, convenções, branches, templates de PR | A6 + A0 | AZ-007 |
| AZ-011 | Docker Compose local: PostgreSQL, RabbitMQ, Redis, object storage; **um comando sobe tudo** | A6 | AZ-010 |
| AZ-012 | Pipeline CI base: lint, testes, Testcontainers, SAST, dependências, segredos, build de imagem | A6 | AZ-010 |
| AZ-013 | Observabilidade base: métricas, dashboards, logs estruturados, tracing, alertas mínimos | A6 | AZ-011 |
| AZ-014 | Esqueleto padrão de serviço Java: Actuator, Flyway, OpenAPI, Problem Details, correlation ID, Outbox, filas e DLQ | A1 | AZ-010, AZ-011 |
| AZ-015 | API Gateway/BFF base: autenticação de entrada, roteamento, rate limit | A1 | AZ-014 |
| AZ-016 | Design system, layout, i18n pt/es e `api-client` gerado do OpenAPI | A2 | AZ-010 |
| AZ-017 | Contratos v0: envelope de resposta, erros, `SearchIntent`, `FlightOffer`, `FlightProvider`, eventos base | A0 + A1 + A3 + A4 | AZ-003 |
| AZ-018 | Estratégia de testes, matriz de rastreabilidade e harness de concorrência | A7 | AZ-012 |
| AZ-019 | Interpretador de linguagem natural v0 e conjunto dourado pt/es (independente do backend, contra `SearchIntent`) | A3 | AZ-017 |

**Saída:** ambiente reproduzível com health checks; CI verde; esqueleto de serviço publicado; contratos v0 aprovados.

### Fase 2 — Identidade, planos e franquia (marco M2)

Agentes: A1, A2, A7.

| ID | Tarefa | Dono | Dep. |
|---|---|---|---|
| AZ-020 | Identity: cadastro, login, refresh token rotacionado, papéis, recuperação de acesso | A1 | AZ-014 |
| AZ-021 | Vínculo telefone-conta com verificação de posse do número | A1 | AZ-020 |
| AZ-022 | Entitlement: planos, `usage_period`, `usage_event`, **reserva → confirmação/liberação** (ADR), idempotência, reserva órfã expirada | A1 | AZ-020 |
| AZ-023 | Planos e preços **configuráveis** (sem valor fixo no código) | A1 | AZ-022 |
| AZ-024 | Web: cadastro, login, perfil, preferências de idioma, saldo e consumo | A2 | AZ-016, AZ-020 |
| AZ-025 | Testes: concorrência da última consulta, repetição, virada de trimestre, isolamento entre usuários (BOLA), token expirado | A7 | AZ-022 |

**Saída (DTC):** testes de concorrência e isolamento aprovados; nenhum achado Crítico ou Alto.

### Fase 3 — Pesquisa (marco M3)

Agentes: A4, A1, A2, A3, A7.

| ID | Tarefa | Dono | Dep. |
|---|---|---|---|
| AZ-030 | Porta `FlightProvider` e adaptador **simulado, rotulado DEMO**, para desenvolver sem credencial | A4 | AZ-017 |
| AZ-031 | Adaptador do **provedor real nº 1** (conforme AZ-004) e teste real autorizado | A4 | AZ-004, credencial |
| AZ-032 | Flight Search Service: `search_id`, estados, Outbox, workers, integração com reserva/confirmação de franquia | A4 + A1 | AZ-022, AZ-014 |
| AZ-033 | Normalização, consolidação e deduplicação preservando origem e condições | A4 | AZ-030 |
| AZ-034 | Recomendação: mais barato, melhor custo-benefício, mais rápido (algoritmo documentado) | A4 | AZ-033 |
| AZ-035 | Resiliência por provedor: timeout, retry com jitter, circuit breaker, bulkhead, cota, degradação com fontes que falharam | A4 | AZ-031 ou AZ-030 |
| AZ-036 | API de pesquisa (`POST/GET /searches`) e histórico paginado por cursor | A1 + A4 | AZ-032 |
| AZ-037 | Flight Finder: formulário, estados `PROCESSING`, filtros, ordenação, cartões de oferta, comparação, histórico | A2 | AZ-016, AZ-036 |
| AZ-038 | Integração da pesquisa por linguagem natural na Web | A3 + A2 | AZ-019, AZ-036 |
| AZ-039 | Testes: contrato, resiliência, oferta duplicada, preço inválido, provedor lento, regra de consumo | A7 | AZ-035 |

**Saída (DTC):** pesquisa de ponta a ponta com fonte real autorizada. Sem credencial: marco aceito **apenas como "DEMO"**, com a pendência registrada.

### Fase 4 — WhatsApp (marco M4)

Agentes: A5, A3, A1, A8 (copy), A7.

| ID | Tarefa | Dono | Dep. |
|---|---|---|---|
| AZ-040 | Porta `WhatsAppProvider`, adaptador Cloud API, webhook com assinatura, resposta rápida e fila | A5 | AZ-014, AZ-005 |
| AZ-041 | Deduplicação por id externo, fora de ordem, falha de fila, DLQ | A5 | AZ-040 |
| AZ-042 | Conversation Service: contexto com expiração, refinamentos, perguntas de esclarecimento | A3 | AZ-019, AZ-040 |
| AZ-043 | Pipeline de áudio: validação, storage privado com URL assinada, transcrição, confirmação por baixa confiança, retenção | A5 + A3 | AZ-042 |
| AZ-044 | Respostas no WhatsApp (resumo + link para a Web), franquia esgotada com upsell, continuidade da pesquisa na Web, copy pt/es | A5 + A8 | AZ-036, AZ-042 |
| AZ-045 | Proteção contra prompt injection e testes de avaliação | A3 + A7 | AZ-042 |
| AZ-046 | Testes de webhook adulterado, replay, janela e templates, E2E WhatsApp↔Web | A7 | AZ-044 |

**Saída (DTC):** jornada conversacional validada com número de teste.

### Fase 5 — Alertas (marco M5)

| ID | Tarefa | Dono | Dep. |
|---|---|---|---|
| AZ-050 | Modelo e API de alertas (`price_alerts`, `alert_runs`, `price_observations`) | A4 + A1 | AZ-036 |
| AZ-051 | Scheduler distribuído sem execução duplicada, workers com concorrência limitada, frequência adaptativa e agrupamento | A4 | AZ-050 |
| AZ-052 | Notification Service: fila, retentativa, deduplicação, cooldown, opt-in, templates | A5 | AZ-044 |
| AZ-053 | Criar e gerir alertas por conversa | A3 | AZ-050 |
| AZ-054 | Web: gestão de alertas, histórico de preço | A2 | AZ-050 |
| AZ-055 | Testes: idempotência, worker reiniciado, consentimento, cota de provedor | A7 | AZ-051 |

### Fase 6 — Web e Admin (marco M6)

| ID | Tarefa | Dono | Dep. |
|---|---|---|---|
| AZ-060 | Backend Admin/Audit: métricas, usuários, planos, provedores, WhatsApp, IA, auditoria | A1 | AZ-023 |
| AZ-061 | AeroZap Admin: telas do DTC §10.3 com permissões e MFA | A2 | AZ-060 |
| AZ-062 | Calendário de preços, comparação avançada e refinamentos da Web | A2 | AZ-051 |
| AZ-063 | Gestão de provedores e prompts versionados no Admin | A1 + A3 | AZ-060 |
| AZ-064 | Testes de permissões administrativas e E2E | A7 | AZ-061 |
| AZ-065 | Validação de produto e usabilidade; ajustes de UX e acessibilidade | A8 | AZ-062 |

### Fase 7 — Billing (marco M7)

| ID | Tarefa | Dono | Dep. |
|---|---|---|---|
| AZ-070 | Checkout e recorrência no gateway escolhido (sem dados de cartão no AeroZap) | A1 | AZ-006, AZ-023 |
| AZ-071 | Webhooks de pagamento idempotentes, cancelamento no fim do período, inadimplência, estorno, conciliação | A1 | AZ-070 |
| AZ-072 | Web: planos, checkout, assinatura e histórico de pagamentos | A2 | AZ-070 |
| AZ-073 | Testes: falha de pagamento não libera Premium, evento repetido não duplica, ciclo completo | A7 | AZ-071 |

### Fase 8 — Escala e produção (marco M8)

| ID | Tarefa | Dono | Dep. |
|---|---|---|---|
| AZ-080 | Metas de escala definidas e **testes de carga** progressivos (ramp-up, pico, soak, recuperação) com provedores simulados | A7 + A6 | M7 |
| AZ-081 | Revisão de segurança (OWASP, API Top 10) e revisão LGPD com jurídico | A7 + A0 | M7 |
| AZ-082 | Backup, **restauração testada**, DR, runbooks | A6 | M7 |
| AZ-083 | Ambiente de produção, IaC, deploy progressivo e **rollback ensaiado** | A6 | AZ-082 |
| AZ-084 | Release: checklist final, aprovação do CTO e do Product Owner | A0 | AZ-080–083 |

## 5. Caminho crítico e paralelismo

**Caminho crítico:** decisões (Fase 0) → repositório e CI (AZ-010/012) → esqueleto de serviço (AZ-014) → identidade e franquia (AZ-020/022) → pesquisa (AZ-032) → WhatsApp (AZ-040–044) → alertas → billing → produção.

**Em paralelo, sem conflito de arquivos:**
- AZ-016 (design system, A2) e AZ-019 (IA v0, A3) com AZ-014 (esqueleto, A1).
- AZ-030 (provedor simulado) enquanto o acesso ao provedor real ainda não saiu.
- AZ-040 (WhatsApp) pode começar assim que houver esqueleto e a avaliação AZ-005, em paralelo à Fase 3.
- A7 acompanha cada fase, com testes escritos junto das entregas.

**Riscos de espera:** credencial de provedor, verificação e templates da Meta, conta do gateway, logo e revisão jurídica. Mitigação: pedir tudo na Fase 0 e seguir com adaptadores simulados rotulados.

## 6. Portões de qualidade

Toda tarefa: DELIVERY REPORT, CI verde, testes aplicáveis, documentação de contrato, **ambiente rodando e verificável**, parecer de A7 e aprovação de A0. Crítico e Alto abertos bloqueiam o DONE (`AGENTS.md` §8).

Cada marco: demonstração ao Product Owner com os comandos para reproduzir, relatório de testes e lista de riscos abertos.

## 7. Riscos e respostas

| Risco | Efeito | Resposta |
|---|---|---|
| Provedor sem acesso comercial ou sem direito de exibição | Sem pesquisa real | Avaliar vários na Fase 0; adaptador simulado rotulado; escalar ao Product Owner |
| Consumo duplicado de franquia por concorrência | Perda de confiança e de receita | Reserva → confirmação, UNIQUE de idempotência, testes de concorrência desde a Fase 2 |
| Regras do WhatsApp (janela, templates, bloqueio) | Alertas não entregues, número banido | Cloud API oficial como padrão, opt-in, templates aprovados cedo |
| Custo de IA e de provedores com uso Premium | Margem negativa | Orçamento por usuário, cache onde permitido, frequência adaptativa de alertas, métricas de custo por pesquisa |
| Microsserviços demais cedo | Custo operacional e lentidão | Deployables iniciais enxutos (§8), divisão guiada por métricas |
| Decisões comerciais em aberto | Retrabalho | Configurável até decidir; lista viva em `docs/decisions/` |
| LGPD e retenção de áudio e conversa | Risco jurídico | Minimização, retenção definida, revisão jurídica a partir da Fase 4 |
| Prompt injection | Execução indevida | Allowlist de ferramentas, backend valida tudo, suíte de testes dedicada |
| Divergência entre agentes sobre contratos | Integração quebrada | Contratos aprovados pelo CTO antes dos consumidores; testes de contrato em CI |

## 8. Proposta de deployables iniciais (a decidir na ADR-0002)

Dividir cada domínio em módulos com **schemas separados e sem acesso cruzado**, mas começar com poucos processos:

| Deployable inicial | Contém (módulos) | Motivo |
|---|---|---|
| `api-gateway` | BFF, autenticação de entrada, rate limit | Borda escalável |
| `platform-core` | Identity, Subscription & Billing, Usage/Entitlement, Admin/Audit | Domínios acoplados à consistência de franquia e de conta; ciclo de release parecido |
| `search` | Flight Search, Provider Integration, normalização, workers de pesquisa | Escala por pesquisa; isolamento de cotas e falhas de provedores |
| `conversation-ai` | Conversation, orquestração de IA, transcrição | Workers próprios, limites de custo e tokens |
| `messaging` | WhatsApp Integration, Notification | Escala por volume de mensagens |

`price-monitoring` começa como workers dentro de `search` e é separado quando a métrica de carga justificar. Esta é uma proposta: o CTO decide e registra na ADR-0002.

## 9. Ordem das primeiras ações

1. Product Owner aprova este plano e inicia os itens de §3.
2. Enviar a mensagem de `agentes/INICIALIZACAO.md` ao A0.
3. A0 executa AZ-001 e apresenta: diagnóstico, arquitetura proposta, backlog em TASKs e plano por fases.
4. A0 convoca A8 (AZ-002/003), A4 (AZ-004), A5 (AZ-005) e A1 (AZ-006) para a Fase 0.
5. Product Owner responde às decisões; A0 registra as ADRs 0001–0004 como aprovadas e libera a Fase 1.

## 10. Fora do escopo (proposta, a confirmar pelo Product Owner)

Emissão e reserva de passagens (o AeroZap mostra ofertas e leva o usuário ao link de compra), app móvel nativo, canais além de WhatsApp e Web, e conversão automática de tarifa em milhas. Qualquer inclusão passa pelo A0 e pelo Product Owner.
