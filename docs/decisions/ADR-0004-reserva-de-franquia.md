# ADR-0004 — Reserva de franquia (reservar → confirmar/liberar)

Data: 2026-11-03

Status: APROVADA em 2026-11-03 (Product Owner e CTO)

Autor: A0 (para detalhamento por A1)

Aprovador: A0 (CTO); a regra de "sucesso" depende do Product Owner

## Contexto
O fluxo do DTC §5.2 verifica o direito de uso no início (passo 3) e registra o consumo na conclusão (passo 9). Entre os dois passos, outra requisição do mesmo usuário (WhatsApp e Web em paralelo, duplo clique, retry) também passa na verificação. Para quem tem **uma** consulta restante, duas pesquisas seriam executadas e o saldo ficaria negativo ou uma delas seria cobrada sem direito. O critério do DTC §15.1 exige que apenas uma seja autorizada.

Restrições (AGENTS.md): falha interna ou de provedor não consome; repetição não debita duas vezes; sem transação aberta durante chamada a provedor ou IA; sem contador apenas em cache.

## Opções consideradas
1. **Verificar no início e debitar no fim** (DTC literal): permite a corrida acima.
2. **Debitar no início e estornar em caso de falha:** evita a corrida, mas gera estornos frequentes, saldo oscilante e janelas em que o usuário vê consumo que não ocorreu.
3. **Reservar no início, confirmar na conclusão, liberar na falha (proposta):** reserva atômica, saldo disponível = permitido − consumido − reservas ativas.

## Decisão
Adotar a opção 3.

Modelo (em `platform-core`, schema `usage`):
- `usage_period(id, user_id, period_start, period_end, allowance, consumed, reserved, status)`
- `usage_reservation(id, user_id, usage_period_id, search_id UNIQUE, idempotency_key UNIQUE, status, created_at, expires_at)`; estados `RESERVED → CONFIRMED | RELEASED | EXPIRED`
- `usage_event(...)` trilha imutável de reserva, confirmação, liberação e expiração (auditável pelo suporte)

Regras:
1. **Reservar:** em uma transação curta, `UPDATE usage_period SET reserved = reserved + 1 WHERE id = ? AND consumed + reserved < allowance`. Zero linhas atualizadas significa franquia esgotada. Inserir a reserva com `UNIQUE (search_id)` e `UNIQUE (idempotency_key)`; repetição com a mesma chave devolve a mesma reserva, sem novo débito.
2. **Confirmar** (`SearchCompleted`): `reserved − 1`, `consumed + 1`, reserva `CONFIRMED`. Idempotente por `search_id`.
3. **Liberar** (`SearchFailed`, cancelamento, erro interno ou de provedor): `reserved − 1`, reserva `RELEASED`. Idempotente.
4. **Expirar:** job libera reservas `RESERVED` cujo `expires_at` passou (TTL maior que o tempo máximo de pesquisa, configurável), cobrindo worker que caiu.
5. **Premium:** não usa contador; passa por rate limit técnico e proteção contra abuso.
6. **Política de consumo configurável**, enquanto o Product Owner não decidir "pesquisa concluída com sucesso" (chaves definidas no PRD v1, RN-12):
   - `usage.consume_on_partial`: `NEVER` | `IF_ANY_OFFER` | `ALWAYS` (alguns provedores falharam);
   - `usage.consume_on_zero_results`: `NEVER` | `ALWAYS` (todos responderam, sem ofertas);
   - falha total (erro interno ou todos os provedores com erro) **nunca** consome;
   - padrão proposto: `IF_ANY_OFFER` e `NEVER` (opção B do PRD).
7. **Mudança de período** (virada do trimestre): a reserva fica atrelada ao período em que nasceu.
8. Nenhuma chamada a provedor ou IA dentro da transação de reserva.

Interface entre deployables: `search` chama `platform-core` por REST (`POST /internal/usage/reservations`, `.../{searchId}/confirm`, `.../release`) e publica `SearchCompleted`/`SearchFailed` via Outbox; `platform-core` consome esses eventos de forma idempotente como rede de segurança.

## Consequências
Positivas: sem corrida na última consulta, sem estornos rotineiros, trilha auditável. Negativas: mais um estado a operar (reservas órfãs, tratadas pelo job de expiração) e uma chamada síncrona extra no início da pesquisa.

## Plano de implementação e de reversão
Implementado em AZ-022 por A1; contrato REST/eventos em AZ-017. Reversão exigiria voltar ao modelo do DTC; não recomendada, pois reintroduz a corrida.

## Validação
Testes de A7 (AZ-025): N requisições simultâneas pela última consulta (exatamente uma autorizada); repetição da mesma chave; worker morto com expiração; falha de provedor libera; confirmação duplicada não duplica; virada de trimestre; dois canais na mesma conta.
