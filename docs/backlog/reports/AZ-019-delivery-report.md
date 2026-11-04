# DELIVERY REPORT

Task: AZ-019

Agent: A3 (IA e Conversa), com A0 (CTO) na revisão

Branch / PR: `feature/AZ-019-interpretador` → `develop`

Status: READY_FOR_REVIEW

## Implementado

**Contrato da interpretação.** `ResultadoDaInterpretacao` é uma interface selada com exatamente
três formas: `Pesquisa`, `Pergunta`, `Recusa`. Essa é a resposta estrutural à injeção de prompt
(RF-046) — não existe tipo que expresse "mude o plano" ou "mostre a conta de outro", então
nenhuma frase consegue pedir isso, por melhor que seja formulada.

**Interpretador v0 determinístico** (`deterministico-v0`): dicionário de cidades e aeroportos pt/es,
papéis por preposição ("de Brasília" é origem, "para Lisboa" é destino), complemento por dedução
quando só um papel aparece, datas absolutas e relativas (inclusive o "dia 20" solto, que se
resolve pela data anterior), passageiros por número e por extenso, cabine. Java puro, sem Spring:
testável e trocável sem subir o serviço.

**Duas regras de produto que viraram código**
- Ano omitido vira a próxima ocorrência: "10/01" em junho é janeiro do ano seguinte.
- **Mês sem dia não é data**: "em julho" gera pergunta, nunca pesquisa com dia 1 inventado — o
  critério de aceite "datas incompletas geram pergunta" sai daí.

**Guarda de injeção** como segunda linha: reconhece a tentativa, responde com clareza, não gasta
consulta e registra evento de segurança com o nome do padrão — nunca o texto recebido.

**Porta de LLM** (`ProvedorDeLlm`): devolve **texto**, e só. A allowlist de ferramentas começa
vazia porque não há superfície para ferramenta. `PedidoAoLlm` separa instrução do sistema e texto
da pessoa em campos distintos — concatenar os dois é o que torna injeção trivial. `UsoDeLlm` e
`RegistroDeUsoDeLlm` registram modelo, versão do prompt, tokens, latência e custo como métrica
por modelo e por versão (RF-049).

**Conjunto dourado**: `tests/conjunto-dourado/interpretacao-v0.json`, 42 casos pt/es nas cinco
categorias pedidas, com data de referência por caso. O `esperado` é **a resposta certa**, não o
comportamento do v0 — por isso há dois recortes, e o segundo é o mapa do que a IA precisa ganhar.

**Serviço `apps/conversation-ai`**: módulo novo, com Actuator, observabilidade e o interpretador.
Endpoints e contexto de conversa chegam no M4.

## Arquivos alterados

- `apps/conversation-ai/` (módulo novo: 20 classes, 4 de teste, `application.yml`, logback)
- `tests/conjunto-dourado/interpretacao-v0.json`
- `pom.xml` (módulo novo)
- `docs/architecture/interpretacao.md`, `README.md`, `docs/backlog/`

## Contratos

Nenhum contrato compartilhado mudou. O interpretador **consome** `SearchIntent` da AZ-017 sem
alterá-lo — e foi preciso criar `IntencaoParcial` justamente para não afrouxar o contrato
compartilhado só para caber um rascunho de pesquisa.

## Testes

- Unitários: **PASS** — 25 testes no módulo (extração de datas, guarda de injeção com casos de
  bloqueio **e de falso positivo**, arquitetura).
- Conjunto dourado: **PASS** — 36/36 dos casos no alcance do v0.
- Arquitetura: **PASS** — o pacote de interpretação não alcança rede, banco, fila nem sistema de
  arquivos; é a allowlist de ferramentas virada teste.
- Integração: N/A (sem endpoints no v0).

**Acerto medido** (`apps/conversation-ai/target/acuracia-conjunto-dourado.md`):

| Recorte | Casos | Acertos | Acerto |
|---|---:|---:|---:|
| No alcance do v0 | 36 | 36 | **100%** |
| Além do v0 (lacuna para a IA) | 6 | 1 | 17% |
| Total | 42 | 37 | 88% |
| pt-BR | 30 | 26 | 87% |
| es | 12 | 11 | 92% |
| sucesso | 16 | 14 | 88% |
| ambiguidade | 9 | 8 | 89% |
| datas relativas | 7 | 7 | 100% |
| injeção | 6 | 6 | **100%** |
| refinamento | 2 | 0 | 0% |

Os 17% na segunda linha não são defeito: são a medida do que o motor de linguagem precisa
entregar no M4 — refinamento com contexto, destino por região, data por evento cultural e a cauda
longa de aeroportos.

**Execução:** `BUILD SUCCESS` nos sete módulos, 143 testes sem Docker, no ambiente do agente
(Maven offline).

## Como verificar

```bash
bash scripts/build.sh
```

O conjunto dourado roda junto e imprime a tabela de acerto. Leitura:
`docs/architecture/interpretacao.md`.

O ambiente está rodando: **não se aplica** — o v0 não expõe endpoint; a verificação é o build.

## Evidências

- `BUILD SUCCESS`, 25 testes no módulo e 143 no total
- `apps/conversation-ai/target/acuracia-conjunto-dourado.md` (regravado a cada execução)
- `tests/conjunto-dourado/interpretacao-v0.json`

## Dependências

- AZ-017 (contratos) — atendida.
- Desbloqueia AZ-038 (pesquisa por linguagem natural na Web) e AZ-042 (contexto de conversa).

## Riscos

- **Dicionário de cidades é curto.** ~70 entradas cobrem as rotas prováveis do começo; a cauda
  longa fica para a IA ou para a base do provedor. Está medido no conjunto dourado (`pt-alem-05`).
- **Regras erram em pedido mal escrito.** Sem preposição, origem e destino saem da ordem das
  palavras — por isso a confiança cai nesse caso e o canal confirma antes de gastar a consulta.
- **Guarda de injeção é lista de padrões**, e lista sempre tem buraco. A proteção que importa é o
  tipo de retorno; a lista só melhora a resposta. Os testes cobrem falso positivo, que é o erro
  mais caro para quem usa.
- **O conjunto dourado é nosso.** Escrito por quem fez o interpretador, com o viés que isso
  carrega. Mitigação parcial: os casos "além do v0" foram escritos como a resposta certa, não
  como o que a implementação faz. Casos vindos de uso real entram a partir do M4.

## Decisões necessárias

Para o M4, com o Product Owner: provedor de LLM e teto de custo por interação; limiar de confiança
definitivo (hoje 0,80); e se a confirmação em baixa confiança é automática ou só no WhatsApp.

## Checklist de pronto

- [x] Critérios de aceite atendidos
- [ ] Testes passando em CI (depende do push e da fila do GitHub Actions)
- [x] Documentação atualizada
- [x] Sem credenciais, sem dados reais de usuários
- [x] Dados de demonstração rotulados (não há)
- [x] Logs sem dados sensíveis — o evento de segurança registra o padrão, nunca o texto recebido
- [x] Plano de reversão: remover `apps/conversation-ai` dos módulos do POM pai; nada depende dele
