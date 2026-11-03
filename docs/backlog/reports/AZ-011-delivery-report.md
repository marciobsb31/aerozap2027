# DELIVERY REPORT

Task: AZ-011

Agent: A6 (DevOps/SRE)

Branch / PR: `feature/AZ-011-docker-compose-local` (PR a abrir)

Status: READY_FOR_REVIEW (verificado em execução real em 03/11/2026)

## Implementado
- `infrastructure/compose/docker-compose.yml` com PostgreSQL 17.5, RabbitMQ 4.1.0 (com painel), Redis 7.4.2 e object storage RustFS 1.0.4 (S3 compatível, URLs assinadas).
- Health checks em todos os serviços, volumes nomeados, rede `aerozap-net`, limites de memória e CPU.
- Portas publicadas só em `127.0.0.1`. Senhas obrigatórias por variável (o Compose falha com mensagem clara se faltarem).
- Scripts `scripts/dev-up.sh` (sobe e espera ficar saudável), `scripts/dev-verify.sh` (testa cada serviço) e `scripts/dev-down.sh`.
- `.env.example` ampliado e `infrastructure/compose/README.md`.

## Arquivos alterados
- `infrastructure/compose/docker-compose.yml`, `infrastructure/compose/README.md`
- `scripts/dev-up.sh`, `scripts/dev-verify.sh`, `scripts/dev-down.sh`
- `.env.example`

## Contratos
Não.

## Testes
- Unitários: N/A
- Integração: **PASS** (`scripts/dev-check.sh`: os 4 serviços saudáveis e verificados individualmente)
- Contrato: N/A
- Concorrência/idempotência: N/A
- Segurança: parcial (sem segredos no repositório; portas só em localhost)

## Como verificar
```bash
bash scripts/dev-up.sh
bash scripts/dev-verify.sh
```
O ambiente está rodando: **Sim**, na máquina do Product Owner (Docker Desktop, Windows).

## Evidências
- `docker compose config` valida sem erro; imagens resolvidas: `postgres:17.5-alpine`, `rabbitmq:4.1.0-management-alpine`, `redis:7.4.2-alpine`, `rustfs/rustfs:1.0.4`; nenhuma tag `latest`.
- Sintaxe dos três scripts validada com `bash -n`.
- Execução real em 03/11/2026: `up` com `--wait` terminou com sucesso (`up=0 verify=0`); `docker compose ps` mostra postgres, rabbitmq, redis e object-storage `healthy`. Saída completa em `docs/backlog/reports/AZ-011-evidencia-dev-check.log`.
- Verificações individuais: consulta no PostgreSQL, RabbitMQ em execução, Redis responde PONG com senha, object storage ativo.
- Problemas encontrados e corrigidos na primeira execução: (1) `minio/minio` não existe mais no Docker Hub, trocado por `rustfs/rustfs:1.0.4`; (2) a porta 5432 do Windows estava indisponível, o PostgreSQL passou a usar 15432 no computador.

## Dependências
AZ-010 (concluída). Evidência de execução real pendente.

## Riscos
- **Tags de imagem não conferidas contra o registro:** o ambiente do agente não alcança o Docker Hub. Se alguma tag não existir, o `docker compose pull` falha de forma explícita e a tag é corrigida.
- **MinIO removido:** a imagem `minio/minio` não existe mais no Docker Hub (confirmado no `dev-check` de 03/11/2026). Substituído por `rustfs/rustfs:1.0.4`, projeto mais novo; aceitável só para desenvolvimento. A escolha de produção entra em D-14 (Fase 3).
- **RustFS:** validado em execução (health check `/health` saudável). Falta ainda testar a geração e o uso de URLs assinadas (será exercitado na AZ-043).
- **Tags conferidas** nas APIs do Docker Hub em 03/11/2026: `postgres:17.5-alpine`, `redis:7.4.2-alpine`, `rabbitmq:4.1.0-management-alpine` e `rustfs/rustfs:1.0.4` existem.

## Decisões necessárias
Nenhuma para fechar a AZ-011.

## Checklist de pronto
- [x] Critérios de aceite atendidos
- [ ] Testes passando em CI (CI chega na AZ-012)
- [x] Documentação atualizada
- [x] Sem credenciais, sem dados reais de usuários
- [x] Dados de demonstração rotulados (não há)
- [x] Logs sem dados sensíveis
- [x] Plano de reversão: remover `infrastructure/compose/` e `scripts/`
