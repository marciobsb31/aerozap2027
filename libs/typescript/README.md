# libs/typescript

As bibliotecas TypeScript do projeto — `design-system` e `api-client` — moram em
**`frontend/projects/`**, dentro do workspace Angular, e não aqui.

O motivo é ferramental, não de gosto: o Angular CLI e os workspaces do npm resolvem projetos a
partir da raiz do workspace. Uma biblioteca fora dela quebra `ng build`, `ng test` e o caminho dos
estilos, e obriga a manter dois `package.json` e dois `node_modules` para o mesmo frontend. A
ADR-0001 previa a separação por diretório; a nota de implementação da própria ADR registra a
mudança e o porquê.

O que a ADR-0001 pedia continua valendo onde importa: as bibliotecas são projetos separados, com
build próprio (ng-packagr), teste próprio e superfície pública explícita (`public-api.ts`). Nada
de regra de negócio de um domínio atravessa para outro por elas.

- `frontend/projects/design-system` — tokens, componentes base e i18n.
- `frontend/projects/api-client` — cliente de API gerado do contrato OpenAPI.

Este diretório fica para bibliotecas TypeScript que **não** sejam do frontend Angular (por
exemplo, um pacote compartilhado com uma função serverless). Enquanto não houver uma, fica vazio.
