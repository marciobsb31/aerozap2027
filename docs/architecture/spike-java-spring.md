# Spike de compatibilidade: Java, Spring Boot e dependências (AZ-014)

**Data:** 03/11/2026 · **Estado:** em andamento (preenchido conforme o `scripts/build.sh` rodar)

## Escolhas
| Item | Versão | Observação |
|---|---|---|
| Java | **21** (baseline) → 25 (alvo) | JDK 21 na máquina do PO. A passagem é a propriedade `java.version` do POM raiz |
| Spring Boot | 4.1.2 | Exige Java 17 ou superior (fonte: documentação oficial, lida na Fase 0) |
| springdoc-openapi | 3.0.0 | A confirmar com Spring Boot 4.1 |
| Testcontainers | gerenciado pelo BOM do Spring Boot | Artefatos `testcontainers-postgresql` e `testcontainers-rabbitmq` |
| ArchUnit | 1.4.1 | A confirmar |
| Flyway | gerenciado pelo BOM | `spring-boot-starter-flyway` + `flyway-database-postgresql` |

## Resultado
- [ ] Build com Java 21 e Spring Boot 4.1.2
- [ ] Testes unitários passando
- [ ] Testes de integração (Testcontainers) passando
- [ ] Build com Java 25 (feito quando o JDK 25 estiver instalado ou na CI)

## Fallback
O projeto **já começa no fallback** (Java 21), conforme a ADR-0003.
