# Relatório do marco M1 — Fundação

**Autor:** A0 CTO Master · **Data:** 05/11/2026 · **Status:** dez tarefas entregues e verificadas, aguardando o aceite do Product Owner (atualizado em 05/11/2026).

## 1. Entregas e parecer do CTO

| Tarefa | Entrega | Dono | Estado | Parecer |
|---|---|---|---|---|
| AZ-010 | Estrutura do repositório, convenções, branches, templates | A6 + A0 | REVIEW | Entregue |
| AZ-011 | Compose local (PostgreSQL, RabbitMQ, Redis, MinIO) | A6 | REVIEW | Entregue; sobe com um comando, verificado na máquina do PO |
| AZ-012 | Pipeline CI base | A6 | REVIEW | **Aprovado com ressalva:** `Dependency graph` ainda desligado no GitHub, então a revisão de dependências roda com `continue-on-error`. Ao ligar, remover a linha |
| AZ-013 | Observabilidade (métricas, logs, tracing, alertas) | A6 | REVIEW | Entregue; métricas, logs, rastros e alerta verificados (seção 7) |
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
| CI verde | **Feito** — `CI OK` verde em `develop` |
| Esqueleto de serviço publicado | **Feito** |
| Contratos v0 aprovados | **Pendente** — entregues e testados; falta o aceite do PO |

### Verificações que dependiam da máquina do PO

Executadas em 05/11/2026; o resultado está na seção 7.

## 5. Riscos e dívidas que atravessam para o M2

1. **Nenhum teste de isolamento entre usuários existe**, porque ainda não existe usuário. É o primeiro teste de segurança do M2 e o mais importante deles (RF-007, BOLA/IDOR).
2. ~~`Dependency graph` desligado~~ — já estava ativo; a revisão de dependências voltou a bloquear.
3. ~~`CI OK` não obrigatório~~ — agora exigido em `main` e `develop`.
4. **Decisões de produto em aberto que o M2 toca**: tipo de período da franquia (RN-02), política de "pesquisa concluída com sucesso" (RN-12) e método de cadastro (RF-001). O desenho do M2 as trata como **configuração**, não como valor fixo no código — é o que permite começar sem a resposta e mudar depois sem migração dolorosa. A escolha continua sua.
5. **Preço indicativo, não reservável** (achado do M0, D-06): segue valendo e molda o M3.

## 6. Próximos passos

1. Aceite do Product Owner: contratos v0 e o PR de `develop` para `main`.
2. Fase 2 (M2): concluir AZ-020 e executar AZ-021, AZ-023, AZ-024 e AZ-025.

## 7. O que a execução real revelou (05/11/2026)

Os testes de integração da identidade e da franquia e a verificação da observabilidade rodaram pela
primeira vez. Todos tinham sido entregues compilando, e cinco defeitos só apareceram ao executar:

| Defeito | Efeito | Severidade (`AGENTS.md` §8) |
|---|---|---|
| Reuso de refresh token não derrubava a família | Token possivelmente roubado continuava valendo | Crítico |
| Limite de tentativas de login quebrado | Senha errada respondia 500; força bruta nunca era bloqueada | Alto |
| Corrida na criação do período da franquia | 9 de 32 primeiras consultas simultâneas respondiam 500; saldo correto | Alto |
| Configuração do Loki4j no formato antigo | Nenhum log chegava ao Loki | Médio |
| `obs-verify.sh` aprovava sem logs nem rastros | Verificação verde sem o que deveria provar | Médio |

Todos corrigidos. Dois testes estavam errados, e não o código: a senha curta (esperava 422, o catálogo
define 400) e a mesma chave simultânea (exigia ausência de duplicatas onde 32 respostas iguais são o
resultado certo).

**Lição para as próximas fases:** nenhuma tarefa vai para REVIEW com testes de integração sem
execução. Nessas tarefas, compilar não provava que funcionava.

**Build completo:** 231 testes, zero falhas, com Docker local; `CI OK` verde em `develop`.
