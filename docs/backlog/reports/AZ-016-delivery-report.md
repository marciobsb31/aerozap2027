# DELIVERY REPORT

Task: AZ-016

Agent: A2 (Frontend Architect), com A0 (CTO) na revisão

Branch / PR: `feature/AZ-016-design-system` → `develop`

Status: READY_FOR_REVIEW

## Implementado

**Workspace Angular 22, TypeScript strict.** `frontend/` com três projetos: `aerozap-web`
(aplicação), `design-system` e `api-client` (bibliotecas, ng-packagr). Zoneless, componentes
standalone, `signal`/`input`/`output`, controle de fluxo nativo (`@if`, `@for`, `@switch`), rotas
carregadas sob demanda. `strict` ligado explicitamente no `tsconfig.json` do workspace.

**Design system.** Tokens em variável CSS (`--az-*`), nunca em variável Sass, porque o tema precisa
trocar em tempo de execução: tema escuro segue o sistema operacional e pode ser fixado com
`data-tema` no `<html>`. Paleta de índigo de cabine com âmbar de cartão de embarque como cor de
ação; o verde fica reservado para estado de canal, para que "verde" signifique sempre a mesma coisa
no produto. Seis componentes: `az-botao`, `az-campo-texto`, `az-cartao`, `az-emblema`,
`az-seletor-idioma`, `az-pular-para-conteudo`.

**i18n pt-BR/es com troca sem recarregar.** O i18n nativo do Angular compila um pacote por idioma e
exige recarregar a página para trocar — o que o critério de aceite proíbe. Então os dois dicionários
viajam no pacote e o idioma ativo é um signal (`I18nService`): trocá-lo redesenha só o que depende
dele e não perde o que a pessoa já digitou. `ChaveDeTraducao` é derivado do dicionário português, de
modo que **um idioma incompleto não compila** — "faltou traduzir" vira erro de build em vez de texto
faltando na tela.

**Cliente de API gerado do OpenAPI.** O contrato `docs/api-contracts/openapi/aerozap-v0.yaml`
descreve o que a borda expõe hoje e **referencia por `$ref` os JSON Schemas já publicados na
AZ-012**, em vez de repeti-los: duas definições do mesmo esquema divergem, é só questão de tempo.
`scripts/gerar-api-client.sh` gera os tipos, e `--verificar` falha se o gerado sair de sincronia —
é um passo da CI. Escrito à mão só a cola com o Angular: `provideAerozapApi`, o interceptador de
correlação e a tradução de Problem Details em `ProblemaDaApi`.

**Aplicação.** Casca com link de pular, cabeçalho, navegação, conteúdo e rodapé; páginas de início,
busca e endereço inexistente. A tela inicial chama o `ping` de verdade pelo cliente gerado e mostra
o estado da plataforma — é o que prova, ao abrir, que o caminho pessoa → gateway → serviço está de
pé. A página de busca diz com todas as letras que a busca nas companhias ainda não acontece:
resultado inventado vazaria para o teste, para a demonstração e para a expectativa de quem vê.

**Placeholder de logo.** A marca é um losango âmbar desenhado em CSS ao lado do nome
(`.casca__marca-sinal`), identificado como provisório. Não há arquivo de imagem a substituir: quando
a identidade visual existir, troca-se esse elemento e o token `--az-cor-acao`.

## Decisões

**Angular 22, e não 21.** A ADR-0003 manda escolher "a major estável mais recente que ainda tenha
pelo menos 12 meses de suporte restante na data de AZ-016". Consultado hoje: a mais recente é a 22
(22.2.4 no npm; a 21 está em 21.2.26). O Angular passou a lançar major por ano a partir da 23
(junho de 2027), e a 22 tem suporte ativo até junho de 2027 e LTS até junho de 2028 — cerca de 20
meses restantes. A regra seleciona a 22 com folga. Nota acrescentada à ADR-0003.

**Bibliotecas dentro do workspace, não em `libs/typescript/`.** A ADR-0001 previa `libs/typescript/`
para `design-system` e `api-client`. O Angular CLI e os workspaces do npm resolvem projetos a partir
da raiz do workspace; fora dela, `ng build` e `ng test` quebram e passa a ser preciso manter dois
`package.json` e dois `node_modules` para o mesmo frontend. As bibliotecas ficaram em
`frontend/projects/`, continuando projetos separados, com build, teste e superfície pública
próprios. Nota acrescentada à ADR-0001; `libs/typescript/README.md` aponta para o novo lugar.

**`aerozap-admin` adiado.** Previsto na ADR-0001, não é exigido por esta tarefa. Um projeto vazio
só faria a CI pagar build e teste de nada; entra no marco que precisar dele.

**Gerador por `npx` com versão fixa, e não como dependência.** O `openapi-typescript` 7 declara par
com TypeScript 5 e o Angular 22 usa TypeScript 6. Instalá-lo na mesma árvore forçaria
`--legacy-peer-deps` no projeto inteiro por causa de uma ferramenta que só roda quando o contrato
muda.

**`$defs` achatado na geração** (`scripts/lib/achatar-defs.mjs`). O gerador trata `$defs` como se
fosse propriedade do objeto, e o cliente passaria a exigir um campo `$defs` que nenhuma resposta
tem. Em vez de mexer nos contratos publicados — que são a fonte de verdade dos eventos e estão
cobertos por teste em Java —, os `$ref` internos são resolvidos numa cópia temporária.

**Node 26 na CI.** O Node 22 saiu do suporte ativo (segurança até abril de 2027) e o 26 é o LTS
atual. O `frontend-dev.sh` recusa versão fora da faixa que o Angular 22 declara em `engines`.

## Como verificar

```bash
bash scripts/frontend-dev.sh      # sobe em http://localhost:4200 (instala na primeira vez)
bash scripts/frontend-check.sh    # roda o que a CI roda, na mesma ordem
```

## Evidências

**Testes: 57, todos verdes** (`ng test`, Vitest + jsdom, 14 arquivos).

| Projeto | Testes | O que cobrem |
| --- | --- | --- |
| `aerozap-web` | 19 | casca, troca de idioma na tela, validação, navegação, estado da plataforma, varredura de strings fixas, acessibilidade |
| `design-system` | 27 | seis componentes, serviço de i18n, consistência dos dicionários |
| `api-client` | 12 | desembrulho do envelope, tradução de Problem Details, interceptador, tipos do contrato |

**Build de produção**: 284,90 kB inicial (76,97 kB transferidos), com `inicio`, `busca` e
`nao-encontrada` em pedaços separados, carregados sob demanda.

**Lint**: `ng lint` limpo nos três projetos (ESLint 10 + angular-eslint 22, regras de TypeScript e
de modelo).

**Contrato**: `npm run verificar:contrato` → "cliente gerado em sincronia com o contrato".

**Auditoria**: `npm audit --audit-level=critical` passa. Restam 15 avisos moderados, todos do mesmo
`minimatch` na cadeia do ESLint — ferramenta de desenvolvimento, nada que vá para o navegador.

**Telas conferidas visualmente** no build de produção, em pt-BR e es, claro e escuro, 1280px e
390px: início, validação do campo, página de busca e o link de pular ao receber foco.

### Critérios de aceite

- [x] **Aplicação sobe localmente com comando documentado** — `bash scripts/frontend-dev.sh`,
      documentado no `frontend/README.md`.
- [x] **Troca pt-BR/es sem recarregar; nenhuma string fixa** — a troca é por signal, provada em
      `app.spec.ts` ("troca o idioma da interface sem recarregar nem recriar a página") e em
      `inicio.spec.ts`. A ausência de string fixa é verificada por varredura automática
      (`sem-strings-fixas.spec.ts`), que falha ao achar texto entre tags ou em atributos de texto
      fora do dicionário, e proíbe modelo embutido no TypeScript — por onde a varredura seria
      contornada.
- [x] **Acessibilidade básica verificada** — axe-core sobre o DOM real em cinco cenários
      (`acessibilidade.spec.ts`), incluindo espanhol e erro de validação na tela. Zero violações.
      Além disso: `<button>`/`<select>`/`<label>` nativos, link de pular, foco sempre visível, erro
      por `role="alert"` ligado por `aria-describedby`, `lang` atualizado na troca de idioma, cor
      nunca sozinha com o significado, alvo de toque de 44px, `prefers-reduced-motion` respeitado.
- [x] **Testes unitários dos componentes base** — 27 no `design-system`.
- [x] **Ambiente rodando e verificável, com comando documentado** — `scripts/frontend-check.sh`
      repete a sequência da CI e grava `logs/frontend-check.log`.

## Defeitos encontrados e corrigidos

Dois apareceram só ao olhar a tela, e nenhum dos dois teria sido pego pelos testes como estavam:

1. **O seletor mostrava "Português (Brasil)" com a interface em espanhol.** O `[value]` do
   `<select>` é aplicado antes de as opções existirem. A seleção passou para a própria opção
   (`[selected]`), com teste cobrindo o caso de o idioma já vir definido de fora.
2. **O botão de busca alinhava com o rótulo, não com o campo.** Corrigido no CSS da tela.

## Ressalvas

- **A instalação de dependências na máquina do PO ainda não foi concluída por mim**: cada tentativa
  pela ponte com o Windows é cortada no limite de tempo e recomeça. Nativo, leva um ou dois minutos
  (`bash scripts/frontend-dev.sh`). Build, testes, lint e as telas foram verificados no ambiente do
  agente, no mesmo commit.
- **Node da máquina do PO é o 22.23.4.** Funciona; o recomendado (e o da CI) é o 26 LTS.
- **Só há um endpoint no contrato** porque só há um endpoint na borda. Rota planejada não entra: um
  cliente gerado a partir de endpoint inexistente compila e mente.
- **O contraste real da paleta não é verificado por máquina** — o jsdom não calcula layout, então a
  regra de contraste do axe fica desligada no teste.

## Próximos passos

- AZ-018 (estratégia de testes) deve incorporar a varredura de strings fixas e o axe à matriz de
  rastreabilidade.
- Quando a identidade visual existir, trocar o placeholder de logo e revisar os tokens de cor.
- `aerozap-admin` no marco que o exigir.
