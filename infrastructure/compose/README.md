# Ambiente local (AZ-011)

Um comando sobe PostgreSQL, RabbitMQ, Redis e armazenamento de objetos (S3 compatível, com URLs assinadas).

## Pré-requisitos
Docker Desktop (ou Docker Engine) com Compose v2 em execução.

## Subir
```bash
bash scripts/dev-up.sh
```
O script cria o `.env` a partir de `.env.example` na primeira vez, sobe tudo com `docker compose up -d --wait` e só termina quando todos os health checks estão saudáveis.

## Subir e verificar de uma vez (grava a evidência em `logs/dev-check.log`)
```bash
bash scripts/dev-check.sh
```

## Verificar
```bash
bash scripts/dev-verify.sh      # testa cada serviço e imprime a evidência
docker compose --env-file .env -f infrastructure/compose/docker-compose.yml ps
```

## Derrubar
```bash
bash scripts/dev-down.sh                  # mantém os dados
bash scripts/dev-down.sh --apagar-dados   # apaga também os volumes
```

## Serviços e portas (somente em 127.0.0.1)
| Serviço | Porta | Painel |
|---|---|---|
| PostgreSQL 17.5 | 5432 | — |
| RabbitMQ 4.1 | 5672 | http://localhost:15672 |
| Redis 7.4 | 6379 | — |
| Object storage (RustFS, S3) | 9000 | http://localhost:9001 |

## Regras
- Versões de imagem fixadas; nenhuma tag `latest`.
- Senhas só por variável de ambiente (`.env`, ignorado pelo Git). Os valores do `.env.example` são fictícios e servem apenas para uso local.
- As portas escutam só em `127.0.0.1`.
- O RustFS é só para desenvolvimento (o MinIO saiu do Docker Hub). A escolha de produção fica para a Fase 3 (D-14).
