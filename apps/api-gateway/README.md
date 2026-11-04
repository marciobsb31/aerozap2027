# api-gateway

Entrada única da plataforma. Faz quatro coisas e só quatro: valida o token de entrada, limita a
taxa por origem, propaga o identificador de correlação e encaminha para o serviço da rota.

Regra de negócio aqui é proibida — há um teste de arquitetura que falha o build se este módulo
passar a depender de banco, de fila ou de outro serviço.

- Arquitetura, decisões e limites: [`docs/architecture/api-gateway.md`](../../docs/architecture/api-gateway.md)
- Contexto: [ADR-0002](../../docs/decisions/ADR-0002-deployables-iniciais.md)
- Verificar de ponta a ponta: `bash scripts/dev-gateway-check.sh`

Porta padrão: 8080 (`GATEWAY_PORT`). Exige `JWT_SECRET` com 32 caracteres ou mais e um Redis
disponível para o limite de taxa.
