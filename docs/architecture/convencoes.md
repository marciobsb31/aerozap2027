# Convenções do repositório (AZ-010)

**Base:** ADR-0001 e `AGENTS.md` §5–6. **Remoto:** `https://github.com/marciobsb31/aerozap2027`.

## Branches
| Branch | Uso |
|---|---|
| `main` | Código estável e liberável. Protegida: só recebe PR |
| `develop` | Integração das entregas em andamento |
| `feature/AZ-xxx-resumo` | Trabalho de uma tarefa |
| `bugfix/AZ-xxx-resumo` | Correção de defeito encontrado antes de `main` |
| `hotfix/resumo` | Correção urgente a partir de `main` |

Uma tarefa, uma branch. Nada vai direto para `main` ou `develop`.

## Commits
Formato: `AZ-xxx tipo(escopo): resumo no imperativo`, até 72 caracteres.
Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`, `build`, `perf`.
Exemplo: `AZ-010 docs(repo): cria estrutura inicial e README`.
Para usar o modelo local: `git config commit.template .gitmessage`.

## Pull requests
- Obrigatório, com o template de `.github/pull_request_template.md`.
- Revisão por quem não escreveu o código; validação do A7 (QA e Segurança) e aprovação do A0 (CTO), conforme a TASK.
- Pipeline verde antes do merge (AZ-012).

## Segredos
- Nunca versionar `.env`, chaves ou tokens. Use `.env.example` apenas com nomes e valores fictícios.
- Se um segredo vazar, revogue-o imediatamente e troque por um novo; remover do histórico não basta.

## Logs
- Log registra **identificador, não dado de pessoa**: `conversation_id` em lugar do telefone,
  `user_id` em lugar do e-mail. Vale para mensagem, MDC e exceção.
- A biblioteca `observability-starter` mascara automaticamente telefone, e-mail, CPF, CNPJ,
  cartão e credenciais em qualquer saída (`%msgSeguro`). Isso é rede de segurança, não permissão:
  escrever o dado e confiar na máscara é erro de revisão.
- Toda requisição carrega o identificador de correlação (`X-Correlation-Id`); use-o para
  amarrar log, métrica e rastro. Detalhes em [observabilidade](observabilidade.md).

## Pipelines por diretório
`apps/*`, `frontend/` e `libs/*` têm pipelines independentes (ADR-0001), criadas na AZ-012
(ver [ci.md](ci.md)).

## Estados de tarefa
`BACKLOG → READY → IN_PROGRESS → BLOCKED → REVIEW → REWORK → DONE`. Máximo de 2 tarefas `IN_PROGRESS` por agente.
