# Erros

Todo erro da plataforma sai em **Problem Details (RFC 9457)**, com `Content-Type:
application/problem+json`. Nunca no envelope de sucesso: um cliente não deveria precisar ler o
corpo para descobrir se deu certo.

```json
{
  "type": "urn:aerozap:problem:usage-exhausted",
  "title": "Franquia esgotada",
  "status": 409,
  "detail": "Sua franquia do trimestre acabou. Ela renova em 01/10/2026.",
  "instance": "/api/plataforma/v1/searches",
  "correlationId": "7b3a1c90-0f2e-4c6b-9d41-5a2f8e3b6c11"
}
```

**`type` é para a máquina, `detail` é para a pessoa.** O cliente decide o que fazer pelo `type`:
mostrar tela de assinatura, pedir login de novo, tentar mais tarde. Texto muda com tradução e com
revisão de redação; tipo não muda nunca. Por isso o `type` é contrato — só se acrescenta.

**`correlationId` vai em todo erro**, inclusive nos que o próprio framework monta (404, 405, 400).
É o que transforma "deu erro aqui" em um log encontrável.

**Nada de detalhe interno no `detail`.** Sem stack trace, sem nome de tabela, sem mensagem de
driver. O que o suporte precisa está no log, achável pelo `correlationId`.

## Catálogo

| `type` (sem o prefixo `urn:aerozap:problem:`) | HTTP | Quando | O que o cliente faz |
|---|:--:|---|---|
| `validation` | 400 | campo faltando, formato errado, regra de domínio violada | corrigir a entrada e repetir |
| `unauthorized` | 401 | sem token válido | autenticar de novo |
| `forbidden` | 403 | token válido, sem permissão | não repetir |
| `not-found` | 404 | não existe, ou não é de quem pediu | não repetir |
| `conflict` | 409 | o estado atual não comporta a operação | reler o recurso |
| `rate-limit` | 429 | limite de taxa da borda | esperar o `Retry-After` |
| `usage-exhausted` | 409 | franquia de consultas esgotada no período | oferecer assinatura; mostrar a data de renovação |
| `provider-unavailable` | 503 | nenhum provedor de voos respondeu | tentar mais tarde; **não** consome franquia |
| `gateway-timeout` | 504 | o serviço de destino não respondeu a tempo | tentar mais tarde |
| `internal-error` | 500 | falha não prevista | tentar mais tarde; registrar o `correlationId` |

A lista vive em código em `br.com.aerozap.contracts.ProblemTypes`: tipo novo entra lá e aqui, no
mesmo commit.

## Dois erros que parecem o mesmo e não são

`rate-limit` e `usage-exhausted` ambos dizem "não agora", mas significam coisas opostas para o
usuário e para o negócio:

- **`rate-limit`** é proteção de infraestrutura, por origem de rede, na borda. Passa em segundos.
  Não tem nada a ver com plano nem com saldo.
- **`usage-exhausted`** é a franquia do plano, por usuário, no período. Só volta na renovação —
  ou assinando o Premium.

Tratar os dois com a mesma mensagem seria dizer a um usuário pagante que ele acabou o saldo, ou
oferecer assinatura a quem só apertou o botão rápido demais.
