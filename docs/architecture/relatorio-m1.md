# Relatório do marco M1 — Fundação

**Autor:** A0 CTO Master · **Data:** 05/11/2026 · **Status:** dez tarefas entregues, em revisão do Product Owner. Três verificações dependem da máquina do PO e estão nomeadas na seção 4.

## 1. Entregas e parecer do CTO

| Tarefa | Entrega | Dono | Estado | Parecer |
|---|---|---|---|---|
| AZ-010 | Estrutura do repositório, convenções, branches, templates | A6 + A0 | REVIEW | Entregue |
| AZ-011 | Compose local (PostgreSQL, RabbitMQ, Redis, MinIO) | A6 | REVIEW | Entregue; sobe com um comando, verificado na máquina do PO |
| AZ-012 | Pipeline CI base | A6 | REVIEW | **Aprovado com ressalva:** `Dependency graph` ainda desligado no GitHub, então a revisão de dependências roda com `continue-on-error`. Ao ligar, remover a linha |
| AZ-013 | Observabilidade (métricas, logs, tracing, alertas) | A6 | **IN_PROGRESS** | Serviços sobem e o alerta dispara; falta a verificação final com logs no Loki e rastros no Tempo. Única tarefa do M1 que não está em REVIEW |
| AZ-014 | Esqueleto de serviço Java | A1 | REVIEW | Entregue; alvo Java 25 adotado com o spike aprovado (nota na ADR-0003) |
| AZ-015 | API Gateway/BFF (token, roteamento, limite de taxa) | A1 | REVIEW | Entregue; os três critérios provados na máquina do PO (`logs/dev-gateway-check.log`) |
| AZ-016 | Design system, layout, i18n pt/es, `api-client` do OpenAPI | A2 | REVIEW | Entregue; Angular 22, 57 testes |
| AZ-017 | Contratos v0 | A0 + A1 + A3 + A4 | REVIEW | Entregue; 26 tipos, 8 esquemas, 52 exemplos executáveis |
| AZ-018 | Estratégia de testes, matriz e harness de concorrência | A7 | REVIEW | Entregue; os três testes de concorrência ainda não foram executados por mim (precisam de Docker) |
| AZ-019 | Interpretador v0 e conjunto dourado pt/es | A3 | REVIEW | Entregue; 42 casos, defesa estrutural a injeção |

**Como revisei.** Li cada DELIVERY REPORT contra os critérios de aceite da TASK e rodei o build do
que podia ser rodado no ambiente do agente: os sete módulos Java e o frontend inteiro. O que exige
Docker ou a máquina do PO está nomeado como pendência, não como entregue. Não aceitei critério
marcado sem evidência reproduzível.

## 2. O que existe hoje, em uma frase cada

- **Ambiente**: `bash scripts/dev-up.sh` sobe PostgreSQL, RabbitMQ, Redis e MinIO; `obs-up.sh` sobe Prometheus, Alertmanager, Loki, Tempo e Grafana, com imagens de versão fixada.
- **CI**: um job por diretório alterado, com lint, testes, Testcontainers, CodeQL, varredura de segredos e um resultado único (`CI OK`) para a proteção de branch.
- **Serviços Java**: `platform-core` (esqueleto com Outbox, consumidor idempotente, Flyway, Problem Details e correlação), `api-gateway` (JWT, limite de taxa em Redis, encaminhamento) e `conversation-ai` (interpretação v0).
- **Contratos**: 8 esquemas JSON publicados, 52 exemplos que os testes executam, tipos Java selados.
- **Frontend**: Angular 22 com design system, troca pt-BR/es sem recarregar e cliente de API gerado do contrato OpenAPI.
- **Qualidade**: harness de concorrência, dados sintéticos, matriz requisito → teste → evidência verificada pela CI.

**Testes automatizados hoje: 213** — 156 Java fora dos de integração, 57 no frontend, mais os de
integração que exigem Docker.

## 3. Decisões tomadas no M1 e registradas

| Decisão | Onde |
|---|---|
| Java 25 como alvo, com o spike concluído | nota de implementação na ADR-0003 |
| Angular 22 (regra da ADR aplicada com a tabela da data) | nota de implementação na ADR-0003 |
| Bibliotecas TypeScript dentro do workspace Angular, não em `libs/typescript/` | nota de implementação na ADR-0001 |
| `aerozap-admin` adiado para o marco que precisar dele | nota na ADR-0001 |
| i18n em tempo de execução por signal (o nativo do Angular exige recarregar) | `frontend/README.md`, relatório da AZ-016 |
| Tempo fixado na linha 2.x (a 3.x removeu o ingester) | relatório da AZ-013 |
| Node 26 LTS na CI | relatório da AZ-016 |

## 4. Critérios de saída do M1

| Critério (plano §Fase 1) | Estado |
|---|---|
| Ambiente reproduzível com health checks | **Feito** — verificado na máquina do PO |
| CI verde | **Pendente de confirmação** — as execuções precisam ser conferidas na aba Actions depois dos dois últimos envios |
| Esqueleto de serviço publicado | **Feito** |
| Contratos v0 aprovados | **Pendente** — entregues e testados; falta o aceite do PO |

### Três verificações que dependem da máquina do PO

Nenhuma delas é código a escrever; são execuções que o ambiente do agente não consegue fazer.

1. **AZ-013, verificação final.** `bash scripts/obs-up.sh && SPRING_PROFILES_ACTIVE=observabilidade bash scripts/dev-service-check.sh && bash scripts/obs-verify.sh` — é o que move a AZ-013 para REVIEW.
2. **Testes de concorrência da AZ-018.** `mvn -pl apps/platform-core -am verify` — exige Docker, que o ambiente do agente não tem.
3. **Frontend.** Node 26 e `bash scripts/frontend-dev.sh` — a instalação de dependências pela ponte é cortada por tempo limite e recomeça.

## 5. Riscos e dívidas que atravessam para o M2

1. **Nenhum teste de isolamento entre usuários existe**, porque ainda não existe usuário. É o primeiro teste de segurança do M2 e o mais importante deles (RF-007, BOLA/IDOR).
2. **`Dependency graph` desligado** no GitHub: a revisão de dependências não bloqueia nada hoje.
3. **`CI OK` ainda não é verificação obrigatória** na proteção de branch. Enquanto não for, um PR vermelho pode ser mesclado.
4. **Decisões de produto em aberto que o M2 toca**: tipo de período da franquia (RN-02), política de "pesquisa concluída com sucesso" (RN-12) e método de cadastro (RF-001). O desenho do M2 as trata como **configuração**, não como valor fixo no código — é o que permite começar sem a resposta e mudar depois sem migração dolorosa. A escolha continua sua.
5. **Preço indicativo, não reservável** (achado do M0, D-06): segue valendo e molda o M3.

## 6. Próximos passos

1. As três verificações da seção 4.
2. Mesclar `feature/AZ-016-design-system` e depois `feature/AZ-018-estrategia-de-testes` (a segunda saiu da primeira).
3. Ligar o `Dependency graph` e tornar `CI OK` obrigatório na proteção de branch.
4. Fase 2 (M2): as TASKs AZ-020 a AZ-025 estão escritas e prontas para execução.
