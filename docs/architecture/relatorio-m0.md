# Relatório do marco M0 — Descoberta e decisões

**Autor:** A0 CTO Master · **Data:** 03/11/2026 · **Status:** Fase 0 entregue para revisão do Product Owner. **A Fase 1 não foi iniciada** (portão do plano §9, passo 5).

## 1. Entregas e parecer do CTO

| Tarefa | Entrega | Dono | Estado | Parecer |
|---|---|---|---|---|
| AZ-001 | `docs/architecture/diagnostico-tecnico.md` | A0 | REVIEW | Entregue |
| AZ-002 | `docs/decisions/decisoes-em-aberto.md` | A0 + A8 | REVIEW | Entregue; 15 decisões e 7 novas do PRD, cada uma com recomendação |
| AZ-003 | `docs/prd/PRD-v1.md` | A8 | REVIEW | **Aprovado com ressalva:** os requisitos de pesquisa (RF-022 a RF-036) devem ser revistos depois de D-06, pois dependem do que o provedor devolve |
| AZ-004 | `docs/architecture/providers/relatorio-avaliacao-provedores.md` | A4 | REVIEW | **Aprovado com ressalva:** a fonte do encerramento do Amadeus Self-Service é imprensa (corroborada por mais de um veículo); o aviso oficial não foi lido. Sabre, Travelport e outros não foram avaliados |
| AZ-005 | `docs/architecture/whatsapp/avaliacao-whatsapp.md` | A5 | REVIEW | **Aprovado com ressalva:** o texto oficial da cláusula de IA da Meta não foi lido; o jurídico precisa lê-lo |
| AZ-006 | `docs/architecture/billing/avaliacao-gateway-pagamento.md` | A1 | REVIEW | **Aprovado com ressalva:** parte das taxas está em páginas sem data de vigência; Pagar.me, Iugu e Vindi ficaram com muito "NÃO VERIFICADO" |
| AZ-007 | `docs/decisions/ADR-0001` a `ADR-0004` | A0 + A6 + A1 | **DONE** | **Aprovadas pelo Product Owner em 03/11/2026** |

**Como revisei:** conferi se cada afirmação relevante tem fonte oficial ou está marcada "NÃO VERIFICADO", se há invenção de preço, cota ou prazo, e a coerência com `AGENTS.md`, o DTC e o PRD. **Não refiz a verificação de cada URL.** Corroborei de forma independente os dois fatos que mais pesam nas decisões (encerramento do Amadeus Self-Service e política de IA do WhatsApp) em outros veículos. O A7 não participou, pois a Fase 0 não tem código.

## 2. O que a Fase 0 descobriu e muda o plano

1. **Provedores de voos (D-06).** Os provedores com dados ao vivo exigem aprovação e audiência que um produto novo provavelmente não cumpre. O candidato acessível entrega **preços em cache (2 a 7 dias), não reserváveis**. O marco M3 passa a significar "preço indicativo, rotulado", e o PRD e a UI devem refletir isso. Este é o achado de maior impacto de produto.
2. **Política de IA da Meta (D-08).** Pode enquadrar o AeroZap como "AI Provider" a critério da Meta. Mitigação: escopo restrito a passagens, vedar treino com dados do WhatsApp, leitura jurídica do texto oficial.
3. **Custo do WhatsApp (D-08).** Mensagens de serviço passaram a ser cobradas após 1.000 por mês por número, desde 01/10/2026; os valores em BRL precisam ser coletados.
4. **Pagamento (D-09).** O gateway com mais evidência (Stripe) não oferece Pix recorrente com conta no Brasil e repassa o cartão doméstico em 30 dias; não emite NFS-e.
5. **Identidade no WhatsApp.** Com *usernames*, o telefone pode não vir no webhook; o vínculo telefone-conta (AZ-021) precisa suportar telefone e BSUID.
6. **Franquia.** A reserva (ADR-0004) é necessária para cumprir o critério de concorrência do próprio DTC.

## 3. Ajustes ao plano propostos (não aplicados até sua decisão)
- Redefinir o critério de saída do M3 para "pesquisa de ponta a ponta com preço indicativo de fonte real rotulada", conforme D-06.
- Se você escolher a opção (c) de D-06, incluir uma tarefa **AZ-008**: ampliar a avaliação de provedores (Sabre, Travelport, Kayak e outros agregadores). Hoje ela não está no backlog.
- Incluir `bsuid` no contrato de vínculo telefone-conta (AZ-017 e AZ-021).

## 4. Critérios de saída do M0
| Critério | Estado |
|---|---|
| Decisões do Product Owner registradas | **Pendente** (você) |
| PRD v1 aprovado | **Pendente** (você) |
| Relatórios de provedores, WhatsApp e gateway entregues | Feito |
| ADRs 0001 a 0004 aprovadas | **Feito (03/11/2026)** |
| Pedidos de acesso externo enviados | **Pendente** (você; ver "O que começar agora" em `decisoes-em-aberto.md`) |

## 5. Próximos passos
1. Você revisa `decisoes-em-aberto.md` e responde por ID.
2. O CTO atualiza as ADRs para APROVADA, redige as ADRs 0005 a 0007 conforme as respostas e libera a Fase 1 (tarefas AZ-010 a AZ-019, já com TASK pronta).
3. Primeiras tarefas da Fase 1: AZ-010 (repositório), depois AZ-011 e AZ-012 (ambiente local e CI), em paralelo com AZ-016 (design system) e AZ-019 (IA v0).

## 6. Atualização de 03/11/2026 — Fase 1 liberada (parcialmente)
- O Product Owner aprovou as ADRs 0001 a 0004 e informou o remoto `https://github.com/marciobsb31/aerozap2027`.
- **O M0 segue aberto** nos itens: respostas às demais decisões (D-01, D-03 a D-11, D-13 a D-15, D-N1 a D-N7), aprovação do PRD v1 e pedidos de acesso externo.
- Foi iniciada a **AZ-010** (repositório, convenções e primeiro commit). As demais tarefas da Fase 1 aguardam a ordem do Product Owner.
