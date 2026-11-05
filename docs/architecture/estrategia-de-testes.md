# Estratégia de testes do AeroZap

> AZ-018 · responsável A7 (QA & Security) · aprovada por A0 · 2026-11-05

Este documento diz **o que testamos, onde, com que dado e quando uma entrega pode passar**. Ele
vale para todo módulo; onde um módulo precisar divergir, a divergência vira ADR, não exceção
silenciosa.

## O que esta estratégia tenta evitar

O AeroZap gasta dinheiro de terceiros (consultas a provedores), controla uma franquia que vale
dinheiro, conversa com pessoas por um canal que entrega mensagem mais de uma vez e usa um modelo de
linguagem que lê texto de quem pode querer enganá-lo. Nessa lista, três classes de defeito não
aparecem em teste de caminho feliz e custam caro quando chegam em produção:

1. **Corrida.** Duas requisições pela última consulta Free, duas entregas da mesma mensagem, dois
   webhooks do mesmo pagamento. O código sequencial passa; a invariante quebra sob simultaneidade.
2. **Isolamento.** Um usuário alcançando pesquisa, conversa ou alerta de outro. Nenhum teste
   funcional encontra isso: é preciso tentar de propósito.
3. **Dado inventado.** Preço, horário ou link que não veio de provedor; resposta de IA que parece
   certa. Um teste que compara com o que o próprio sistema produziu nunca pega isso.

Toda a estratégia é organizada em torno dessas três, e não em torno de percentuais de cobertura.

## Camadas

| Camada | Onde roda | O que prova | Custo |
| --- | --- | --- | --- |
| **Unitário** | módulo, sem Spring | regra de negócio, contrato de tipo, cálculo, parser | milissegundos |
| **Contrato** | `libs/java/event-contracts`, `docs/api-contracts` | o que entra e sai é exatamente o publicado, em exemplos válidos e inválidos | milissegundos |
| **Arquitetura** | ArchUnit, por módulo | fronteiras: quem pode depender de quem | milissegundos |
| **Integração** | Testcontainers (PostgreSQL, RabbitMQ, Redis) | SQL, migração, transação, mensageria — contra o serviço de verdade, nunca contra simulação de banco | segundos |
| **Concorrência** | harness próprio + Testcontainers | invariante sob N operações simultâneas | segundos |
| **Frontend** | Vitest + jsdom | componente, i18n, acessibilidade (axe-core) | segundos |
| **E2E** | `tests/e2e` | jornada completa, Web e WhatsApp | minutos |
| **Carga** | `tests/load` | ramp-up, pico, soak, recuperação | minutos |
| **Segurança** | `tests/security` + CodeQL + auditoria de dependências | OWASP Top 10 e API Top 10, segredo em log, injeção de prompt | varia |

A pirâmide é consequência do custo, não dogma: o teste de integração é melhor que o unitário sempre
que a dúvida é sobre o banco, e pior sempre que a dúvida é sobre uma regra.

**Banco de verdade, sempre.** Nenhum teste de persistência usa banco em memória. H2 aceita SQL que
o PostgreSQL recusa e tem outro modelo de isolamento — exatamente a parte que precisamos provar.

## Testes de concorrência

O harness é `tests/concorrencia` (`ArenaDeConcorrencia`). Ele existe por um motivo específico: um
laço que cria N tarefas e sai executando quase nunca coloca duas delas dentro da seção crítica ao
mesmo tempo. A primeira termina antes de a última nascer, o teste fica verde, e a corrida aparece
em produção. A arena segura todas as tarefas numa linha de largada e solta juntas.

```java
var resultado = ArenaDeConcorrencia.disparar(32, indice -> reservarConsulta(usuario));

assertThat(resultado.totalDeFalhas()).isZero();          // negar é resposta, não erro
assertThat(resultado.sucessosQue(Boolean.TRUE::equals)).isEqualTo(1);
```

Regras para quem escreve um teste desses:

- **A invariante é do teste, não do harness.** O harness só coleta fatos. Um harness que soubesse
  afirmar serviria para um cenário só.
- **Repita.** `ArenaDeConcorrencia.repetir(...)` roda a arena várias vezes com preparação entre
  elas. Corrida é probabilística: a que escapa aparece uma vez a cada vinte execuções.
- **Negar não é falhar.** Se a invariante é "uma autorização", as outras 31 operações devolvem
  `false` — não lançam exceção. Teste que conta exceção como recusa esconde erro de verdade.
- **Nada de pausa para "dar tempo".** `Thread.sleep` só aparece quando a pausa **é** o objeto do
  teste (abrir de propósito a janela de um padrão errado, como no contraexemplo da franquia).
  Pausa para sincronizar é teste instável disfarçado.
- **Travar é defeito.** A arena falha em 60 segundos dizendo quantas tarefas não responderam.

Os três casos escritos hoje:

| Teste | Invariante |
| --- | --- |
| `ReservaDeFranquiaConcorrenciaIntegrationTest` | 32 requisições pela última consulta → exatamente uma autorizada, saldo nunca negativo. Inclui o contraexemplo: decidir na aplicação em vez de no banco autoriza mais do que havia. |
| `OutboxConcorrenciaIntegrationTest` | o mesmo evento gravado 24 vezes ao mesmo tempo vira uma linha; 24 eventos distintos viram 24 linhas, nenhuma perdida |
| `ConsumidorIdempotenteConcorrenciaIntegrationTest` | 24 entregas simultâneas do mesmo evento executam o efeito uma vez; consumidores diferentes têm cada um a sua |

O serviço de franquia é do marco M2. A invariante foi escrita antes da implementação de propósito:
é a classe de defeito que não aparece em revisão de código e cobra caro depois. Quando o serviço
existir, o teste passa a chamá-lo; a invariante não muda.

## Dados de teste

`DadosSinteticos` (em `tests/concorrencia`) é a única fonte de dado de pessoa em teste.

Gerar nome e telefone ao acaso **não** cumpre a regra de "nunca dados reais": um telefone plausível
gerado ao acaso é o telefone de alguém, e um dia um teste manda mensagem para ele ou o grava num
log. Por isso cada tipo usa uma faixa que o mundo reserva para exemplo:

| Tipo | Faixa | Por quê |
| --- | --- | --- |
| e-mail | `@...invalid` | RFC 6761: não resolve e não pode ser registrado |
| telefone | `+999…` | código de país reservado pela ITU-T E.164, não roteável |
| CPF | dígito verificador errado de propósito | formato certo para exercitar máscara e validação, nunca corresponde a documento real |
| nome | lista fictícia fixa | sem sobrenome de pessoa real |
| aeroporto | IATA real | infraestrutura pública, não dado pessoal |

Tudo é determinístico a partir de uma semente: a mesma execução produz os mesmos dados. Teste que
muda de entrada a cada rodada falha de forma irreproduzível, e falha irreproduzível é tratada como
ruído até o dia em que não era.

**Dado de produção nunca entra em teste**, nem anonimizado: anonimização é reversível com mais
frequência do que parece, e a regra sem exceção é a que se consegue cumprir.

## Provedores externos

| Situação | O que vale |
| --- | --- |
| Teste automatizado (unitário, integração, E2E, carga) | **sempre** adaptador simulado ou sandbox do provedor |
| Carga | **nunca** contra provedor real — custo e termos de uso |
| Verificação funcional de uma integração | chamada real **só** com credencial de teste autorizada, registrada no DELIVERY REPORT |

O adaptador simulado devolve dado marcado `demo: true`, e esse campo é obrigatório no contrato da
oferta. Ele sobe até a interface e até a mensagem do WhatsApp: dado simulado que chega ao usuário
sem rótulo é defeito **Crítico**, não detalhe de apresentação.

Simulação de provedor imita também o que dá errado — lentidão, resposta incompleta, preço inválido,
oferta duplicada, indisponibilidade. Um simulador que só sabe responder certo testa metade do
código e deixa a outra metade para produção.

## Testes de IA

Modelo de linguagem não é determinístico, então a avaliação é por conjunto dourado, não por
igualdade de string.

- `tests/conjunto-dourado/interpretacao-v0.json`: 42 casos pt/es, com data de referência por caso.
- O campo `esperado` é **a resposta certa**, não o comportamento atual. Casos fora do alcance do
  interpretador v0 ficam marcados; esse recorte é o mapa do que falta.
- Injeção de prompt tem teste próprio, e a primeira defesa não é teste: é o tipo de retorno selado,
  que não tem forma capaz de expressar "mude o plano" ou "mostre a conta de outro".
- Quando entrar o LLM: avaliar injeção **indireta** (texto vindo de página de provedor ou de
  mensagem de terceiro), vazamento de prompt e limite de ferramentas.

## Teste instável

**Teste instável é defeito, não inconveniente.** Não se marca como ignorado, não se "roda de novo".

Ao aparecer um: abra TASK, investigue a causa (quase sempre uma de três — tempo, ordem de execução
ou estado compartilhado) e corrija. Enquanto estiver aberto, o teste continua rodando e continua
vermelho. Desligar o alarme é como se perde a confiança na suíte inteira, e uma suíte em que
ninguém confia não impede defeito nenhum.

## Portão de qualidade

A classificação de severidade é a de `AGENTS.md` §8.

- **Crítico** e **Alto** bloqueiam o DONE. Sem exceção.
- **Médio** exige correção ou risco aceito **por escrito** pelo CTO, no DELIVERY REPORT.
- Risco que possa comprometer dado, dinheiro, segurança ou funcionamento vai para o CTO na hora em
  que é descoberto, não no relatório do fim.

Uma entrega não passa só porque compila. O que se exige:

1. cada requisito tocado tem linha na matriz de rastreabilidade com teste que existe;
2. os cenários obrigatórios aplicáveis à entrega estão cobertos e verdes;
3. nenhum achado Crítico ou Alto em aberto;
4. evidência reproduzível no DELIVERY REPORT — comando, saída, log.

## Cenários obrigatórios

A lista completa está em `agentes/07-qa-security-engineer.md`. A matriz
(`docs/architecture/matriz-de-rastreabilidade.md`) liga cada um ao requisito e ao teste, e a CI
falha se um status não tiver arquivo de teste que o sustente.

Resumo do que já tem prova automatizada hoje, no marco M1:

- última consulta Free disputada por requisições simultâneas (mecanismo);
- repetição da mesma mensagem não duplica efeito;
- evento do outbox não se perde nem duplica sob simultaneidade;
- texto de interface fora do dicionário quebra o build;
- acessibilidade das telas em pt e es, com e sem erro em tela;
- log sem dado sensível;
- requisição sem token válido é recusada na borda;
- limite de taxa na borda;
- interpretação de linguagem natural com conjunto dourado e guarda de injeção.

## Matriz de rastreabilidade

`tests/rastreabilidade/matriz.json` é a fonte; `docs/architecture/matriz-de-rastreabilidade.md` é
gerado dela.

```bash
bash scripts/rastreabilidade.sh              # verifica e regrava o documento
bash scripts/rastreabilidade.sh --conferir   # modo da CI: não escreve, falha se divergir
```

O verificador existe porque matriz escrita à mão envelhece em silêncio. Ele falha quando:

- um requisito do PRD não está na matriz, ou está com título/marco diferentes;
- a matriz cita um requisito que não existe no PRD;
- um status `COBERTO` ou `PARCIAL` não aponta nenhum teste;
- um caminho de teste citado não existe no repositório;
- o documento publicado está diferente do que a matriz geraria.

Requisito pendente com nota honesta vale mais que verde sem teste por trás. A contagem não é nota
de desempenho: no M1, a maior parte dos requisitos ainda não tem código.

## Como rodar

```bash
bash scripts/build.sh                      # compila e roda os testes Java (unitários e integração)
bash scripts/frontend-check.sh             # o que a CI roda no frontend
bash scripts/rastreabilidade.sh            # matriz
mvn -pl tests/concorrencia -am test        # só o harness
mvn -pl apps/platform-core -am verify      # inclui os três testes de concorrência (exige Docker)
```

Na CI, tudo isso roda por caminho alterado: cada serviço Java no seu job, o frontend no dele, e a
matriz no job base, que roda sempre.

## O que esta estratégia ainda não cobre

Honestidade sobre os buracos, para que ninguém os descubra achando que estavam cobertos:

- **E2E e carga não existem ainda.** `tests/e2e` e `tests/load` estão vazios; entram com as
  jornadas, a partir do M3. Carga precisa de ambiente parecido com produção, que é do M8.
- **Isolamento entre usuários (BOLA/IDOR) não tem teste** porque ainda não há usuário. É o primeiro
  teste de segurança a escrever no M2, e o mais importante deles.
- **Contraste de cor** não é verificado por máquina: o jsdom não calcula layout. Conferido à vista
  nos tokens.
- **Testes de recuperação** (worker reiniciado no meio do processamento, reserva órfã liberada)
  precisam de worker, que é do M2.
- **Varredura de imagem** (Trivy) entra com o primeiro Dockerfile.
