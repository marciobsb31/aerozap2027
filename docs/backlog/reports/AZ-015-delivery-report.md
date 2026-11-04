# DELIVERY REPORT

Task: AZ-015

Agent: A1 (Backend/Arquitetura), com A0 (CTO) na revisão

Branch / PR: `feature/AZ-015-api-gateway` → `develop`

Status: READY_FOR_REVIEW

## Implementado

Serviço novo `apps/api-gateway`, a borda da plataforma. Faz quatro coisas e só quatro.

**Autenticação de entrada**
- Validação de JWT: assinatura, validade, emissor (`iss`) e público (`aud`). Fase 1 com segredo
  compartilhado (HS256), porque o serviço de identidade só chega na AZ-020; a troca por JWKS é de
  configuração, sem mexer no gateway.
- O serviço **não sobe** sem `JWT_SECRET` com pelo menos 32 caracteres. Falhar na subida é melhor
  que subir aceitando qualquer token.
- Erros em Problem Details (RFC 9457) com `correlationId`, no mesmo formato do resto da
  plataforma: 401 sem token válido, 403 com token válido sem permissão.
- Caminhos abertos: `/actuator/health`, `/actuator/info`, `/v3/api-docs`. Nada além.

**Limite de taxa**
- Janela fixa no Redis, por origem, com script Lua que incrementa e marca o vencimento na mesma
  operação (sem isso, uma falha no meio deixaria a origem bloqueada para sempre).
- Contador no Redis, logo o limite vale para o conjunto de instâncias e não por processo.
- 429 com `Retry-After` e cabeçalhos `X-RateLimit-Limit`/`Remaining`/`Reset`.
- **Falha em aberto**: Redis indisponível deixa a requisição passar, com aviso no log. Perder o
  limite por segundos é menos grave que derrubar o único caminho de entrada.
- Aplicado **antes** da autenticação, para que uma tentativa em massa com tokens inválidos não
  custe verificação de assinatura a cada requisição.

**Roteamento**
- Tabela de prefixo e destino. O prefixo da borda sai e o resto do caminho vai ao serviço, então
  o endereço público não amarra o caminho interno. Prefixo mais específico ganha; sem rota, 404.
- Repassa método, parâmetros, cabeçalhos e corpo; descarta os cabeçalhos ponto a ponto (RFC 9110);
  acrescenta `X-Correlation-Id` e os `X-Forwarded-*`; devolve o status do serviço como veio.
- Tempos limite explícitos (2 s para conectar, 15 s para responder); esgotado, 504.

**Fronteira garantida por teste**
- Teste de arquitetura que **falha o build** se o gateway passar a depender de banco, de fila ou
  de qualquer outro serviço. Regra de negócio na borda é regra em dois lugares, no ponto por onde
  passa todo o tráfego.

**Bibliotecas: dependências de dados agora são opcionais**
- `service-foundation` e `observability-starter` marcavam `spring-boot-starter-jdbc` e
  `spring-boot-starter-amqp` como dependências normais, o que arrastava banco e fila para
  qualquer serviço — inclusive para a borda, que não tem nem um nem outro (o Spring Boot exigiria
  uma fonte de dados e o serviço não subiria).
- Agora são `optional`: quem precisa declara. Em `FoundationAutoConfiguration`, Outbox, filas e
  DLQ passaram para uma configuração interna condicionada à presença das classes; a parte web
  (correlação e Problem Details) continua valendo para todo serviço.

## Arquivos alterados

- `apps/api-gateway/` (módulo novo: 11 classes, `application.yml`, script Lua, `logback-spring.xml`)
- `apps/api-gateway/src/test/` (6 classes de teste)
- `libs/java/service-foundation/pom.xml`, `FoundationAutoConfiguration.java`
- `libs/java/observability-starter/pom.xml`
- `pom.xml` (módulo novo)
- `scripts/dev-gateway-check.sh` (novo)
- `infrastructure/monitoring/prometheus/prometheus.yml` (coleta da borda)
- `.env.example`, `README.md`, `docs/architecture/api-gateway.md`

## Contratos

Nenhum contrato compartilhado mudou. Entram nomes públicos novos, que depois não se muda de
graça: o prefixo `/api/plataforma`, os cabeçalhos `X-RateLimit-*` e os tipos de problema
`urn:aerozap:problem:unauthorized`, `:forbidden` e `:rate-limit`.

**Alteração fora do módulo da tarefa.** A AZ-015 permite mexer em `apps/api-gateway`, e as
bibliotecas foram alteradas para tornar as dependências de dados opcionais — sem isso o gateway
não sobe. Mesmo dono (A1) e mudança compatível: `platform-core` já declara os dois starters e não
precisou de ajuste. Aprovado pelo CTO como parte desta entrega.

## Testes

- Unitários: **PASS** esperado — tabela de rotas (prefixo específico, prefixo parcial que não
  casa, validação que falha na subida) e arquitetura de fronteira.
- Integração: **PASS** esperado — 6 testes de autenticação (sem token, assinatura errada,
  expirado, outro emissor, outro público, token válido passa), 3 de limite de taxa (429 com
  cabeçalhos, limite antes da autenticação, health não conta) e 5 de encaminhamento (correlação,
  corpo do POST, status repassado, 504 por tempo esgotado, 404 sem rota). Redis real por
  Testcontainers; o destino é um servidor de teste que ecoa o recebido, para medir o gateway e
  não o serviço.
- Contrato: N/A nesta tarefa (contratos v0 são a AZ-017).
- Concorrência/idempotência: parcial — o contador do limite é atômico no Redis por desenho; o
  teste de concorrência de verdade é a AZ-018.
- Segurança: **PASS** por desenho — sem credencial no repositório, segredo obrigatório na subida,
  nenhum detalhe interno nas respostas de erro, limite antes da autenticação.

**Ainda não executados nesta máquina**: o container de desenvolvimento não tem Docker nem Maven.
A execução fica com o Product Owner (abaixo) e com a CI.

## Como verificar

```bash
bash scripts/build.sh
bash scripts/dev-gateway-check.sh
```

O script sobe a infraestrutura, o `platform-core` e a borda, assina um token de teste com
`openssl` e prova os três critérios: sem token é rejeitado (401), com token a resposta do serviço
volta pela borda com a correlação preservada, e o limite de taxa responde 429. Evidência em
`logs/dev-gateway-check.log`.

O ambiente está rodando: **não** — depende da execução na máquina do Product Owner.

## Evidências

- `logs/dev-gateway-check.log` (a anexar depois da execução)
- `logs/build.log` (a anexar: compilação e testes do módulo novo)
- Diagrama e decisões em `docs/architecture/api-gateway.md`

## Dependências

- AZ-014 (esqueleto de serviço) — atendida.
- A emissão de token é da AZ-020 (Identity). Até lá, o token de teste é assinado pelo script de
  verificação com o mesmo segredo.

## Riscos

- **Encaminhamento próprio, não Spring Cloud Gateway.** Decidido assim porque o Spring Cloud
  ainda não tem trem de versões confirmado para o Spring Boot 4.1, e a borda não pode depender de
  uma peça incerta. O custo: sem WebSocket e sem resposta em fluxo, que a Fase 1 não usa. Se vier
  a precisar, troca-se o encaminhador mantendo filtros e rotas.
- **Segredo compartilhado (HS256).** Qualquer serviço com o segredo pode emitir token válido. É
  aceitável enquanto só o gateway valida; com a AZ-020, passa a chave assimétrica (JWKS).
- **Limite por origem de rede.** Atrás de balanceador, o endereço real só chega com
  `server.forward-headers-strategy` ligado **e** acesso restrito ao balanceador — `X-Forwarded-For`
  é cabeçalho e qualquer um escreve nele. Documentado, desligado por padrão.
- **Falha em aberto no limite de taxa.** Escolha consciente: disponibilidade acima do limite.

## Correções feitas a partir do primeiro build

Primeira execução (04/11/2026): compilação limpa, testes de unidade e de arquitetura verdes
(tabela de rotas 5, fronteira 3), e **15 erros de contexto** nos testes de integração, todos com
a mesma causa: o Spring Boot 4.1 usa **Jackson 3** (`tools.jackson.databind`) e publica o mapeador
como `JsonMapper`; o código pedia o `ObjectMapper` do Jackson 2 (`com.fasterxml.jackson.databind`),
que está no classpath por compatibilidade mas não tem bean. Trocado o import nos quatro arquivos
que escrevem Problem Details direto na resposta.

Fica anotado para os próximos serviços: **em Boot 4, Jackson é `tools.jackson`**.

## Decisões necessárias

Nenhuma para fechar a tarefa. Para a AZ-020 ficam: formato das permissões no token (papéis ou
escopos) e passagem para JWKS.

## Checklist de pronto

- [x] Critérios de aceite atendidos no código, na documentação e nos testes
- [ ] Testes passando em CI (depende do push e da primeira execução do fluxo Java na CI)
- [x] Documentação atualizada
- [x] Sem credenciais, sem dados reais de usuários
- [x] Dados de demonstração rotulados (não há)
- [x] Logs sem dados sensíveis — o gateway não registra corpo nem token; o mascaramento da
      AZ-013 vale aqui também
- [x] Plano de reversão: remover `apps/api-gateway` dos módulos do POM pai; nada mais depende
      dele. As bibliotecas continuam compatíveis com o `platform-core`
