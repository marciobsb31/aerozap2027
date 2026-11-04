# Interpretação de linguagem natural

> Entrega da **AZ-019** (marco M1). Contexto: DTC §6 e §8.3, `agentes/03-ai-conversation-engineer.md`.

A pessoa escreve "quero ir de Brasília a Lisboa em 10/07 e voltar dia 20, 1 adulto". O
interpretador transforma isso em um [`SearchIntent`](../api-contracts/README.md) — ou numa
pergunta, ou numa recusa. Nada além disso.

## O retorno é a defesa

```java
sealed interface ResultadoDaInterpretacao {
    record Pesquisa(SearchIntent intencao, double confianca) { }
    record Pergunta(CampoFaltante campo, String pergunta, IntencaoParcial parcial) { }
    record Recusa(MotivoDaRecusa motivo, String resposta) { }
}
```

Essas três formas são a resposta estrutural ao risco de injeção de prompt (RF-046). Não existe
como o interpretador mandar mudar um plano, ler a conta de outra pessoa ou executar uma
ferramenta — **não há tipo que expresse isso**. Uma mensagem que peça essas coisas só pode virar
pesquisa, pergunta ou recusa.

Isso importa porque lista de frases proibidas é jogo de gato e rato: sempre há uma formulação
nova. Contrato de tipo não tem essa fraqueza. O `GuardaDeInjecao` existe como segunda linha — para
responder com clareza, não gastar consulta e registrar o evento de segurança —, não como a
proteção principal.

A mesma ideia vale para a porta do modelo de linguagem: `ProvedorDeLlm.completar` devolve **texto**.
A allowlist de ferramentas começa vazia porque não há superfície para ferramenta nenhuma. Um teste
de arquitetura falha o build se o pacote de interpretação passar a depender de rede, banco, fila
ou sistema de arquivos.

## Por que um interpretador sem IA

O v0 é regras, dicionário e expressões regulares. Três motivos para existir antes do modelo:

1. **Linha de base.** Sem ele, "a IA acerta 85%" não quer dizer nada. Com ele, quer.
2. **Plano B.** Provedor de IA fora do ar, caro ou lento é cenário previsto (RF-045); o canal
   continua funcionando para o pedido bem escrito.
3. **Desenho do contrato.** Obriga a decidir agora o que a interpretação pode devolver — e essa
   decisão é a que protege a plataforma, não o modelo escolhido.

## O que ele faz

- **Lugares**: dicionário de cidades e aeroportos em pt e es, mais códigos IATA. A preposição
  decide o papel — "de Brasília" é origem, "para Lisboa" é destino. Com dois lugares e um papel
  conhecido, o outro é o complementar. Sem pista nenhuma, vale a ordem, e a confiança cai.
- **Datas**: `10/07`, `2026-07-10`, "10 de julho", "amanhã", "próxima sexta", "semana que vem", e
  o "dia 20" solto, que se resolve pela data anterior — é assim que se diz a volta.
- **Passageiros**: adultos, crianças e bebês, por número ou por extenso; "somos 3"; padrão de um
  adulto.
- **Cabine**: econômica, premium, executiva, primeira.

Duas decisões que mudam o comportamento do produto:

**Ano omitido vira a próxima ocorrência.** "10/01" em junho é janeiro do ano que vem. Ninguém
compra passagem para o passado.

**Mês sem dia não é data.** "em julho" gera pergunta, nunca uma pesquisa com o dia 1 inventado.
Chutar aqui gastaria a consulta de alguém com uma resposta errada — e a franquia Free tem cinco.

## O que ele não faz

Não entende pedido vago ("algo barato para a Europa"), não usa contexto de mensagens anteriores,
não sabe que réveillon é 31/12 e não conhece cidade fora do dicionário. **Isso está medido**, não
suposto: são os casos marcados `alcanceV0: false` no conjunto dourado.

## Conjunto dourado

`tests/conjunto-dourado/interpretacao-v0.json` — 42 casos em português e espanhol, cobrindo
sucesso, ambiguidade, refinamento, datas relativas e injeção. É **um gabarito só**, usado pelo
motor de regras hoje e pelo de linguagem no M4: é isso que torna a troca uma decisão com número.

Cada caso traz a data de referência (`hoje`), para que expressão relativa seja testável, e o
`esperado` é **a resposta certa** — não o que o v0 faz. Daí os dois recortes:

| Recorte | Casos | Acerto do v0 |
|---|---:|---:|
| No alcance do v0 | 36 | **100%** |
| Além do v0 (lacuna para a IA) | 6 | 17% |
| Total | 42 | 88% |

Os 36 primeiros são contrato: se um deixar de passar, o build quebra. Os outros 6 medem o que o
modelo precisa ganhar. A medição é regravada a cada execução em
`apps/conversation-ai/target/acuracia-conjunto-dourado.md`.

## Confiança

O resultado de pesquisa carrega uma confiança de 0 a 1. Ela cai quando origem e destino vieram só
da ordem das palavras, sem preposição que confirme — é onde o v0 mais erra. Abaixo do limiar
(`aerozap.interpretacao.limiar-de-confianca`, padrão 0,80), o canal confirma a leitura com a
pessoa antes de gastar a consulta (RF-044).

## Custo da IA, desde o primeiro dia

`UsoDeLlm` registra modelo, versão do prompt, tokens de entrada e saída, latência e custo, e
`RegistroDeUsoDeLlm` publica isso como métrica por modelo **e por versão de prompt**. É o que
permite responder "o prompt novo ficou mais caro?" com número (RF-049) — custo de IA é risco
declarado do projeto.

No pedido ao modelo, a instrução do sistema e o texto da pessoa são campos separados. Concatenar
os dois numa string só é o que torna injeção trivial.

## Como verificar

```bash
bash scripts/build.sh
```

O conjunto dourado roda junto e imprime a tabela de acerto.
