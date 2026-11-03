# AZ-006 — Avaliação do gateway de pagamento para assinaturas

**Autor:** A1 Backend Architect · **Data:** 03/11/2026 · **Estado:** proposta para CTO (A0) e Product Owner (PO). Sem código de produção.
**Escopo:** assinaturas Free/Premium. Mensal R$ 39,90, semestral R$ 179,40 e anual R$ 297,00 são **hipótese comercial** (DTC §3.3, `AGENTS.md` §3), configurável no Admin.
**Aviso:** esta avaliação não é parecer fiscal nem jurídico. Contabilidade e jurídico precisam validar os pontos marcados.

**Método e limites.** Só entram fatos lidos em documentação, páginas de preço ou suporte dos próprios fornecedores, consultados em 03/11/2026. Os IDs `[Sx]` remetem à seção 9. O que não foi confirmado em fonte oficial está marcado **NÃO VERIFICADO**. Páginas oficiais que não carregaram (preços do Mercado Pago, referência de webhooks do Pagar.me, detalhes de webhook da Vindi) ficaram como NÃO VERIFICADO. Nenhum preço foi estimado.

---

## 1. Resumo executivo

| Item | Resultado |
|---|---|
| **Gateway nº 1 (proposta)** | **Stripe**, com Checkout hospedado em modo assinatura. É o que tem mais evidência oficial verificada: assinatura nativa, cancelamento no fim do período, dunning configurável, webhooks assinados e com retentativa, idempotência de API e SDK Java. Fica atrás de uma porta `PaymentGateway`. |
| **Plano B** | **Asaas**: Pix Automático em produção na API, boleto, cartão, NFS-e pela API e ciclos semestral e anual nativos. Tem lacunas de integração (seção 3). |
| **Condição que inverte a ordem** | Se o PO exigir **Pix recorrente ou boleto no lançamento**, o Asaas passa a nº 1. Com conta Stripe sediada no Brasil, o Pix só funciona como pagamento avulso `[S1][S4]`. |
| **Fase 2 (América Latina)** | **Mercado Pago**, atrás da mesma porta. É o único com assinaturas documentadas em AR, BR, CL, CO, MX, PE e UY `[M1]`. Exige conta e KYC por país (NÃO VERIFICADO). |
| **Não recomendados agora** | Pagar.me, Iugu e Vindi: menos evidência verificável (Pix não recorrente no Pagar.me, Iugu com Pix Automático só para PJ, Vindi sem conteúdo técnico lido). Podem voltar à comparação com proposta comercial. |

### Achados críticos

1. **Stripe com conta BR não oferece Pix Automático.** A documentação diz que ele não está disponível para negócios sediados no Brasil `[S1]`, e o suporte lista Pix como "recorrência não suportada" `[S4]`. Em Stripe BR a recorrência é cartão. Boleto aparece como "recorrência suportada" `[S5]`, mas com `Off-session payments ✗`, ou seja, o cliente paga cada boleto manualmente.
2. **Repasse lento no cartão doméstico.** Stripe BR: cartão emitido no Brasil fica disponível após **30 dias corridos**; cartão internacional, 5 dias; Pix e boleto, 2 dias `[S7]`. No Asaas, a FAQ de preços cita D+32 para cartão à vista `[A1]`. Isso afeta o caixa do PO.
3. **Nota fiscal.** O Stripe **não** emite NF dos serviços vendidos pelo AeroZap `[S15]`. O Asaas emite NFS-e pela API (R$ 0,49 por nota) e exige configuração fiscal conforme o município `[A14][A1]`.
4. **Asaas e PCI.** Enviar cartão pela API faz os dados passarem pelo backend do AeroZap e mantém a infraestrutura no escopo PCI `[A7]`. Para respeitar `AGENTS.md` §2.9, só dá para usar Checkout, Fatura ou Link hospedados. O exemplo de Checkout recorrente do Asaas mostra apenas cartão e ciclo mensal `[A12]`. Semestral e anual no hospedado: NÃO VERIFICADO.
5. **Segurança do webhook do Asaas.** A autenticação é um token fixo no header `asaas-access-token`, sem HMAC documentado `[A10]`. A fila é interrompida após 15 falhas consecutivas e os eventos pausados são apagados após 14 dias `[A11]`. Exige monitoramento e reconciliação obrigatórios.
6. **Pix Automático já é realidade no mercado.** Asaas (changelog de 26/01/2026) `[A15]`, Iugu para conta PJ `[I2]` e Paddle (03/09/2026) `[D1]`. No Stripe, só para negócios fora do Brasil `[S1]`.

---

## 2. Requisitos do AeroZap usados como critério

- Premium só com assinatura vigente **e** adimplente. Falha de pagamento nunca libera Premium. Evento repetido não duplica cobrança nem benefício (DTC §15.1, A1).
- Nenhum dado de cartão armazenado (`AGENTS.md` §2.9). Webhook com assinatura validada e idempotente (A1).
- Cancelamento mantém acesso até o fim do período pago; tolerância e suspensão configuráveis (DTC §3.2).
- Preço, duração, desconto e parcelamento configuráveis sem novo deploy (DTC §3.3).
- Endpoint de entrada previsto: `POST /api/v1/webhooks/payments/{provider}` e `POST /api/v1/subscriptions/checkout` (DTC §9.2).
- Backend Java; versão (21 ou 25) ainda por ADR.

---

## 3. Matriz comparativa

Datas e versões estão nas fontes da seção 9. "NV" = NÃO VERIFICADO.

### 3.1 Recorrência e meios de pagamento (critérios 1 e 2)

| Gateway | Recorrência e ciclos | Troca de plano e cancelamento | Cartão | Pix | Pix recorrente | Boleto |
|---|---|---|---|---|---|---|
| **Stripe** | Billing/Subscriptions e Checkout em modo assinatura `[S1]`. Ciclos mensal/semestral/anual: NV nesta leitura (esperado, não confirmado). | `cancel_at_period_end` documentado; reativação até o fim do período `[S11]`. Troca de plano e proration: existem, mas não li o conteúdo. | Sim (Visa, Mastercard, Amex, Elo, Hipercard) `[S4]` | Avulso, BRL, T+2 `[S1]` | **Não para conta BR** `[S1][S4]` | Sim; recorrência só manual (sem off-session) `[S5]` |
| **Mercado Pago** | Assinaturas com ou sem plano; frequência semanal/mensal/anual na landing `[M1]`; `frequency`, `repetitions`, `billing_day`, `free_trial` `[M2]`. Semestral = 6 meses: NV. | Gestão (pausar/cancelar/valor): páginas listadas, não lidas `[M2]`. Fim do período: NV. | Cartão por `card_token_id` `[M2]`; também saldo em conta, Abitab e Red Pagos `[M1]` | Pix existe como meio avulso `[M6]` | NV em doc técnica (só posts de blog nos resultados, não lidos) | NV para assinatura |
| **Asaas** | `cycle`: WEEKLY, BIWEEKLY, MONTHLY, BIMONTHLY, QUARTERLY, **SEMIANNUALLY**, **YEARLY** `[A4]` | `PUT` altera valor, ciclo, vencimento e forma; `DELETE` encerra na hora; pausa com `INACTIVE` `[A5]`. **Fim do período nativo: não documentado** (implementar no AeroZap). | Sim; cobra no vencimento `[A2]` | Cobrança Pix por fatura (cliente paga) `[A2]` | **Sim, API pública desde 26/01/2026** `[A15]`; autorização ativa pelo QR Code `[A16]` | Sim `[A2]` |
| **Pagar.me (v5)** | `interval` ano/mês/semana, `interval_count`; sem número de ciclos = infinita `[P1]` | Troca de plano e cancelamento: não cobertos na página lida (NV) | Crédito; débito Pinless `[P1]` | Avulso: NV | **Não para recorrência** (checkout de assinatura) `[P3]` | Contraditório na doc `[P3]` (NV) |
| **Iugu** | Mensal, trimestral e anual na página inicial `[I1]`; intervalos 1/3/6/12 meses no fluxo de Pix Automático `[I2]` | Troca de plano com proration citada `[I1]`; suspender e alterar plano: páginas não lidas | Sim, com cofre PCI `[I1]` | Sim `[I1]` | **Sim, só conta PJ verificada**; jornadas 3 e 4 `[I2]` | Sim `[I1]` |
| **Vindi** | Assinaturas, planos e períodos na API `[V1]` | Criar, reativar, atualizar, cancelar e renovar existem como endpoints `[V1]` | NV | NV | NV | NV |

### 3.2 Tokenização, PCI e webhooks (critérios 3 e 4)

| Gateway | Tokenização e escopo PCI | Webhook: autenticidade | Reentrega | Idempotência e ordem | Eventos de falha, estorno e disputa |
|---|---|---|---|---|---|
| **Stripe** | Checkout hospedado e Elements existem `[S1]`. Enquadramento SAQ: NV (doc de PCI não lida). | `Stripe-Signature` com HMAC-SHA256 sobre o corpo cru; tolerância padrão de 5 min nas bibliotecas; segredos rotacionáveis; lista de IPs `[S8]` | 3 dias com backoff exponencial em produção; 3 tentativas em poucas horas no sandbox; reenvio manual por 15 dias (painel) ou 30 dias (CLI) `[S8]` | Guardar `event.id`; **sem garantia de ordem**; não usar `created` `[S8]`. `Idempotency-Key` em POST, chave de até 255 caracteres, podada após 24 h `[S12]` | `invoice.payment_failed`, `invoice.paid`, `customer.subscription.updated/deleted`, `charge.refunded`, `charge.dispute.created` `[S9]` |
| **Mercado Pago** | `card_token_id`; Secure Fields "facilitam o SAQ-A" `[M5]` | Header `x-signature` (`ts`, `v1`) com chave secreta; manifest e algoritmo: NV na página lida `[M4]` | Espera 22 s; sem 200/201, reenvia a cada 15 min, depois com intervalo maior; máximo: NV `[M4]` | Duplicatas: sem orientação oficial. Tópicos `subscription_preapproval` e `subscription_authorized_payment`; busca os detalhes por API `[M4]`. **Assinatura exige configurar a notificação na criação**, não pelo painel `[M4]` | Rejeições por parcela (`authorized_payments`); e-mail só no cancelamento da assinatura `[M3]`; estorno e disputa: NV |
| **Asaas** | `creditCardToken` aceito, mas **em produção a tokenização é habilitada pelo Asaas após análise** `[A6]`. Cartão pela API mantém o backend em escopo PCI; Checkout, Fatura e Link hospedados podem reduzir `[A7]` | Token estático no header `asaas-access-token` (32–255 caracteres); sem HMAC documentado `[A10]` | Backoff de 30 s a 3 h; fila pausa após 15 falhas; eventos guardados por 14 dias; `removeBackoff` `[A11]` | Doc manda implementar idempotência, sem campo de deduplicação `[A10][A11]`. `sendType` SEQUENTIALLY ou NON_SEQUENTIALLY `[A10]`. Criar assinatura não tem chave de idempotência; após timeout, consultar antes de repetir `[A4]` | `PAYMENT_OVERDUE`, `PAYMENT_CREDIT_CARD_CAPTURE_REFUSED`, `PAYMENT_REFUNDED`, `PAYMENT_CHARGEBACK_REQUESTED/DISPUTE`, `SUBSCRIPTION_INACTIVATED/DELETED` `[A8][A9]` |
| **Pagar.me** | Checkout hospedado por link de assinatura `[P3]`. PCI: NV | **Não informado** na página lida `[P2]` | Número de reenvios configurável; intervalo NV; listar e reenviar falhas por API `[P2]` | Idempotência e prazo de resposta: NV | `invoice.payment_failed`, `charge.payment_failed`, `charge.refunded`, `subscription.canceled` `[P2]` |
| **Iugu** | Cofre criptografado PCI; `iugu.js` para quem não é PCI `[I1]` | Não descrita nas páginas lidas `[I3]` | Reenvio manual por API; retentativa automática: NV `[I4]` | NV. Corpo `application/x-www-form-urlencoded`; IP de saída fixo `[I3]` | `invoice.status_changed`, `subscription.created` `[I2]`; no Pix Automático, `authorization_changed`, `payment_changed`, `cancellation_changed` `[I2]` |
| **Vindi** | NV | Seções de segurança, retentativas, payload e IPs existem, conteúdo não lido `[V1]` | NV | NV | NV |

### 3.3 Dunning, taxas, repasse e conta (critérios 5, 6 e 8)

| Gateway | Dunning e inadimplência | Estorno e disputa | Taxas (fonte oficial; data) | Repasse | Conta e KYC |
|---|---|---|---|---|---|
| **Stripe** | Smart Retries (padrão recomendado: 8 tentativas em 2 semanas) ou até 3 retentativas customizadas; ao esgotar: cancelar, `unpaid`, `past_due` ou pausar `[S10]`. Cancela após até 8 tentativas `[S11]`. Sem retry em recusa definitiva `[S10]` | Estorno de Pix em até 90 dias; disputa de Pix (MED) não é contestável `[S1]`. Boleto sem estorno nem chargeback `[S5]`. Disputa: R$ 55,00 por disputa `[S6]` | Cartão doméstico 3,99% + R$ 0,39; internacional +2%; Pix 1,19%; Boleto R$ 3,45; Billing 0,7% do volume `[S6]`. **A página não traz data de vigência** (só © 2026): confirmar | Cartão BR 30 dias; internacional 5 dias; Pix/boleto 2 dias; repasse diário `[S7]` | PF ou PJ (CNPJ opcional para PF); CPF/CNPJ, nome igual ao documento, endereço no Brasil, representante legal com CPF e residente no Brasil, conta bancária em BRL `[S14]` |
| **Mercado Pago** | Até 4 reagendamentos em janela de 10 dias por parcela; cancela a assinatura após 3 parcelas rejeitadas `[M3]` | NV | Pix: 0% (0,49% para novo vendedor CNPJ com faturamento a partir de R$ 15.000/mês), página sem data `[M6]`. **Cartão e assinatura: NV** | Pix "cai na hora" `[M6]`; cartão: NV | NV |
| **Asaas** | Falha de cartão no vencimento: retentativa **não documentada** `[A5]`. `PAYMENT_OVERDUE` marca atraso `[A8]` | Eventos de estorno total/parcial e chargeback `[A8]`. Taxas de estorno e chargeback: não informadas `[A1]` | Cartão à vista R$ 0,49 + 2,99% (coluna promocional: 1,99%); 2–6x 3,49%; 7–12x 3,99%; assinatura de cartão segue os mesmos percentuais; Pix e boleto R$ 2,15 (promoção R$ 1,08 por 3 meses); NFS-e R$ 0,49. Consultado em 03/11/2026 `[A1]`. Pix Automático: sem taxa informada | Cartão: D+32 (FAQ); antecipação em até 2 dias úteis, sujeita a crédito `[A1]`. Pix/boleto: NV | Produção: NV. Sandbox sem documentos `[A13]` |
| **Pagar.me** | NV | `charge.refunded` `[P2]`; disputa: NV | Página promocional cita Pix 0,99% e crédito à vista 4,19%, sem data e sem taxa de assinatura `[P4]`: **tratar como NV para o AeroZap** | "Dia seguinte*", asterisco sem nota `[P4]` | NV |
| **Iugu** | Pix Automático sem mecanismo de retentativa descrito `[I2]`; cartão: NV | NV | **NV** (sem página oficial de preços lida) | NV | Pix Automático exige conta PJ verificada `[I2]` |
| **Vindi** | Cita "régua de cobrança" `[V1]`; detalhes NV | NV | **NV** | NV | NV |

### 3.4 Operação, fiscal, moedas e engenharia (critérios 7, 9, 10, 11 e 12)

| Gateway | Sandbox | Nota fiscal e impostos | Outras moedas e países | SDK Java e documentação | Risco de dependência |
|---|---|---|---|---|---|
| **Stripe** | Sim; eventos de teste, CLI, test clocks `[S8][S11]` | **Não emite** NFS-e do vendedor `[S15]`. IOF de 3,5% só para negócio **fora** do Brasil cobrando brasileiro `[S1]` | Pix e boleto só BRL `[S1][S5]`. Moeda de apresentação por cartão de uma conta BR: NV. Cartão internacional com +2% `[S6]` | SDK oficial `stripe-java`, JDK 8/11/17/21/25, ativo `[S13]`. Docs extensas e versionadas | Médio. Objetos (Customer, Subscription, Price) não portáveis. Token de cartão é do gateway; migração: NV |
| **Mercado Pago** | Usuários e cartões de teste `[M7]` | NV | **AR, BR, CL, CO, MX, PE, UY** `[M1]` | SDK Java: NV. Docs com páginas que falharam ao carregar | Médio/alto: conta por país; ecossistema próprio |
| **Asaas** | Sim, `api-sandbox.asaas.com`; aprovação não vale para produção `[A13]` | **NFS-e via API**, emissão automática por assinatura, depende do município `[A14]`. Certificado digital e inscrição municipal: não citados | Moedas e países: NV (tudo indica BRL) | SDK Java oficial: NV. Docs em português, boas guias de Pix Automático | Médio. Sem HMAC e sem `Idempotency-Key`: mais código defensivo |
| **Pagar.me** | NV | NV | NV | SDK Java: NV | NV |
| **Iugu** | Página cita "cartões de teste"; sandbox: NV `[I1]` | NV | NV | NV | NV |
| **Vindi** | Página "Ambientes para Testes" existe, conteúdo NV `[V1]` | NV | NV | NV | NV |

**Outro candidato (Paddle, merchant of record).** O changelog de 03/09/2026 informa Pix Automático para clientes no Brasil em BRL, com ciclos semanal a anual, limite de R$ 14.000 por transação e mandato de até 1,5× o preço de renovação `[D1]`. Não confirmei se Paddle atende vendedor sediado no Brasil, quem recolhe tributos nem emissão de NFS-e: **NÃO VERIFICADO**. Fica como pista para o PO validar com o fornecedor. Não entra na recomendação.

**Regras oficiais do Pix Automático (Banco Central, guia v1.2) `[B1]`.** O recebedor só cobra após o pagador confirmar a recorrência. A instrução de cobrança sai de 2 a 10 dias corridos antes da liquidação. Nova tentativa pós-vencimento: até 7 dias corridos, no máximo 3 datas e sempre o mesmo valor. O pagador pode cancelar a autorização. Efeito para o AeroZap: Pix Automático terá tolerância e prazos próprios, diferentes do cartão.

---

## 4. Recomendação

**Nº 1: Stripe (Checkout hospedado, modo assinatura, cartão).** Razões, todas verificadas acima:
1. Cancelamento no fim do período, dunning configurável, estados de assinatura claros (`active`, `past_due`, `unpaid`, `canceled`) e eventos para falha, estorno e disputa `[S9][S10][S11]`.
2. Webhook com HMAC e tolerância de tempo, reentrega por 3 dias e chaves de idempotência de API `[S8][S12]`. Cumpre sem concessões a regra "assinatura validada e idempotente".
3. Página de pagamento hospedada, sem cartão no AeroZap (enquadramento SAQ a confirmar).
4. SDK Java oficial com JDK 21 e 25 `[S13]`. Isso não bloqueia a ADR da versão do Java.

**Custos da escolha que o PO precisa aceitar:** sem Pix recorrente nem boleto recorrente na conta BR (seção 1, item 1); repasse de 30 dias no cartão doméstico; sem NFS-e; taxas em 3.3, sem data de vigência na página.

**Plano B: Asaas.** Entra se o PO exigir Pix Automático/boleto no lançamento, se NFS-e integrada pesar mais que a robustez do webhook, ou se a abertura de conta Stripe travar. Antes de adotar, validar com o Asaas por escrito: (a) Checkout hospedado com ciclos semestral/anual e Pix; (b) política de retentativa de cartão recusado; (c) assinatura criptográfica ou IPs fixos do webhook; (d) habilitação de tokenização em produção; (e) prazo e documentos de KYC.

**Fase 2 (público hispanofalante):** Mercado Pago como terceiro adaptador, após o PO decidir países, moedas e entidade legal.

**Decisão estrutural:** registrar ADR "porta `PaymentGateway` + adaptador Stripe". O domínio do AeroZap (plano, assinatura, ledger, direito) fica no Subscription & Billing Service e não depende de tipos do gateway.

---

## 5. Desenho resumido da integração (proposta)

### 5.1 Onde mora
`platform-core` (módulo Subscription & Billing, ADR-0002 proposta), schema próprio. Entitlement consome eventos de domínio e **nunca** consulta o gateway. Contratos novos (eventos) passam pelo CTO (`AGENTS.md` §6.2).

### 5.2 Checkout hospedado x embutido
| Opção | Prós | Contras | Uso |
|---|---|---|---|
| **Hospedado (recomendado)** | Menor escopo PCI, menos código, 3DS e métodos tratados pelo gateway, UI localizada | Menos controle visual; redirecionamento | MVP |
| Embutido (Elements) | UI integrada ao design system | Mais código, mais responsabilidade de segurança e acessibilidade; mais exposição a PCI | Só por ADR, se a conversão exigir |

Fluxo: `POST /api/v1/subscriptions/checkout` (`user_id` do token; `plan_id`; `Idempotency-Key` do cliente) → backend grava `checkout_intent` e cria a sessão no gateway, com a chave de idempotência derivada do `checkout_intent_id` → devolve a URL → o usuário paga. **A URL de sucesso não concede Premium**: só mostra "processando". Quem concede é o webhook (e uma reconsulta ao gateway). A assinatura nasce vinculada ao usuário por `client_reference_id` e metadados gravados no servidor, nunca por dado vindo do front.

### 5.3 Recebimento de eventos
1. `POST /api/v1/webhooks/payments/{provider}`: ler o **corpo cru**, validar assinatura e tolerância de tempo, responder 2xx rápido e enfileirar. Sem trabalho pesado na requisição (DTC §7.2).
2. Gravar em `payment_event_inbox` com `UNIQUE(provider, event_id)`. Repetido: ignora e responde 2xx.
3. Consumidor idempotente (RabbitMQ, DLQ, `correlation_id`). Antes de decidir, **reconsultar** o objeto no gateway, porque a ordem dos eventos não é garantida `[S8]`.
4. Outbox publica `SubscriptionStateChanged` para Entitlement.

### 5.4 Eventos a tratar (Stripe) e efeito
| Evento | Efeito no AeroZap |
|---|---|
| `checkout.session.completed` | Vincula `customer` e `subscription` do gateway ao usuário. Não concede Premium sozinho |
| `invoice.paid` | Concede ou renova **somente se** a assinatura estiver `active` `[S9]`; grava pagamento com `UNIQUE(provider_invoice_id)` e estende `current_period_end` |
| `invoice.payment_failed` | `PAST_DUE`; avisa o usuário; Premium segue a regra de tolerância (5.5). Nunca estende o período |
| `invoice.payment_action_required` | Avisa que há autenticação pendente |
| `customer.subscription.updated` | Sincroniza `cancel_at_period_end`, plano e status |
| `customer.subscription.deleted` | Encerra o direito (no fim do período, se foi cancelamento agendado) |
| `charge.refunded` | Aplica a política de estorno (pergunta 4) e audita |
| `charge.dispute.created` | Marca risco, suspende conforme política, alerta o Admin |

Este é um conjunto inicial; a lista final sai da TASK de implementação (AZ-070/071). Para o plano B, os equivalentes do Asaas estão em `[A8][A9]`.

### 5.5 Regras de direito (Premium)
- `premium_ativo = status == ACTIVE e agora <= current_period_end` (mais tolerância, se existir). Decisão sempre no backend, a partir do estado local gravado por webhook.
- Falha de pagamento: `PAST_DUE`. **Premissa registrada:** tolerância configurável, com padrão conservador de **0 dias** até o PO decidir (pergunta 5).
- Cancelamento: `cancel_at_period_end`; acesso até o fim do período pago (DTC §3.2).
- Evento repetido ou fora de ordem não altera o resultado: concessão com `UNIQUE(subscription_id, period_start)`.
- A IA nunca toca em plano nem pagamento (`AGENTS.md` §2.12).

### 5.6 Dados
Guardar só identificadores do gateway (`customer`, `subscription`, `invoice`, `charge`) e estado. Nada de PAN, CVV ou validade. Payloads brutos de webhook ficam fora dos logs; no inbox, só o necessário e com retenção definida (a decidir com o jurídico, LGPD). Chaves e segredos de webhook em secret manager (`AGENTS.md` §2.11).

### 5.7 Conciliação
- Job diário compara faturas e cobranças do gateway com o ledger local por período e alerta divergências (pago sem direito, direito sem pagamento, estorno não refletido).
- Reprocessamento de eventos perdidos: reenvio manual existe (15 dias no painel, 30 no CLI) `[S8]`; listagem de eventos por API para o job: **NÃO VERIFICADO**.
- Relatório por período no Admin (DTC §10.3) com status, estornos e inadimplência.

### 5.8 Testes (para a TASK de implementação)
Webhook adulterado ou com timestamp velho; mesmo evento 50 vezes em paralelo; `invoice.paid` antes de `customer.subscription.created`; pagamento recusado nunca libera Premium; cancelamento no fim do período; estorno e disputa; queda do endpoint e recuperação pela reentrega; teste de concorrência em checkout duplo (duplo clique).

---

## 6. O que o Product Owner precisa providenciar

| # | Item | Observação |
|---|---|---|
| 1 | Decidir gateway nº 1 e plano B, e se **Pix recorrente e boleto** são obrigatórios no lançamento | Define Stripe x Asaas |
| 2 | Entidade que vende: **CNPJ** (ou CPF) e representante legal **com CPF e residente no Brasil**; conta bancária em BRL no mesmo CPF/CNPJ | Exigências do Stripe `[S14]`. Iugu Pix Automático exige PJ `[I2]`. KYC dos demais: NV |
| 3 | **Abrir a conta sandbox agora** e iniciar a análise da conta de produção (prazos de aprovação não verificados) | Marco M7, Fase 7; início sugerido na Fase 0/5 |
| 4 | Domínio HTTPS público de homologação para o endpoint de webhook | Com A6 |
| 5 | **Contador:** quem emite NFS-e, município, inscrição municipal, item de serviço e regime tributário; se o gateway emitirá ou um emissor terceiro | Não é parecer fiscal; Stripe não emite `[S15]` |
| 6 | **Jurídico:** termos de uso, política de cancelamento e reembolso, direito de arrependimento, papel do gateway na LGPD, contrato do gateway, retenção dos dados de cobrança | Fase 4 em diante |
| 7 | Confirmar **taxas contratadas por escrito** (páginas sem data de vigência; Pagar.me, Iugu, Vindi e Mercado Pago sem taxa verificada) | |
| 8 | Aceitar o impacto de **caixa**: 30 dias no cartão doméstico (Stripe) ou D+32 (Asaas) | Seção 1, item 2 |
| 9 | Decidir países e moedas do público hispanofalante (BRL apenas no início, ou Mercado Pago na fase 2) | Pergunta 9 |
| 10 | Responder às perguntas em aberto da seção 7 | |

---

## 7. Perguntas em aberto (decisão do PO; implementar configurável até lá)

1. **Parcelamento do semestral e do anual.** O DTC §3.3 interpreta o anual como R$ 297,00 no ano, "em 12 × R$ 24,75". São três modelos diferentes: (a) cobrança única por ciclo, (b) parcelamento no cartão pelo emissor, (c) recorrência mensal com compromisso anual. Não encontrei parcelamento de cartão no Brasil na página de parcelamento da Stripe, que lista Mastercard Installments, México e Japão `[S16]`: **NÃO VERIFICADO**. O checkout de assinatura do Pagar.me não suporta parcelas `[P3]`. Parcelas do Asaas existem para cobrança avulsa de cartão `[A1]`; para assinatura, NV.
2. **Renovação.** Automática por padrão? Aviso prévio antes de renovar (a Stripe tem `invoice.upcoming`) `[S9]`. Pix Automático exige autorização explícita do pagador e notificação do banco 2 a 10 dias antes `[B1]`.
3. **Troca de plano.** Upgrade imediato com cobrança proporcional ou só no próximo ciclo? Downgrade no fim do período? Mensal para anual no meio do ciclo?
4. **Estorno.** Prazo e política (integral, proporcional ao uso)? Premium é revogado na hora ou no fim do período? O que fazer com o Free no trimestre corrente? Boleto não tem estorno no Stripe `[S5]`.
5. **Período de tolerância.** Quantos dias de Premium após falha? Quantas retentativas e em que janela (padrão Stripe: 8 em 2 semanas; Mercado Pago: 4 em 10 dias) `[S10][M3]`? O que acontece ao fim: cancelar, suspender ou manter `past_due`?
6. **Disputa e chargeback.** Suspender acesso durante a disputa? Cancelar a assinatura (a Stripe permite configurar) `[S11]`?
7. **Mudança de preço.** Assinantes existentes mantêm o preço ou migram? O Admin precisa espelhar o preço no gateway (novo objeto de preço por alteração: comportamento exato NV).
8. **Cupons, desconto e período de teste.** Haverá? O DTC fala em desconto configurável.
9. **Moedas e países.** Cobrar em BRL de público hispanofalante (cartão internacional, +2% no Stripe `[S6]`) ou preço local por país?
10. **Free → Premium no meio do trimestre.** O saldo Free restante continua valendo ao cancelar? (afeta A1/A8, regra de franquia)
11. **Retenção** dos registros de cobrança e prazo de guarda fiscal (com contador e jurídico).

---

## 8. Riscos e mitigação

| Risco | Mitigação |
|---|---|
| Dependência do gateway (objetos e tokens não portáveis; migração de cartões NV) | Porta `PaymentGateway`; domínio próprio (plano, assinatura, ledger); sem tipos do gateway fora do adaptador; ADR registrando a decisão |
| Pix recorrente indisponível no nº 1 | Mostrar o limite ao PO agora; plano B pronto; Pix avulso para planos pré-pagos é opção a validar (NV se vale para renovação) |
| Webhook perdido ou fora de ordem | Inbox idempotente, reconsulta ao gateway, job de conciliação, alerta de entregas falhando |
| Caixa (repasse de 30 dias no cartão doméstico) | PO ciente; ver antecipação oferecida pelo gateway (Asaas cita antecipação `[A1]`; Stripe: NV) |
| Taxas sem vigência datada | Confirmação contratual (item 7 da seção 6) |
| Mudanças de API (Stripe tem versões nomeadas, `2026-04-22.dahlia` no changelog consultado `[S3]`) | Fixar a versão da API; testes de contrato; atualizar por TASK |

---

## 9. Fontes (consultadas em 03/11/2026)

**Stripe**
- `[S1]` https://docs.stripe.com/payments/pix
- `[S2]` https://docs.stripe.com/payments/pix/pix-automatico.md
- `[S3]` https://docs.stripe.com/changelog/dahlia/2026-04-22/pix-recurring-payments-support
- `[S4]` https://support.stripe.com/questions/accepted-payment-methods-in-brazil
- `[S5]` https://docs.stripe.com/payments/boleto
- `[S6]` https://stripe.com/en-br/pricing (sem data de vigência na página)
- `[S7]` https://support.stripe.com/questions/brazil-specific-payout-schedule-and-payment-availability
- `[S8]` https://docs.stripe.com/webhooks
- `[S9]` https://docs.stripe.com/billing/subscriptions/webhooks
- `[S10]` https://docs.stripe.com/billing/revenue-recovery/smart-retries
- `[S11]` https://docs.stripe.com/billing/subscriptions/cancel
- `[S12]` https://docs.stripe.com/api/idempotent_requests
- `[S13]` https://github.com/stripe/stripe-java
- `[S14]` https://support.stripe.com/questions/brazil-specific-information-to-open-a-stripe-account
- `[S15]` https://support.stripe.com/questions/nota-fiscal-eletr%C3%B4nica-de-servi%C3%A7os-invoices-%28nf-e%29
- `[S16]` https://docs.stripe.com/payments/installments?locale=pt-BR

**Mercado Pago**
- `[M1]` https://www.mercadopago.com.uy/developers/en/docs/subscriptions/landing
- `[M2]` https://mercadopago.com.br/developers/en/docs/subscriptions/integration-configuration/subscription-associated-plan
- `[M3]` https://www.mercadopago.com.br/developers/en/docs/subscriptions/integration-configuration/subscription-no-associated-plan/authorized-payments.md
- `[M4]` https://www.mercadopago.com.br/developers/pt/docs/subscriptions/additional-content/your-integrations/notifications/webhooks
- `[M5]` https://www.mercadopago.com.br/developers/en/news/2022/09/30/Secure-Fields-protects-card-details-at-checkout
- `[M6]` https://www.mercadopago.com.br/ferramentas-para-vender/aceitar-pix
- `[M7]` https://www.mercadopago.com.br/developers/pt/docs/subscriptions/integration-test/payment-approval

**Asaas**
- `[A1]` https://asaas.com/precos-e-taxas
- `[A2]` https://docs.asaas.com/docs/assinaturas-recorrência
- `[A4]` https://docs.asaas.com/reference/criar-assinatura-com-cartao-de-credito
- `[A5]` https://docs.asaas.com/docs/faq-assinaturas
- `[A6]` https://docs.asaas.com/docs/criando-assinatura-com-cartao-de-credito
- `[A7]` https://docs.asaas.com/docs/pci-dss-1
- `[A8]` https://docs.asaas.com/docs/webhook-para-cobrancas
- `[A9]` https://docs.asaas.com/docs/eventos-para-assinaturas
- `[A10]` https://docs.asaas.com/docs/criar-novo-webhook-pela-api
- `[A11]` https://docs.asaas.com/docs/penaliza%C3%A7%C3%A3o-de-filas
- `[A12]` https://docs.asaas.com/docs/checkout-com-assinatura-recorrente
- `[A13]` https://docs.asaas.com/docs/aprovação-de-contas
- `[A14]` https://docs.asaas.com/docs/faq-de-notas-fiscais
- `[A15]` https://docs.asaas.com/changelog/libera%C3%A7%C3%A3o-p%C3%BAblica-do-pix-autom%C3%A1tico
- `[A16]` https://docs.asaas.com/docs/pix-automatico e https://docs.asaas.com/docs/diferença-entre-pix-automático-e-assinaturas

**Pagar.me, Iugu, Vindi, Paddle, Banco Central**
- `[P1]` https://docs.pagar.me/v5/docs/assinatura · `[P2]` https://docs.pagar.me/docs/webhooks · `[P3]` https://docs.pagar.me/docs/checkout_pagarme_skill_subscription · `[P4]` https://pagar.me/precos/ (página promocional, sem data)
- `[I1]` https://dev.iugu.com · `[I2]` https://dev.iugu.com/docs/cobranças-recorrentes-por-api-pix-automático · `[I3]` https://dev.iugu.com/docs/gatilhos · `[I4]` https://dev.iugu.com/docs/notificações
- `[V1]` https://developers.vindi.com.br/reference/introducao
- `[D1]` https://developer.paddle.com/changelog/2026/pix-automatico
- `[B1]` https://cdn-www.bcb.gov.br/content/estabilidadefinanceira/pix/automatico/guia_pix_automatico.pdf

**Pendências desta avaliação (NÃO VERIFICADO):** PCI/SAQ do Stripe; ciclos semestral e anual na doc do Stripe e do Mercado Pago; troca de plano e proration em todos; taxas de assinatura de Mercado Pago, Iugu, Vindi e Pagar.me; KYC e prazo de aprovação de Asaas, Mercado Pago, Pagar.me, Iugu e Vindi; SDKs Java de Mercado Pago, Asaas, Pagar.me, Iugu e Vindi; Pix Automático no Mercado Pago em doc técnica; data de lançamento do Pix Automático pelo Banco Central (a leitura confirmou só as regras do guia v1.2, não a data).
