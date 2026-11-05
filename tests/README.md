# tests

Testes e ferramentas de teste que não pertencem a um serviço só.

| Diretório | O que é |
| --- | --- |
| `concorrencia/` | módulo Maven com o harness `ArenaDeConcorrencia` e o gerador `DadosSinteticos`. Escopo de teste: não vai para produção. |
| `rastreabilidade/` | `matriz.json`, a fonte da matriz requisito → teste → evidência |
| `conjunto-dourado/` | casos de referência da interpretação de linguagem natural (pt/es) |
| `e2e/` | jornadas completas — vazio até o marco M3 |
| `load/` | carga e stress — vazio até o marco M8 |
| `security/` | testes de segurança transversais — vazio até haver usuário (M2) |

A estratégia que rege tudo isso está em `docs/architecture/estrategia-de-testes.md`.

## Harness de concorrência

```java
var resultado = ArenaDeConcorrencia.disparar(32, indice -> reservarConsulta(usuario));

assertThat(resultado.totalDeFalhas()).isZero();
assertThat(resultado.sucessosQue(Boolean.TRUE::equals)).isEqualTo(1);
```

Todas as tarefas esperam uma linha de largada comum. Sem isso, um laço que cria N tarefas quase
nunca coloca duas dentro da seção crítica ao mesmo tempo — e o teste fica verde justamente onde a
corrida existe.

```bash
mvn -pl tests/concorrencia -am test     # o harness e seus próprios testes
mvn -pl apps/platform-core -am verify   # os três casos de concorrência (exige Docker)
```

## Matriz de rastreabilidade

```bash
bash scripts/rastreabilidade.sh              # verifica e regrava o documento
bash scripts/rastreabilidade.sh --conferir   # modo da CI
```
