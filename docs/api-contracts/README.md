# Contratos v0

> Entrega da **AZ-017** (marco M1). Contexto: DTC §8–9, [ADR-0004](../decisions/ADR-0004-reserva-de-franquia.md).
> **Estado: aprovado pelo CTO em 04/11/2026.** Versão `v0` (esquema de evento `schemaVersion` 1).

Contrato aqui é o que atravessa fronteira: entre serviços, entre o back-end e a Web, entre a
plataforma e um provedor. Dentro de um serviço, o código mude à vontade; o que está nesta pasta
tem outro custo, porque alguém do outro lado depende da forma exata.

Por isso os contratos v0 existem **antes** dos consumidores: a pesquisa, a IA, o WhatsApp e a Web
são escritos contra estas formas, e não o contrário.

## Onde cada contrato vive

| Contrato | Para que serve | Esquema | Tipo Java | Exemplos |
|---|---|---|---|---|
| **SearchIntent** | o que pesquisar, já estruturado | [search-intent](schemas/search-intent.schema.json) | `contracts.search.SearchIntent` | [8](exemplos/search-intent/) |
| **FlightOffer** | oferta normalizada de qualquer provedor | [flight-offer](schemas/flight-offer.schema.json) | `contracts.search.FlightOffer` | [7](exemplos/flight-offer/) |
| **ProviderResult** | resposta de um provedor, inclusive falha | [provider-result](schemas/provider-result.schema.json) | `contracts.search.ProviderResult` | [6](exemplos/provider-result/) |
| **FlightProvider** | porta de saída para provedores | — (interface Java) | `contracts.search.FlightProvider` | — |
| **ApiResponse** | envelope de toda resposta de sucesso | [api-response](schemas/api-response.schema.json) | `foundation.web.ApiResponse` | — |
| **ProblemDetail** | erro (RFC 9457) | [problem-detail](schemas/problem-detail.schema.json) | `ProblemDetail` do Spring | ver [erros.md](erros.md) |
| **EventEnvelope** | envelope de todo evento | [event-envelope](schemas/event-envelope.schema.json) | `contracts.EventEnvelope` | [3](exemplos/event-envelope/) |
| **Reserva de franquia** | API interna de consumo (ADR-0004) | [pedido](schemas/usage-reservation-request.schema.json), [resposta](schemas/usage-reservation-response.schema.json) | `contracts.usage.*` | [6](exemplos/usage-reservation-request/) |
| **Eventos base** | os oito fatos da plataforma | — | `contracts.events.*` | ver [eventos.md](eventos.md) |

Os tipos Java ficam em `libs/java/event-contracts`, pacote `br.com.aerozap.contracts`. A
biblioteca não tem dependência nenhuma em tempo de execução: são registros e enumerações de Java
puro, para que nenhum serviço herde escolha de framework por tabela.

## Convenções

- **Nomes de campo em `camelCase`**, no esquema e no JSON. Onde a documentação mais antiga escreve
  `search_id`, o campo é `searchId`.
- **Enumerações em `MAIUSCULA_COM_SUBLINHADO`**, em inglês, como o resto dos contratos.
- **Instantes em UTC, ISO-8601** (`2026-07-10T22:15:00Z`). Horário local sem fuso é a origem
  clássica de erro em voo internacional; quem exibe converte.
- **Datas sem hora** (`departureDate`) em `AAAA-MM-DD`: é um dia de calendário, não um instante.
- **Dinheiro nunca é número solto**: sempre `{ "amount": 4820.35, "currency": "BRL" }`.
- **Aeroportos em IATA de três letras maiúsculas**; companhias em dois caracteres.
- **Campo que o provedor não informa vem nulo.** A plataforma não preenche lacuna com estimativa.

## O que o contrato valida — e o que não valida

Os tipos validam **forma e coerência interna**: formato de código IATA, ordem das datas, soma de
passageiros, moeda, encadeamento dos trechos de um itinerário.

Eles **não** validam o que depende de dados ou de relógio: se o aeroporto existe, se a data é
futura, se a rota é atendida, se o usuário tem saldo. Isso é do serviço (RF-026). A separação é
deliberada: uma pesquisa guardada no histórico precisa poder ser lida de novo daqui a um ano sem
estourar porque a data virou passado.

## Versionamento e compatibilidade

Versão atual: **v0** dos contratos; `schemaVersion` **1** nos eventos.

Mudança **compatível** (não exige versão nova), desde que anunciada:
- acrescentar campo opcional;
- acrescentar valor de enumeração **em campo de saída**, se os consumidores já tratam desconhecido;
- afrouxar uma validação.

Mudança **incompatível** (exige `schemaVersion` novo no evento, ou caminho novo na API):
- remover ou renomear campo;
- tornar obrigatório um campo que era opcional;
- mudar tipo ou unidade;
- apertar validação de algo já aceito;
- acrescentar valor de enumeração em campo de **entrada**.

Dois contratos têm nome que **nunca** se renomeia, porque vivem em binding de fila e em código de
terceiros: `eventType` ([eventos.md](eventos.md)) e `type` de erro ([erros.md](erros.md)). Nesses,
só se acrescenta.

Alterar qualquer arquivo desta pasta exige aprovação do CTO **antes** de mexer nos consumidores —
é a regra inegociável nº 1 do `AGENTS.md`.

## Como o contrato se mantém verdadeiro

Documentação de contrato apodrece em silêncio. Aqui não: cada arquivo de `exemplos/` é lido pelo
teste `ContratosPublicadosTest`, que exige

- todo exemplo `valido-*.json` seja aceito pelo tipo correspondente, e sobreviva à ida e volta
  (serializar e ler de novo);
- todo exemplo `invalido-*.json` seja recusado;
- todo contrato tenha pelo menos um exemplo de cada;
- nenhuma pasta de exemplo fique sem tipo.

Um exemplo que deixa de casar, ou uma regra que some do tipo, quebra o build.

```bash
bash scripts/build.sh    # roda o teste de contrato junto com o resto
```

## O que v0 ainda não cobre

- **Esquema JSON dos payloads de evento.** Hoje os eventos são consumidos só por serviços da JVM,
  que usam os tipos da biblioteca; os exemplos e o teste cobrem o contrato. O esquema entra quando
  houver consumidor fora da JVM.
- **Múltiplos destinos** (RF-020): `FlightOffer` aceita um ou dois itinerários.
- **Milhas** (RF-030): bloqueado por decisão em aberto.
- **Bagagem estruturada**: hoje `fareConditions` é o texto do provedor. Estruturar exige
  normalização confiável entre fontes, que não temos ainda.
