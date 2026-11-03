# Spike de compatibilidade: Java, Spring Boot e dependências (AZ-014)

**Data:** 03/11/2026 · **Estado:** concluído para Java 21; Java 25 pendente (JDK 25 não instalado)

## Escolhas
| Item | Versão | Observação |
|---|---|---|
| Java | **21** (baseline) → 25 (alvo) | JDK 21 na máquina do PO. A passagem é a propriedade `java.version` do POM raiz |
| Spring Boot | 4.1.2 | Exige Java 17 ou superior (fonte: documentação oficial, lida na Fase 0) |
| springdoc-openapi | 3.0.0 | A confirmar com Spring Boot 4.1 |
| Testcontainers | gerenciado pelo BOM do Spring Boot | Artefatos `testcontainers-postgresql` e `testcontainers-rabbitmq` |
| ArchUnit | 1.4.1 | A confirmar |
| Flyway | gerenciado pelo BOM | `spring-boot-starter-flyway` + `flyway-database-postgresql` |

## Resultado (build de 03/11/2026, Windows 11, Maven 3.10.0, Docker Desktop)
- [x] **Build com Java 21 e Spring Boot 4.1.2:** `BUILD SUCCESS`. O build rodou no JDK **26.0.2** com `--release 21` (nenhum JDK 21 estava instalado), então o bytecode é de Java 21.
- [x] **Testes unitários e de arquitetura:** passando (contratos 3, base técnica 4, arquitetura do serviço 3).
- [x] **Testes de integração (Testcontainers, PostgreSQL e RabbitMQ reais):** 8 passando (fluxo do Outbox e endpoints).
- [x] **Dependências compatíveis com Spring Boot 4.1.2:** springdoc-openapi 3.0.0, Testcontainers (BOM), Flyway (`spring-boot-starter-flyway`), ArchUnit 1.4.1.
- [ ] **Build com Java 25:** pendente. Instale um JDK 25 (ou deixe a CI fazer isso) e troque `java.version` no `pom.xml` raiz.

## Observações
- O ArchUnit 1.4.1 emite um aviso benigno ao rodar no JDK 26 (`Error during import from jrt:/java.base/... falling back to simple import`). Os testes passam. O aviso some em Java 21 ou 25.
- O `JAVA_HOME` da máquina apontava para um JDK 25 removido. O `scripts/build.sh` agora procura um JDK instalado por conta própria.
- Os avisos 401 de repositórios do Flyway (`maven.pkg.github.com`, `red-gate`) vêm de repositórios opcionais do plugin e não afetam o build.

## Fallback
O projeto **já começa no fallback** (Java 21), conforme a ADR-0003. A decisão de adotar o Java 25 como alvo continua valendo.
