# Frontend AeroZap

Workspace Angular com a aplicação web, o design system e o cliente de API.

## Node

Node **26 LTS** — a mesma versão da CI. O Angular 22 aceita 22.22.3+, 24.15+ ou 26+; o
`frontend-dev.sh` recusa o que estiver fora dessa faixa, com a mensagem dizendo o que instalar.

```powershell
winget install OpenJS.NodeJS.LTS   # Windows
```

## Como subir

```bash
bash scripts/frontend-dev.sh      # da raiz do repositório
```

Abre em <http://localhost:4200>. Na primeira vez instala as dependências (alguns minutos).

A aplicação fala com o gateway em <http://localhost:8080>. Para ter a API no ar:

```bash
bash scripts/dev-up.sh            # Postgres, Redis, RabbitMQ
# e os serviços conforme docs/desenvolvimento.md
```

Sem o gateway, a tela inicial mostra **Plataforma sem resposta** e um botão para tentar de novo —
é o comportamento esperado, não um defeito.

## Como verificar

```bash
bash scripts/frontend-check.sh    # roda o que a CI roda, na mesma ordem
```

Ou, dentro de `frontend/`:

| Comando                      | O que faz                                              |
| ---------------------------- | ------------------------------------------------------ |
| `npm start`                  | servidor de desenvolvimento                            |
| `npm test`                   | testes das três bibliotecas/aplicações, sem observar   |
| `npm run test:observar`      | testes da aplicação em modo observação                 |
| `npm run lint`               | ESLint + regras do angular-eslint nos modelos          |
| `npm run build`              | build de produção da aplicação                         |
| `npm run build:bibliotecas`  | empacota `design-system` e `api-client` (ng-packagr)   |
| `npm run verificar:contrato` | confirma que o cliente gerado bate com o OpenAPI       |

## Estrutura

```
frontend/
  projects/
    aerozap-web/      aplicação web (casca, rotas, páginas)
    design-system/    tokens, componentes e i18n
    api-client/       cliente de API gerado do contrato OpenAPI
```

`aerozap-admin` (painel interno, previsto na ADR-0001) entra no marco que o exigir; criar agora um
projeto vazio só faria a CI pagar o custo de construir e testar nada.

## Design system

Estilos globais entram uma vez por aplicação:

```scss
@use 'estilos'; // resolvido por stylePreprocessorOptions.includePaths
```

Tudo é variável CSS (`--az-*`), não variável Sass, para que o tema troque em tempo de execução.
Tema escuro acompanha o sistema operacional e pode ser fixado com `data-tema="claro|escuro"` no
elemento `<html>`.

Componentes: `az-botao`, `az-campo-texto`, `az-cartao`, `az-emblema`, `az-seletor-idioma`,
`az-pular-para-conteudo`.

## Idiomas

Português do Brasil e espanhol, com **troca sem recarregar a página**.

O i18n nativo do Angular compila um pacote por idioma e exige recarregar para trocar, o que o
critério da AZ-016 proíbe. Então os dois dicionários viajam no pacote e o idioma ativo é um signal
(`I18nService`): trocá-lo redesenha só o que depende dele e não perde o que a pessoa digitou.

```ts
i18n.t('inicio.busca.enviar');              // em componentes e modelos
i18n.t('rodape.direitos', { ano: 2026 });   // com interpolação
```

Regras que a suíte de testes cobre:

- **Nenhum texto de interface escrito no modelo.** `sem-strings-fixas.spec.ts` varre os `.html` da
  aplicação e falha ao achar texto fora do dicionário — inclusive em atributos como `rotulo` e
  `aria-label`.
- **Nenhuma chave faltando.** `Dicionario` é derivado do dicionário português, então um idioma
  incompleto não compila.
- **Mesmos marcadores de interpolação** nas duas línguas.

Para acrescentar um idioma: criar `dicionario.<idioma>.ts` tipado como `Dicionario`, registrá-lo em
`IDIOMAS` e `NOME_DO_IDIOMA`. Quando forem muitos, o caminho é carregar o dicionário sob demanda —
a interface do serviço não muda.

## Acessibilidade

`acessibilidade.spec.ts` roda o [axe-core](https://github.com/dequelabs/axe-core) sobre o DOM que a
aplicação realmente produz, em português e em espanhol, com e sem erro de validação na tela.

O que o axe não vê continua sendo trabalho de revisão: ordem de foco que faça sentido, texto que
explique o suficiente, contraste real (o jsdom não calcula layout, então a regra de contraste fica
desligada no teste — a paleta é conferida nos tokens).

Decisões que estão no código por acessibilidade, e não por estética:

- `<button>`, `<select>` e `<label>` nativos, nunca `div` clicável;
- link "pular para o conteúdo" como primeiro elemento focável;
- anel de foco sempre visível (`:focus-visible`), nunca removido;
- erro de campo anunciado por `role="alert"` e ligado por `aria-describedby`;
- `lang` do documento atualizado na troca de idioma, para o leitor de tela trocar de voz;
- cor nunca carrega significado sozinha (o emblema diz o estado por escrito);
- alvos de toque com no mínimo 44px;
- `prefers-reduced-motion` respeitado.

## Cliente de API

`api-client` **não é escrito à mão**: os tipos saem de
`docs/api-contracts/openapi/aerozap-v0.yaml` pelo gerador em `scripts/gerar-api-client.sh`, e o
arquivo gerado é versionado para quem clona compilar sem rodar nada.

```bash
bash scripts/gerar-api-client.sh              # regenera
bash scripts/gerar-api-client.sh --verificar  # falha se estiver fora de sincronia (a CI roda isto)
```

O contrato referencia os JSON Schemas já publicados em `docs/api-contracts/schemas/` por `$ref`,
em vez de repeti-los: duas definições do mesmo esquema divergem, é só questão de tempo.

Escrito à mão só a cola com o Angular: `provideAerozapApi`, o interceptador que acrescenta
`X-Correlation-Id` (e o `Authorization` **apenas** para o nosso endereço base, para não vazar token
a terceiros) e a tradução de Problem Details em `ProblemaDaApi`, que preserva o `correlationId`
mostrado na tela e usado pelo suporte.
