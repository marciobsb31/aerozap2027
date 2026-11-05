# Eventos

Os eventos são publicados pelo **Transactional Outbox**: gravados na mesma transação do fato que
descrevem e entregues depois. Isso garante que o evento existe se e somente se o fato aconteceu —
e obriga os consumidores a tolerar **entrega repetida**.

## Envelope

Todo evento viaja no mesmo envelope ([esquema](schemas/event-envelope.schema.json)):

```json
{
  "eventId": "9f1c2a44-5e31-4a6a-9a8d-2c0f6b1e77aa",
  "eventType": "search.completed",
  "schemaVersion": 1,
  "occurredAt": "2026-07-01T13:22:19Z",
  "correlationId": "7b3a1c90-0f2e-4c6b-9d41-5a2f8e3b6c11",
  "aggregateId": "sch_01HZ",
  "payload": "{ ... }"
}
```

- **`eventId`** é a chave de idempotência: o consumidor registra o que já processou e descarta a
  repetição. Entrega "pelo menos uma vez" sem isso vira débito dobrado.
- **`eventType`** é também a chave de roteamento no RabbitMQ.
- **`schemaVersion`** muda quando o payload muda de forma incompatível. O tipo continua o mesmo;
  o consumidor é que decide quais versões entende.
- **`payload`** é o JSON do evento, como texto.

## Os oito fatos

Convenção: `<domínio>.<fato no passado>`. Evento diz o que **aconteceu**; nunca o que alguém deve
fazer. Comando disfarçado de evento acopla quem publica a quem consome.

| `eventType` | Quem publica | Quem consome | Para quê |
|---|---|---|---|
| `search.requested` | `search` | workers de pesquisa | disparar a consulta aos provedores |
| `search.completed` | `search` | `platform-core`, `messaging` | confirmar a franquia; responder no canal |
| `search.failed` | `search` | `platform-core`, `messaging` | liberar a franquia; avisar |
| `usage.reserved` | `platform-core` | trilha de uso, Web | saldo reservado |
| `usage.confirmed` | `platform-core` | trilha de uso, Web | saldo consumido |
| `usage.released` | `platform-core` | trilha de uso, Web | saldo devolvido |
| `alert.price-target-reached` | `search` (alertas) | `messaging` | avisar que o preço bateu o alvo |
| `message.received` | `messaging` | `conversation-ai` | interpretar a mensagem |

Exemplos válidos e inválidos de cada payload estão em [`exemplos/`](exemplos/), em pastas
`event-<tipo>`, e são verificados pelo teste de contrato.

## Três decisões que valem explicar

**`search.completed` não quer dizer "achou".** `offerCount` pode ser zero, e o evento carrega
`providersSucceeded` e `providersFailed` em vez de um "sucesso" binário. É isso que permite
aplicar a política de consumo da [ADR-0004](../decisions/ADR-0004-reserva-de-franquia.md):
"ninguém respondeu" e "responderam, não há voo" levam a decisões diferentes sobre debitar a
consulta de quem pesquisou.

**`usage.*` existe mesmo com a reserva sendo síncrona.** A reserva em si é REST, porque a pesquisa
não pode começar sem a certeza de que há saldo. Os eventos servem à trilha de auditoria e à
interface — e são a rede de segurança: `platform-core` também consome `search.completed` e
`search.failed` de forma idempotente, caso a chamada REST de confirmação se perca.

**`message.received` não carrega conteúdo nem telefone.** Leva `messageId`, `contactRef` e o tipo.
Evento circula por fila, log e DLQ; o que não entra ali não vaza. O conteúdo fica no serviço de
mensageria, com o controle de acesso dele. O tipo do evento recusa um `contactRef` que pareça um
telefone — a regra é verificada em teste, não só escrita aqui.
