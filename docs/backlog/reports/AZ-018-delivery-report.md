# DELIVERY REPORT

Task: AZ-018

Agent: A7 (QA & Security Engineer), com A0 (CTO) na revisão

Branch / PR: `feature/AZ-018-estrategia-de-testes` → `develop`

Status: READY_FOR_REVIEW

## Implementado

**Estratégia de testes** (`docs/architecture/estrategia-de-testes.md`). Organizada em torno das
três classes de defeito que o caminho feliz não encontra e que custam caro neste produto
especificamente: corrida (franquia, reentrega de mensagem, webhook repetido), isolamento entre
usuários e dado inventado. Define camadas, dados sintéticos, política de provedores simulados,
avaliação de IA, tratamento de teste instável e o portão de qualidade. Termina com a lista do que
**ainda não** cobre — porque buraco conhecido é risco administrado, e buraco que ninguém escreveu é
surpresa.

**Harness de concorrência** (`tests/concorrencia`, módulo Maven próprio). `ArenaDeConcorrencia`
segura N tarefas numa linha de largada comum e solta todas juntas. O motivo é concreto: um laço que
cria N tarefas e sai executando quase nunca coloca duas dentro da seção crítica ao mesmo tempo — a
primeira termina antes de a última nascer, o teste fica verde e a corrida aparece em produção. O
harness coleta fatos (sucessos, falhas, tipo do erro, duração) e não afirma nada: a invariante é de
quem escreve o teste. Thread virtual por tarefa, para que mil operações simultâneas não virem
exercício de configurar pool. `repetir(...)` roda a arena várias vezes com preparação entre elas,
porque corrida é probabilística e uma execução só diz "passou" quase sempre.

**Três invariantes provadas contra o esqueleto Java com PostgreSQL real** (Testcontainers):

| Teste | Invariante |
| --- | --- |
| `ReservaDeFranquiaConcorrenciaIntegrationTest` | 32 requisições simultâneas pela última consulta Free → exatamente uma autorizada, saldo nunca negativo; e com saldo 5, exatamente 5 autorizações |
| `OutboxConcorrenciaIntegrationTest` | o mesmo evento gravado 24 vezes ao mesmo tempo vira uma linha; 24 eventos distintos viram 24 linhas, nenhuma perdida |
| `ConsumidorIdempotenteConcorrenciaIntegrationTest` | 24 entregas simultâneas do mesmo evento executam o efeito uma vez; consumidores diferentes têm cada um a sua |

O teste da franquia inclui o **contraexemplo**: a mesma disputa resolvida com "ler, decidir na
aplicação, gravar" autoriza mais consultas do que o saldo tinha. Sem ele não há prova de que é o
`UPDATE` condicional que está segurando a invariante — só a afirmação de que está.

**Dados sintéticos** (`DadosSinteticos`). Gerar nome e telefone ao acaso não cumpre a regra de
"nunca dados reais": um telefone plausível gerado ao acaso é o telefone de alguém. Cada tipo usa
faixa reservada pelo mundo para exemplo — e-mail em `.invalid` (RFC 6761), telefone com código de
país `+999` (ITU-T E.164), CPF com dígito verificador errado de propósito. Determinístico a partir
da semente, porque teste com entrada variável falha de forma irreproduzível e falha irreproduzível
vira ruído tolerado.

**Matriz de rastreabilidade verificada por máquina.** `tests/rastreabilidade/matriz.json` é a
fonte; `docs/architecture/matriz-de-rastreabilidade.md` é gerado dela. O verificador
(`scripts/rastreabilidade.sh`, no job base da CI) falha quando um requisito do PRD não está na
matriz, quando a matriz cita requisito inexistente, quando título ou marco divergem do PRD, quando
um status `COBERTO`/`PARCIAL` não aponta teste, quando um caminho de teste citado não existe no
disco, ou quando o documento publicado está desatualizado.

Matriz escrita à mão envelhece em silêncio — o requisito entra no PRD e ninguém acrescenta a linha,
o teste é renomeado e a evidência vira caminho morto, alguém marca verde sem teste. As três viraram
falha de CI.

## Números da matriz

104 requisitos do PRD: **3 cobertos, 20 parciais, 81 pendentes**.

Isso não é nota de desempenho e não deve ser lido como tal: o produto está no marco M1 e a maior
parte dos requisitos ainda não tem código. O que a matriz garante é que nenhum requisito some do
radar e que nenhum status exista sem arquivo de teste que o sustente. Um pendente com nota honesta
vale mais que um verde que não corresponde a teste nenhum.

## Decisões

**Harness em módulo próprio, sob `tests/`.** Fica no diretório de propriedade de A7, tem build e
teste próprios, e os serviços o declaram com escopo de teste — não entra em artefato de produção.
Depende só da biblioteca padrão: ferramenta de teste pesada ninguém usa.

**A invariante da franquia foi escrita antes da implementação.** O serviço é do marco M2. Escrever
agora é deliberado: é a classe de defeito que não aparece em teste sequencial nem em revisão de
código, e cobra caro em produção — duas consultas debitadas de um saldo de uma. Hoje a tabela é
criada pelo próprio teste, e isso está dito na primeira linha da classe. Quando o serviço existir,
o teste passa a chamá-lo; a invariante não muda.

**Pausa no contraexemplo, e só nele.** O padrão "ler e depois gravar" tem uma janela de
microssegundos: a corrida aconteceria de vez em quando, e um teste que falha de vez em quando é
teste instável — que esta mesma estratégia classifica como defeito. A pausa de 100 ms escancara a
janela e torna o defeito determinístico. Ela não inventa o problema: a janela existe com ou sem ela.

**Testes de concorrência moram em `apps/platform-core`**, e não em `tests/`, porque precisam do
esquema que o Flyway daquele serviço cria e do contexto Spring que expõe `OutboxService` e
`IdempotentConsumer`. São fontes de **teste**; nenhuma linha de produção do serviço mudou. Exceção
ao limite de módulos da TASK, autorizada por A0 e registrada aqui.

**Matriz em JSON, documento gerado.** Markdown à mão com 104 linhas não sobrevive a seis meses de
mudança de requisito. O JSON é diffável, verificável e pequeno; o documento é a leitura.

## Como verificar

```bash
mvn -pl tests/concorrencia -am test        # o harness e seus próprios testes
mvn -pl apps/platform-core -am verify      # os três casos de concorrência (exige Docker)
bash scripts/rastreabilidade.sh            # matriz: verifica e regrava o documento
bash scripts/build.sh                      # todos os módulos Java
```

## Evidências

**Build completo offline**, `mvn test` nos sete módulos (JDK 21 com `-Djava.version=21`; a CI usa
Temurin 25): **BUILD SUCCESS**, 156 testes fora dos de integração, nenhuma falha.

| Módulo | Testes |
| --- | --- |
| `event-contracts` | 81 |
| `service-foundation` | 5 |
| `observability-starter` | 21 |
| `harness-de-concorrencia` | **13** (novos) |
| `platform-core` | 3 |
| `api-gateway` | 8 |
| `conversation-ai` | 25 |

Dos 13 novos, o mais importante é `criaDisputaReal`: ele prova que a arena **de fato** sobrepõe as
tarefas, usando um contador propositalmente desprotegido que só perde incrementos sob
simultaneidade. Sem esse teste do teste, o harness poderia serializar as tarefas e devolver verde
sem ter testado corrida nenhuma.

**Matriz**: `Matriz íntegra: 104 requisitos — 3 cobertos, 20 parciais, 81 pendentes.` O verificador
foi exercitado contra um estado inválido durante o desenvolvimento e apontou corretamente os cinco
caminhos de teste que não existiam.

**Lint de shell**: `shellcheck` limpo em `scripts/rastreabilidade.sh`.

### Critérios de aceite

- [x] **Matriz cobre os cenários obrigatórios de `agentes/07`** — cada cenário aplicável está ligado
      ao requisito e ao teste na matriz; o que ainda não tem código aparece como pendente com o
      marco em que entra, e o verificador impede que um status fique sem teste por trás.
- [x] **Harness executa no CI contra o esqueleto de AZ-014** — os três testes rodam no job
      `platform-core` (`mvn -pl apps/platform-core -am verify`), que já usa Testcontainers no
      runner. O filtro de caminhos passou a incluir `tests/concorrencia/**`, para que mexer no
      harness dispare o job que o executa.
- [x] **Política de testes com provedores simulados documentada** — seção "Provedores externos":
      simulado ou sandbox em todo teste automatizado, nunca carga contra provedor real, chamada real
      só com credencial de teste autorizada e registrada no relatório, e o rótulo `demo` obrigatório
      no contrato chegando até a interface.
- [x] **Ambiente rodando e verificável, com comando documentado** — comandos acima, e
      `bash scripts/rastreabilidade.sh` roda sem Docker e sem Java.

## Ressalvas

- **Os três testes de concorrência não foram executados por mim.** Eles compilam e estão ligados ao
  job que os roda, mas precisam de Docker, que o ambiente do agente não tem. A execução acontece na
  CI e na máquina do PO (`mvn -pl apps/platform-core -am verify`). Até sair verde lá, a prova é de
  que o código está correto por construção, não de que passou — e essa distinção importa.
- **`tests/e2e`, `tests/load` e `tests/security` continuam vazios.** E2E depende de jornada (M3);
  carga, de ambiente parecido com produção (M8); segurança, de usuário (M2). Estão nomeados na
  estratégia com o marco de cada um.
- **BOLA/IDOR não tem teste** porque ainda não existe usuário. É o primeiro teste de segurança do
  M2 e o mais importante deles.
- **A matriz liga requisito a arquivo de teste, não a método.** Granularidade por método seria mais
  precisa e quebraria a cada refatoração; a escolha foi por algo que sobreviva.

## Próximos passos

- M2: ao implementar a franquia, apontar `ReservaDeFranquiaConcorrenciaIntegrationTest` para o
  serviço real e escrever os cenários de recuperação (worker reiniciado, reserva órfã).
- M2: primeiro teste de isolamento entre usuários.
- M3: começar `tests/e2e` com a jornada J1 e atualizar a matriz na mesma TASK.
