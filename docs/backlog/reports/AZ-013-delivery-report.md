# DELIVERY REPORT

Task: AZ-013

Agent: A6 (DevOps/SRE), com A0 (CTO) na revisão

Branch / PR: `feature/AZ-013-observabilidade` → `develop`

Status: READY_FOR_REVIEW

## Implementado

**Infraestrutura (`infrastructure/monitoring/`)**
- Prometheus (linha LTS) com coleta do serviço de exemplo, do RabbitMQ agregado e do RabbitMQ
  **por fila** (`/metrics/detailed`), retenção de 72 h e recarga por API habilitada.
- 5 regras de alerta: serviço indisponível, taxa de 5xx alta, fila morta com mensagens, Outbox
  acumulando e um **alerta de teste que dispara de propósito**, para comprovar o caminho até o
  Alertmanager sem precisar quebrar nada.
- Alertmanager com receptor sem integração: no ambiente local o alerta fica visível na interface
  e **não sai da máquina**. Regra de inibição para não avisar duas vezes quando o serviço cai.
- Loki com ingestão por OTLP/push, retenção de 72 h e compactação com remoção habilitada.
- Tempo recebendo OTLP em HTTP (4318) e gRPC (4317), retenção de 72 h.
- Grafana com fontes de dados e painel **provisionados como código**, ida e volta entre log e
  rastro (campo `TraceID` no log abre o rastro; o rastro filtra os logs do mesmo `traceId`) e
  abertura direta no painel de saúde.
- Painel "saúde dos serviços e filas": serviços no ar, 5xx na janela, Outbox pendente, mensagens
  na DLQ, requisições por segundo e p95 por rota, memória da JVM, CPU, profundidade das filas e
  os últimos logs.
- Plugin `rabbitmq_prometheus` ligado por arquivo versionado, sem alterar o Compose base.

**Biblioteca `libs/java/observability-starter/`**
- Métrica `aerozap_outbox_pendentes`: eventos gravados no Outbox e não publicados — o sinal mais
  direto de entrega de eventos parada. Tolera banco ausente (publica indefinido, não quebra).
- Rastros por OTLP e exportação Prometheus pelo Actuator, via configuração automática.
- **Mascaramento automático de dados sensíveis**, em duas frentes: `%msgSeguro` (palavra de
  conversão aplicável a qualquer padrão de saída: console, arquivo, Loki) e
  `MascaraNoLogEstruturado` (ponto de extensão do log estruturado do Spring Boot, que cobre
  mensagem e campos do MDC). Mascara telefone com DDI/DDD, e-mail, CPF, CNPJ, cartão,
  `Bearer`/`Basic` e pares `senha=`/`token=`/`api_key=`.
- Preserva de propósito UUID de correlação, portas, durações e contagens: mascarar esses números
  cegaria o diagnóstico.
- `logback-base.xml` reaproveitável pelos cinco serviços: console legível com correlação e envio
  ao Loki apenas quando o perfil `observabilidade` está ativo.

**Serviço de exemplo (`apps/platform-core`)**
- Endpoint `prometheus` exposto, baldes de histograma ligados (sem eles não há p95 por rota),
  exportação OTLP configurável por variável e `logback-spring.xml` incluindo a base da biblioteca.

**Scripts e documentação**
- `scripts/obs-up.sh`, `scripts/obs-verify.sh` (evidência em `logs/obs-verify.log`) e
  `scripts/obs-down.sh` (`--apagar-dados` remove os volumes).
- `scripts/probe-tags.sh`: lista as versões publicadas no Docker Hub, para fixar imagem sem
  depender de tag flutuante.
- `docs/architecture/observabilidade.md`: arquitetura, métricas, logs, rastros, alertas,
  retenção e o que muda em produção.
- `docs/architecture/convencoes.md`: regra de log sem dado de pessoa.
- CI e `ci-local.sh` passam a validar também o Compose da observabilidade e a conferir as tags
  de imagem em **todos** os arquivos Compose.

## Arquivos alterados

- `infrastructure/monitoring/` (Prometheus, Alertmanager, Loki, Tempo, Grafana, RabbitMQ, Compose)
- `libs/java/observability-starter/` (biblioteca completa, com testes)
- `apps/platform-core/` (`pom.xml`, `application.yml`, `logback-spring.xml`)
- `pom.xml` (módulo novo e versões gerenciadas)
- `scripts/obs-up.sh`, `scripts/obs-down.sh`, `scripts/obs-verify.sh`, `scripts/probe-tags.sh`,
  `scripts/ci-local.sh`
- `.github/workflows/ci.yml`
- `.env.example`, `README.md`, `docs/architecture/observabilidade.md`,
  `docs/architecture/convencoes.md`

## Contratos

Nenhum contrato compartilhado mudou. A métrica `aerozap_outbox_pendentes` e os rótulos
`service_name`/`nivel` nos logs são nomes públicos novos: mudá-los depois quebra painéis e
alertas, por isso entram documentados.

## Testes

- Unitários: **PASS** — 21 testes do `observability-starter` verdes no build de 04/11/2026
  (mascarador, inclusive os casos que devem ser preservados, e palavra de conversão).
- Integração: N/A nesta tarefa (a verificação é o `obs-verify.sh` contra os contêineres reais).
- Contrato: N/A
- Concorrência/idempotência: N/A
- Segurança: **PASS** por desenho — nenhuma credencial no repositório (senha do Grafana vem do
  `.env`), portas publicadas só em `127.0.0.1`, alerta não sai da máquina, log mascarado.

## Como verificar

```bash
bash scripts/dev-up.sh
bash scripts/obs-up.sh
SPRING_PROFILES_ACTIVE=observabilidade bash scripts/dev-service-check.sh
bash scripts/obs-verify.sh
```

Depois, no Grafana (http://localhost:3000, usuário e senha do `.env`): o painel abre direto em
"saúde dos serviços e filas"; em Explore, `{service_name="platform-core"} | logfmt` mostra os
logs e o campo `TraceID` abre o rastro no Tempo. Em http://localhost:9093 o `AlertaDeTeste`
comprova o caminho de alertas.

O ambiente está rodando: **não** — depende da execução na máquina do Product Owner (o container
de desenvolvimento não tem Docker).

## Evidências

- `logs/build.log` (04/11/2026 10:11): `BUILD SUCCESS`, `observability-starter ... SUCCESS`,
  21 testes verdes; o console já sai com `[platform-core] [sem-correlacao]` e a mensagem mascarada
- `logs/obs-verify.log` (a anexar depois de subir os contêineres)
- `logs/probe-tags.log` (04/11/2026 10:08): versões publicadas que justificam as tags fixadas —
  Prometheus v3.13.5 (LTS), Alertmanager v0.34.2, Grafana 13.2.5, Loki 3.7.9, Tempo 3.1.1
- Lint: `shellcheck -S warning` limpo em `scripts/*.sh` e `scripts/lib/*.sh`; `actionlint` limpo
- YAML de todas as configurações de monitoramento validado

## Dependências

- AZ-011 (ambiente local) — atendida.
- Depende da execução na máquina do Product Owner para anexar as evidências de runtime.

## Riscos

- **Configuração de terceiros parcialmente exercitada.** O build de 04/11/2026 confirmou que o
  `logback-base.xml` carrega (console com correlação e `%msgSeguro` funcionando) e que a classe do
  appender do Loki existe; as chaves internas desse appender e os arquivos do Loki e do Tempo só
  são confirmados ao subir os contêineres. Se algo divergir, aparece no `obs-verify.sh`.
- **Memória.** A observabilidade soma cerca de 2 GB. Por isso ela é opcional e separada do
  ambiente base: `dev-up.sh` não a sobe.
- **O mascaramento é rede de segurança, não permissão.** Registrar dado de pessoa e confiar na
  máscara continua sendo erro de revisão.
- **Alerta de teste.** Dispara sempre, de propósito. Se virar ruído, basta comentar o grupo
  `aerozap-teste`.

## Correções feitas a partir do primeiro build

- `conversionRule` usa `class`, não `converterClass` (atributo depreciado na versão de logback do
  Spring Boot 4.1).
- Envio OTLP de métricas desligado (`management.otlp.metrics.export.enabled: false`): o
  `spring-boot-starter-opentelemetry` liga o empurrão de métricas por padrão, e aqui a coleta é
  pelo Prometheus; sem isso o serviço tentaria empurrar métricas ao Tempo a cada minuto.
- `obs-verify.sh` passou a conferir o plugin `rabbitmq_prometheus` por
  `rabbitmq-plugins list -e`, sem passar caminho (o Git Bash converte caminhos em argumentos).

## Correções feitas a partir da primeira execução com Docker (04/11/2026)

A pilha subiu, e três coisas apareceram:

- **Tempo em laço de reinício.** `field ingester not found in type app.Config`: o Tempo 3 removeu
  ingester e compactor e mudou para ingestão por Kafka. Fixado na linha **2.10.10**, que é a de
  binário único; a volta para a 3.x é decisão da Fase 7.
- **Grafana parecia fora do ar e não estava.** O contêiner sobe saudável, mas gasta dezenas de
  segundos tentando pré-instalar plugins contra `grafana.com`, sem internet no contêiner.
  Pré-instalação desligada, e a verificação passou a tentar cinco vezes antes de acusar falha.
- **Verificação do Loki rígida demais.** O `/ready` responde 503 enquanto o ingester aquece e,
  sem nenhum log gravado, a consulta de rótulos volta com sucesso mas sem o campo `data`. Os dois
  casos eram normais e eram tratados como falha.

O que já estava comprovado nessa mesma execução: Prometheus e Alertmanager de pé, **alerta de
teste chegando ao Alertmanager**, RabbitMQ coletado com o plugin `rabbitmq_prometheus` ativo e as
métricas por fila disponíveis.

## Decisões necessárias

Nenhuma para fechar a tarefa. Para a Fase 7 ficam registradas: taxa de amostragem de rastros em
produção, destino real dos alertas (plantão) e política de retenção.

## Checklist de pronto

- [x] Critérios de aceite atendidos no código e na documentação
- [ ] Testes passando em CI (depende do push e da primeira execução do fluxo Java na CI)
- [x] Documentação atualizada
- [x] Sem credenciais, sem dados reais de usuários
- [x] Dados de demonstração rotulados (não há)
- [x] Logs sem dados sensíveis — mascaramento automático e testado
- [x] Plano de reversão: `bash scripts/obs-down.sh --apagar-dados` remove tudo; o ambiente base
      segue intacto, pois a observabilidade vive em arquivo Compose separado
