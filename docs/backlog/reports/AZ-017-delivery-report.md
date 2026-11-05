# DELIVERY REPORT

Task: AZ-017

Agent: A0 (CTO) + A1 (Backend) + A3 (IA) + A4 (Integrações)

Branch / PR: `feature/AZ-017-contratos-v0` → `develop`

Status: READY_FOR_REVIEW

## Implementado

Os contratos que atravessam fronteira, **antes** dos consumidores que vão depender deles.

**Pesquisa**
- `SearchIntent`: o que pesquisar, já estruturado — origem, destino, datas, passageiros, cabine,
  moeda, escalas, companhias, teto de preço e idioma. É a forma única produzida pelo formulário da
  Web, pelo interpretador de linguagem natural (AZ-019) e pelo WhatsApp.
  Passageiros como **objeto**, resolvendo a divergência do diagnóstico técnico (item 4).
- `FlightOffer` + `Itinerary`, `Segment`, `Money`: oferta normalizada de qualquer provedor, com
  `provider` e `checkedAt` obrigatórios e `demo` explícito.
- `ProviderResult` + `ProviderStatus`: resposta de provedor, **inclusive falha**. Erro é resultado,
  não exceção — é o que viabiliza a degradação controlada (RF-027).
- `FlightProvider`: a porta de saída, com o contrato de comportamento escrito junto (não lançar por
  falha do provedor, respeitar o prazo, não inventar campo, declarar simulação).

**Franquia (ADR-0004)**
- `ReservationRequest`, `ReservationResponse`, `ReleaseRequest`, `ReservationStatus`,
  `ReleaseReason`: a API interna de reserva, confirmação e liberação.

**Eventos**
- Os oito fatos: `search.requested`, `search.completed`, `search.failed`, `usage.reserved`,
  `usage.confirmed`, `usage.released`, `alert.price-target-reached`, `message.received`, com
  `EventTypes` reunindo tipo e versão de esquema.

**Erros**
- `ProblemTypes`: catálogo dos tipos de erro (RFC 9457), agora **usado** pela base técnica e pelo
  gateway em lugar de texto solto.

**Publicação**
- `docs/api-contracts/`: README com convenções e regras de compatibilidade, `erros.md`,
  `eventos.md`, 8 esquemas JSON e **52 exemplos** em 14 pastas.

## Arquivos alterados

- `libs/java/event-contracts/` (26 tipos novos, 5 classes de teste, `pom.xml`)
- `docs/api-contracts/` (README, erros, eventos, `schemas/`, `exemplos/`)
- `libs/java/service-foundation/.../ApiExceptionHandler.java` (usa o catálogo)
- `apps/api-gateway/` (usa o catálogo; declara `event-contracts`)
- `README.md`, `docs/backlog/`

## Contratos

É a tarefa que **cria** os contratos compartilhados. Nada foi quebrado porque nada dependia deles
ainda — e é exatamente esse o motivo de fazê-los agora. A partir deste commit, qualquer alteração
em `docs/api-contracts/` exige aprovação do CTO antes dos consumidores (`AGENTS.md`, regra 1).

Decisões de convenção que valem registro, porque custam caro para mudar depois:
- **`camelCase` no JSON.** A documentação mais antiga escreve `search_id`; o campo é `searchId`.
  Alinha com o que já estava no ar e testado (`correlationId`).
- **Instantes em UTC ISO-8601**; datas de calendário sem hora.
- **Dinheiro sempre com moeda**, nunca número solto.
- **`eventType` e `type` de erro só admitem acréscimo** — vivem em binding de fila e em código de
  terceiros.

## Testes

- Unitários: **PASS** — 81 testes no módulo. Regras de domínio cobertas: passageiros (bebê no
  colo, limite de 9), IATA, ordem das datas, moeda e idioma, encadeamento de trechos, conexão
  impossível, coerência entre status de provedor e ofertas, moeda do alerta, `contactRef` que
  parece telefone.
- **Teste de contrato: PASS** — `ContratosPublicadosTest` lê os 52 exemplos publicados: todo
  `valido-*.json` é aceito **e sobrevive à ida e volta** (serializa e lê de novo), todo
  `invalido-*.json` é recusado, todo contrato tem exemplo dos dois tipos e nenhuma pasta fica sem
  tipo correspondente. Um exemplo que deixar de casar quebra o build.
- Integração: N/A (contratos não têm infraestrutura).
- Segurança: **PASS** por desenho — `message.received` não carrega telefone nem conteúdo, e o
  tipo recusa `contactRef` com cara de telefone.

**Execução:** `BUILD SUCCESS` nos cinco módulos, 118 testes sem Docker, rodado no ambiente do
agente (Maven offline contra o cache do projeto). Os testes de integração com Testcontainers
dependem de Docker e rodam na máquina do Product Owner.

## Como verificar

```bash
bash scripts/build.sh
```

O teste de contrato roda junto. Para ler os contratos: `docs/api-contracts/README.md`.

O ambiente está rodando: **não se aplica** — esta entrega é de contratos; não há serviço novo.

## Evidências

- `BUILD SUCCESS` com 81 testes em `event-contracts` (54 deles de contrato)
- `docs/api-contracts/` com 8 esquemas e 52 exemplos
- Diagrama e decisões em `docs/api-contracts/README.md`, `erros.md` e `eventos.md`

## Dependências

- AZ-003 (decisões) — atendida.
- Desbloqueia: AZ-019 (interpretador), AZ-030 (adaptador simulado), AZ-032 (serviço de pesquisa),
  AZ-016 (`api-client` gerado), AZ-040 (WhatsApp).

## Riscos

- **Contrato nasce antes do uso real.** Algo só se revela errado quando o primeiro provedor de
  verdade responde. Mitigação: v0 é deliberadamente pequeno, e as regras de compatibilidade
  dizem o que pode mudar sem quebrar ninguém.
- **Sem esquema JSON dos payloads de evento.** Hoje só serviços da JVM os consomem, e os exemplos
  mais o teste cobrem o contrato. Entra quando houver consumidor fora da JVM. Registrado no README.
- **`fareConditions` é texto livre do provedor.** Estruturar bagagem exige normalização confiável
  entre fontes, que ainda não existe. A Web exibe como veio, com a fonte.
- **Validação só de forma.** Aeroporto existente, data futura e rota atendida ficam no serviço
  (RF-026). Proposital: pesquisa no histórico precisa poder ser relida anos depois.

## Decisões necessárias

Nenhuma para fechar. Ficam anotadas para quando os consumidores chegarem: estruturar bagagem,
esquema dos payloads de evento, múltiplos destinos (RF-020) e milhas (RF-030, bloqueada).

## Checklist de pronto

- [x] Critérios de aceite atendidos
- [ ] Testes passando em CI (depende do push e da fila do GitHub Actions)
- [x] Documentação atualizada
- [x] Sem credenciais, sem dados reais de usuários
- [x] Dados de demonstração rotulados — `FlightOffer.demo` é obrigatório e há exemplo publicado
- [x] Logs sem dados sensíveis — os eventos carregam referências, não dados de pessoa
- [x] Plano de reversão: a biblioteca não tem consumidor fora do próprio repositório; reverter é
      remover os tipos novos e a pasta de contratos
