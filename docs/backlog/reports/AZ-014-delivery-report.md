# DELIVERY REPORT

Task: AZ-014

Agent: A1 (Backend)

Branch / PR: `feature/AZ-014-esqueleto-java`

Status: READY_FOR_REVIEW (build e testes verificados; execução contra o Compose pendente)

## Implementado
- POM pai (Spring Boot 4.1.2, Java 21) e três módulos: `event-contracts`, `service-foundation`, `platform-core`.
- Base técnica: correlation ID, envelope de resposta, Problem Details (RFC 9457), Transactional Outbox (`for update skip locked`), consumidor idempotente, topologia de exchanges, filas com DLQ e três tentativas.
- `platform-core`: Actuator (health, liveness, readiness), Flyway, OpenAPI, schema próprio (`platform_core`).
- Testes de arquitetura de fronteira de módulos e relatório do spike (`docs/architecture/spike-java-spring.md`).
- Documentação: `docs/architecture/esqueleto-servico-java.md`. Scripts: `scripts/build.sh`, `scripts/dev-service-check.sh`.

## Arquivos alterados
`pom.xml`, `libs/java/**`, `apps/platform-core/**`, `scripts/build.sh`, `scripts/dev-service-check.sh`, `docs/architecture/esqueleto-servico-java.md`, `docs/architecture/spike-java-spring.md`, `docs/decisions/ADR-0003-versoes-java-e-angular.md` (nota), `.github/workflows/java-service.yml` (JDK 21).

## Contratos
Sim, **novo**: `EventEnvelope` (v1) e a topologia `aerozap.events` / `aerozap.events.dlx`. Ainda sem consumidores externos.

## Testes
- Unitários: **PASS** (contratos, envelope web, Problem Details, correlação)
- Integração: **PASS** (PostgreSQL e RabbitMQ reais; 8 testes)
- Contrato: N/A
- Concorrência/idempotência: **PASS** (mensagem duplicada produz um único efeito)
- Segurança: parcial (erro interno não vaza detalhes; correlação sanitizada)
- Arquitetura: **PASS**
Total: 18 testes, 0 falhas.

## Como verificar
```bash
bash scripts/build.sh               # compila e testa tudo (Docker em execução)
bash scripts/dev-service-check.sh   # sobe a infraestrutura e o serviço e confere o health
```
O ambiente está rodando: **Não** (o serviço sobe sob demanda pelo script).

## Evidências
`docs/backlog/reports/AZ-014-evidencia-build.log` (resumo do build: `BUILD SUCCESS`, 18 testes, 1 min 22 s).

## Dependências
AZ-010 e AZ-011 (feitas).

## Riscos
- O serviço roda como processo Java, não como contêiner do Compose. Dockerfile e serviço no Compose ficam para quando houver mais de um serviço (decisão registrada na documentação).
- Java 25 ainda não testado (JDK 25 ausente). A CI usa o JDK 21.
- O build local usou o JDK 26 com `--release 21`; um aviso benigno do ArchUnit aparece nesse JDK.

## Decisões necessárias
Nenhuma.

## Checklist de pronto
- [ ] Critérios de aceite atendidos (faltam: serviço respondendo contra o Compose via `dev-service-check.sh` e CI verde)
- [ ] Testes passando em CI
- [x] Documentação atualizada
- [x] Sem credenciais, sem dados reais de usuários
- [x] Dados de demonstração rotulados (filas e listeners de demonstração só em testes)
- [x] Logs sem dados sensíveis
- [x] Plano de reversão: remover `pom.xml`, `libs/java`, `apps/platform-core`
