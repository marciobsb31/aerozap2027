# Esqueleto de serviço Java (AZ-014)

**Dono:** A1 (Backend) · **ADR:** 0001, 0002, 0003 · **Estado:** escrito; verificação por `scripts/build.sh` e `scripts/dev-service-check.sh`

## Módulos Maven
| Módulo | Função | Depende de |
|---|---|---|
| `libs/java/event-contracts` | `EventEnvelope` (contrato de evento). Sem frameworks e sem regra de negócio | nada |
| `libs/java/service-foundation` | Correlation ID, envelope de resposta, Problem Details (RFC 9457), Outbox, consumidor idempotente, topologia e DLQ. Auto-configuração Spring | event-contracts |
| `apps/platform-core` | Primeiro serviço, modelo para os outros quatro (`api-gateway`, `search`, `conversation-ai`, `messaging`) | service-foundation |

A versão do Java está em **uma propriedade** do `pom.xml` raiz (`java.version`). Começa em 21; o alvo é 25 (ADR-0003).

## O que a base técnica entrega
- **Correlation ID:** `X-Correlation-Id` lido (só valores curtos e seguros) ou gerado, no MDC dos logs e na resposta.
- **Envelope de sucesso:** `{ "data": ..., "meta": { "correlationId", "timestamp" } }`. **Erros:** Problem Details com `correlationId`, sem stack trace.
- **Transactional Outbox:** `OutboxService.append` exige transação (`MANDATORY`), então o evento só existe se a mudança de negócio for confirmada. `OutboxPublisher` publica em lotes com `for update skip locked` (várias instâncias não disputam as mesmas linhas), entrega "pelo menos uma vez".
- **Consumidor idempotente:** `IdempotentConsumer.process(eventId, consumidor, efeito)` grava `processed_event` e executa o efeito na mesma transação.
- **Mensageria:** exchange `aerozap.events` (tópico, chave = tipo do evento) e `aerozap.events.dlx`. `EventQueues.durableWithDlq` cria a fila do consumidor e sua DLQ. Três tentativas (1 s, 2 s) antes da DLQ.
- **Banco:** um schema por serviço (`platform_core`), migrações Flyway (`V1__fundacao_outbox_e_idempotencia.sql`).
- **Operação:** Actuator `health` com probes de liveness e readiness; OpenAPI em `/v3/api-docs`.

## Como criar o próximo serviço
1. Copie `apps/platform-core` para `apps/<serviço>` e renomeie pacote (`br.com.aerozap.<serviço>`), artefato e `spring.application.name`.
2. Troque o schema (`currentSchema` e `spring.flyway.schemas`) e a porta (`server.port`).
3. Registre o módulo em `pom.xml` e ajuste as regras de fronteira em `ArchitectureTest`.
4. Cada serviço tem a sua cópia das tabelas `outbox_event` e `processed_event` no seu schema.

## Testes
- `EventEnvelopeTest`, `WebFoundationTest`, `FoundationArchitectureTest`: unitários, sem Docker.
- `OutboxFlowIntegrationTest`: PostgreSQL e RabbitMQ reais (Testcontainers). Cobre: evento consumido uma só vez mesmo com mensagem duplicada, rollback sem evento, `append` fora de transação recusado, mensagem que falha chega à DLQ.
- `ServiceEndpointsIntegrationTest`: health, probes, envelope com correlação e OpenAPI.
- `ArchitectureTest`: sem dependência de outros serviços, sem ciclos, controllers sem JDBC direto.

## Como verificar
```bash
bash scripts/build.sh                 # compila e roda todos os testes (exige Docker em execução)
bash scripts/build.sh --sem-docker    # só compila e roda os testes unitários
bash scripts/dev-service-check.sh     # sobe a infraestrutura e o platform-core e confere o health
```
Os logs ficam em `logs/` (não versionados).

## Limitações conhecidas
- O serviço roda como processo Java na máquina de desenvolvimento. Dockerfile e serviço no Compose ficam para quando houver mais de um serviço.
- `PlatformCoreApplication` ainda não tem regra de negócio; ela entra nas tarefas das fases 2 em diante.
