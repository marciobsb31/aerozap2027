# DELIVERY REPORT

Task: AZ-022

Agent: A1 (Backend/Plataforma), com A0 (CTO) na revisão

Branch / PR: `feature/AZ-022-franquia` → `develop`

Status: READY_FOR_REVIEW

## Implementado

**Módulo da franquia** em `apps/platform-core`, pacote `franquia`, schema `franquia` (ADR-0002:
schema próprio por módulo). Reserva → confirmação ou liberação, exatamente como a ADR-0004 desenhou.

A regra de negócio em uma frase: **nada é debitado quando a pesquisa termina — é debitado quando
ela começa, e devolvido se ela não der certo.** O contrário permite que N pesquisas simultâneas
comecem com saldo 1, que é o cenário obrigatório do marco.

**Três invariantes, e nenhuma delas mora em código Java.** Todas são condição de `UPDATE` no banco,
porque é o único lugar onde "verificar e gravar" acontece de uma vez só:

| Invariante | Onde |
| --- | --- |
| O saldo nunca fica negativo nem passa da franquia | `update ... set consumidas = consumidas + 1 where consumidas < franquia`, mais uma `check` como rede |
| Uma reserva só é fechada uma vez | `update ... where id = :id and estado = 'RESERVED'` |
| A mesma chave nunca debita duas vezes | índice único `(usuario_id, chave_idempotencia)` |

**Expiração de reserva órfã.** Worker derrubado no meio da pesquisa deixa a consulta debitada e
ninguém para confirmar ou liberar. Sem isso ela ficaria presa até a virada do período, e a pessoa
não teria como saber disso nem como reclamar com precisão — por isso o conserto é automático, e não
um procedimento de suporte.

**Período de uso** nas duas formas que o PRD deixou em aberto (RN-02): trimestre-calendário (padrão)
e janela móvel contada do cadastro. A escolha é configuração. Início inclusivo e fim exclusivo, para
que entre dois períodos não haja nem buraco nem sobreposição.

**Eventos** `usage.reserved`, `usage.confirmed` e `usage.released` publicados pelo Outbox, na mesma
transação da mudança: ou os dois existem, ou nenhum. Os contratos já vinham prontos da AZ-017 e não
precisaram de uma linha de mudança — que é o teste real de um contrato bem escrito.

## Correção de um desvio meu

A ADR-0002 coloca **Identity dentro do `platform-core`**, com schema e pacote próprios, e define
critérios para separar um módulo em deployable: escala independente comprovada por métrica,
isolamento de falha ou de cota, ciclo de release distinto, requisito de segurança diferente.

Na AZ-020 eu criei `apps/identity` como sexto deployable. **Nenhum daqueles critérios estava
atendido** — foi inércia de quem estava escrevendo um serviço novo, não decisão. Antes de seguir
para a franquia, desfiz: o código virou o pacote `platformcore.identidade`, com schema `identidade`,
o módulo Maven e o job de CI saíram, e as consultas passaram a nomear o schema explicitamente
(`identidade.usuario`), o que deixa visível na revisão qualquer módulo tocando a tabela de outro.

Registro aqui em vez de abrir nota na ADR porque o desvio não chegou a valer: foi corrigido antes
de qualquer merge. Nota de ADR é para decisão que muda, não para erro que se conserta.

## Decisões

**Débito na reserva, não na conclusão.** É o que a ADR-0004 manda e é o que torna o cenário da
última consulta solúvel.

**Franquia esgotada é erro `409`, não resposta de sucesso com saldo zero.** Assim quem chama não
precisa inspecionar o corpo de uma resposta de sucesso para descobrir que não pode pesquisar. O
tipo `urn:aerozap:problem:usage-exhausted` já existia no catálogo desde a AZ-017.

**O contador fica no período, não numa contagem de linhas de reserva.** Contar linhas e decidir na
aplicação reabriria exatamente a janela que a reserva existe para fechar.

**Crédito só depois de fechar a reserva.** Creditar antes permitiria duas liberações concorrentes
devolverem duas consultas de uma reserva só. Há teste com 32 liberações simultâneas.

**Quem perde a corrida pela mesma chave tem a transação desfeita pelo índice único** — inclusive o
débito — e lê a reserva vencedora **fora** daquela transação, porque depois de uma violação de
restrição o PostgreSQL não aceita mais nada na mesma transação.

**A janela móvel está implementada mas não selecionada.** Ela precisa da data de cadastro, e o
módulo da franquia não lê a tabela da identidade (ADR-0002). Enquanto a API interna da identidade
não existir, o padrão é o trimestre-calendário, que ignora esse valor. Está escrito no código, no
ponto exato onde faltaria.

## Como verificar

```bash
mvn -pl apps/platform-core -am verify     # exige Docker (Testcontainers)
```

## Evidências

**Build offline dos seis módulos: BUILD SUCCESS**, 174 testes fora dos de integração.
`platform-core` passou de 3 para 21 testes nesta tarefa e na AZ-020.

Testes de integração escritos (executam na CI, que tem Docker):

| Classe | O que prova |
| --- | --- |
| `FranquiaIntegrationTest` (9) | primeira reserva cria período; idempotência; confirmar consome; falha devolve; liberação dupla devolve uma; confirmar depois de liberar não derruba o saldo; sexta pesquisa recusada com o tipo certo; recusa não deixa rastro; saldo é por usuário |
| `FranquiaConcorrenciaIntegrationTest` (4) | 32 simultâneas com saldo 5 → 5 reservas; última consulta disputada → uma, em 3 rodadas; 32 chamadas com a mesma chave → uma reserva; 32 liberações → uma consulta de volta |
| `ExpiracaoDeReservasIntegrationTest` (3) | reserva órfã volta marcada `EXPIRED`; rodar duas vezes não credita duas; reserva confirmada não é tocada |
| `TipoDePeriodoTest` (5, unitário) | bordas da virada, os quatro trimestres, janela móvel |

### Critérios de aceite

- [x] **Duas requisições simultâneas pela última consulta: exatamente uma é autorizada** — com o
      harness da AZ-018 apontado para o serviço real, em 3 rodadas, mais o caso de 32 pesquisas
      distintas contra saldo 5.
- [x] **A mesma solicitação repetida com a mesma chave debita uma vez só** — inclusive com as 32
      chamadas chegando juntas.
- [x] **Erro de provedor e falha interna não consomem franquia** — `SEARCH_FAILED` devolve ao saldo.
- [x] **Reserva órfã é liberada automaticamente** — com teste que envelhece a reserva no banco em
      vez de esperar o relógio.
- [ ] **Virada de período repõe o saldo exatamente uma vez** — a janela está testada nas bordas e o
      índice único impede dois períodos para a mesma janela, mas **não há teste que atravesse a
      virada com o relógio andando**. Depende de controlar o tempo no serviço, que ainda não é
      injetável. Fica como primeiro item da AZ-025.
- [x] **Saldo zero bloqueia nova pesquisa sem consumir** — 409 e nada gravado.
- [ ] **Oferecer o Premium** — é interface, do M3.
- [x] **Tipo de período e política de conclusão configuráveis, com o padrão documentado.**
- [x] **Ambiente rodando e verificável, com comando documentado.**

## Ressalvas

- **Testes de integração executados em 05/11/2026**, com Docker local e na CI. A primeira execução
  reprovou a primeira consulta simultânea: 9 de 32 requisições respondiam 500, com o saldo correto.
  Corrigido em `bugfix/AZ-022-periodo-concorrente`, junto de uma verificação errada do próprio teste.
- **Não há teste de virada de período com o tempo andando** (acima). É o buraco mais relevante
  desta entrega.
- **O relógio não é injetável** no serviço; `Instant.now()` aparece direto. Para testar virada e
  expiração com precisão, vale introduzir um `Clock` — mudança pequena, mas que toca vários pontos
  e não cabia aqui sem engordar a tarefa.
- **AZ-023 (planos configuráveis sem deploy) continua aberta**: hoje o plano é dado no banco, o que
  já tira o valor do código, mas publicar alteração ainda não tem fluxo.

## Próximos passos

- AZ-025: virada de período com relógio controlado, isolamento entre usuários (BOLA), token
  expirado e refresh reutilizado.
- AZ-023: publicação de plano sem deploy.
- Introduzir `Clock` injetável antes de o M3 depender de prazos.
