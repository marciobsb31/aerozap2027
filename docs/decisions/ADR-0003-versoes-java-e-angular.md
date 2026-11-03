# ADR-0003 — Versões de Java, Spring Boot e Angular

Data: 2026-11-03

Status: APROVADA em 2026-11-03 (Product Owner e CTO)

Autor: A0 (com A1 e A6)

Aprovador: A0 (CTO) e Product Owner

## Contexto
O DTC §9.1 pede "Java 21 ou 25, após compatibilidade das dependências" e Angular "em versão LTS/suportada na data de início".

Fatos verificados em 03/11/2026:
- A documentação do Spring Boot (versão 4.1.2) informa: "requires at least Java 17 and is compatible with versions up to and including Java 27", com Spring Framework 7.0.10 ou superior; Maven 3.6.3+ e Gradle 8.14+ / 9.x. Fonte: https://docs.spring.io/spring-boot/system-requirements.html
- Política do Angular: cada major tem cerca de 18 meses de suporte (6 de ativo e 12 de LTS). Na tabela consultada, o Angular 21 foi lançado em 19/11/2025 e tem LTS até 19/05/2027; o Angular 20 tem LTS até 28/11/2026, ou seja, **não deve ser adotado**. Fonte: https://v21.angular.dev/reference/releases (a página consultada é de uma versão anterior do site; confirmar a major mais recente em angular.dev na data do AZ-016).

## Opções consideradas
1. **Java 21 (LTS):** maduro, máxima compatibilidade de bibliotecas.
2. **Java 25 (LTS):** versão LTS mais recente, dentro da faixa suportada pelo Spring Boot 4.1; suporte mais longo.

## Decisão
- **Java 25 LTS** com **Spring Boot 4.1.x** (patch mais recente na data de AZ-014).
- AZ-014 inclui um **spike de compatibilidade**: Testcontainers, Resilience4j, Flyway, MapStruct (se usado), gerador de OpenAPI, Micrometer/OpenTelemetry, driver PostgreSQL e cliente RabbitMQ. Se algum bloqueio crítico aparecer, **fallback para Java 21** registrando a causa.
- **Angular:** a major estável mais recente que ainda tenha pelo menos 12 meses de suporte restante na data de AZ-016, com TypeScript `strict`. Atualizar de major por tarefa dedicada.
- Imagens de container com JRE da versão escolhida, sem root, tags fixas.

## Consequências
Suporte mais longo e recursos recentes da plataforma Java; custo: possível atrito pontual com bibliotecas. O fallback limita o risco.

## Plano de implementação e de reversão
Spike em AZ-014 com critérios: build, testes de integração com Testcontainers e execução em container verdes. Reversão para Java 21 é troca de versão no POM pai e na imagem base.

## Validação
Pipeline verde na versão escolhida; relatório curto do spike anexado ao DELIVERY REPORT de AZ-014.

## Nota de implementação (2026-11-03)
O Product Owner informou que o JDK instalado na máquina de desenvolvimento é o **Java 21**. Conforme o fallback previsto nesta ADR, o projeto **começa em Java 21** (`release` 21) com Spring Boot 4.1.x, e a CI usa o JDK 21. A versão fica em uma única propriedade do POM pai, de modo que a passagem para o **Java 25 LTS** é uma troca de uma linha, a fazer quando o JDK 25 estiver instalado e o spike da AZ-014 confirmar a compatibilidade das dependências. A decisão de adotar o Java 25 como alvo continua valendo.
