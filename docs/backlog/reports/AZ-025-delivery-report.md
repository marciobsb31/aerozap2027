# DELIVERY REPORT

Task: AZ-025

Agent: A7 (QA e Segurança), com A1 nas correções e A0 na revisão

Branch / PR: `feature/AZ-025-isolamento-e-virada` → `develop`

Status: READY_FOR_REVIEW

## Implementado

Os cenários obrigatórios do M2 que faltavam, e as correções que eles exigiram.

| Cenário | Onde |
| --- | --- |
| Isolamento com dois usuários reais pela borda real | `scripts/dev-isolamento-check.sh` |
| Cabeçalho de identidade forjado, rotas internas, entradas sem token | `EncaminhamentoIntegrationTest` (gateway) |
| Perfil, saldo, idempotência e sessões de pessoas diferentes | `IsolamentoEntreUsuariosIntegrationTest` |
| Virada do trimestre com o relógio andando, inclusive com corrida | `ViradaDePeriodoIntegrationTest` |
| Reserva órfã devolvida pela passagem do tempo | `ViradaDePeriodoIntegrationTest` |
| Refresh token reutilizado, de ponta a ponta | `scripts/dev-isolamento-check.sh` |
| Token expirado, adulterado, de outro emissor ou público | `SegurancaIntegrationTest` (já existia) |

## Achados

| Severidade | Achado | Situação |
| --- | --- | --- |
| **Crítico (latente)** | O gateway repassava `X-Usuario-Id` vindo do cliente e nunca o preenchia a partir do token. Só não era explorável porque o defeito seguinte impedia o acesso | Corrigido; provado antes e depois |
| **Alto** | Cadastro e login não funcionavam pela borda: o gateway exigia token nessas rotas, e o caminho `/v1/auth` não era alcançável a partir de `/api` | Corrigido; provado de ponta a ponta |
| Médio | O `platform-core` confia no cabeçalho `X-Usuario-Id` e `/internal/usage` não tem autenticação. Quem alcançar a porta do serviço diretamente passa por cima da borda | **Em aberto**, ver Ressalvas |
| Baixo | A matriz dizia que o cadastro funcionava de ponta a ponta, o que não era verdade | Corrigido |

O teste do gateway foi escrito antes da correção e reprovou contra o código antigo: o serviço recebia
`usuario=null`.

## Decisões

**Só a borda escreve `X-Usuario-Id`.** Qualquer valor vindo do cliente é descartado, em qualquer
capitalização, e o valor vem do `sub` do token validado.

**A identidade foi para `/api/v1/auth` e o saldo do usuário ganhou `/api/v1/usage/saldo`.** Reservar,
confirmar e liberar continuam em `/internal/usage`: o gateway só encaminha o que está sob `/api`.

**Quatro entradas abertas, só por POST:** registrar, entrar, renovar e sair. Renovar precisa ser aberto
porque quem renova está, em geral, com o token de acesso vencido. O cliente não deve enviar o token
vencido nessa chamada, porque a borda recusaria a requisição. A AZ-024 precisa seguir isso.

**Relógio injetável na franquia.** As datas dos testes ficam em 2100 porque o banco é compartilhado:
uma reserva com prazo distante nunca é apanhada pelo expirador de outro contexto de teste.

## Como verificar

```bash
bash scripts/build.sh                    # 245 testes; exige Docker
bash scripts/dev-isolamento-check.sh     # dois usuários reais pela borda; log em logs/dev-isolamento-check.log
```

## Evidências

- **Build completo com Docker: BUILD SUCCESS, 245 testes, zero falhas.**
- `dev-isolamento-check.sh`: 20 verificações aprovadas, incluindo cabeçalho forjado em duas grafias,
  identificador na query, token adulterado, rotas internas por quatro caminhos e reuso de refresh.

### Critérios de aceite

- [x] Cenários obrigatórios do M2 cobertos e verdes na CI (confirmar no PR)
- [x] Nenhuma rota devolve dado de outro usuário, provado com dois usuários de verdade
- [x] Refresh token reutilizado invalida a família de sessões, provado
- [x] Erro de provedor e falha interna não consomem franquia, provado (`FranquiaIntegrationTest`)
- [x] Nenhum achado Crítico ou Alto em aberto
- [ ] Matriz atualizada: os requisitos do M2 saem de PENDENTE. Os que dependem de AZ-020 (recuperação), AZ-021, AZ-023 e AZ-024 continuam PENDENTE até essas tarefas
- [x] Ambiente rodando e verificável, com comando documentado

## Ressalvas

- **Acesso direto ao serviço.** A segurança depende de o `platform-core` só ser alcançável pela borda. Em
  desenvolvimento a porta 8081 está aberta na máquina, e por isso o achado é Médio hoje. **Precisa ser
  resolvido antes de qualquer ambiente exposto**, por isolamento de rede e, de preferência, com o serviço
  validando o token também. Fica como condição da AZ-083.
- **Janela móvel.** A virada foi provada no trimestre-calendário, que é o padrão. A janela móvel continua
  testada só nas bordas, porque precisa da data de cadastro, que o módulo da franquia ainda não recebe.
- **Histórico.** Não há histórico de pesquisas para testar isolamento: entra no M3, e o teste precisa ir junto.
- **Contrato.** As rotas de identidade e saldo ainda não estão no OpenAPI. A AZ-024 precisa delas para gerar o
  `api-client`.
- **E2E fora da CI.** `dev-isolamento-check.sh` roda na máquina do desenvolvedor. Na CI, a mesma regra está
  coberta em duas metades: o gateway contra um serviço de teste e o `platform-core` com o cabeçalho já definido.

## Próximos passos

- AZ-024, começando pelo contrato OpenAPI de identidade e saldo.
- AZ-020: recuperação de acesso, que depende de um canal de e-mail.
- AZ-021 e AZ-023.
