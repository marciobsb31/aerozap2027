# ADR-0002 — Deployables iniciais

Data: 2026-11-03

Status: APROVADA em 2026-11-03 (Product Owner e CTO)

Autor: A0

Aprovador: A0 (CTO) e Product Owner

## Contexto
O DTC prevê 11 serviços lógicos (§4.1) e pede "arquitetura distribuída enxuta, expandindo conforme métricas reais" (§1.1). Cada processo a mais aumenta custo operacional, pipelines, observabilidade e superfície de falha.

## Opções consideradas
1. **Monólito modular único:** menor custo, mas isola mal falhas e cotas de provedor e escala tudo junto.
2. **11 microsserviços desde o início:** máxima autonomia, custo e complexidade altos antes de existir métrica de carga.
3. **Poucos deployables com módulos de fronteira rígida (escolhida como proposta):** isolamento onde há ganho real, custo moderado.

## Decisão
Cinco deployables iniciais:

| Deployable | Módulos | Motivo de separar |
|---|---|---|
| `api-gateway` | BFF, autenticação de entrada, rate limit | Borda, escala independente |
| `platform-core` | Identity, Subscription & Billing, Usage/Entitlement, Admin/Audit | Consistência de conta e franquia; ciclo de release parecido |
| `search` | Flight Search, Provider Integration, normalização, workers de pesquisa e de alertas | Escala por pesquisa; isolar cotas e falhas de provedores |
| `conversation-ai` | Conversation, orquestração de IA, transcrição | Workers próprios; limites de custo e de tokens |
| `messaging` | WhatsApp Integration, Notification | Escala por volume de mensagens |

Regras:
- Dentro de um deployable, cada módulo tem **schema próprio, pacote próprio e API interna explícita**; nenhum acesso a tabelas de outro módulo. Verificar com testes de arquitetura (por exemplo ArchUnit) no CI.
- Comunicação entre deployables por REST (consulta e comando síncrono) e eventos (RabbitMQ com Outbox).
- `price-monitoring` começa como workers dentro de `search` e vira deployable quando a fila de monitoramento, o custo de provedor ou o ciclo de release justificarem.

**Critérios para separar um módulo em deployable próprio:** necessidade de escala independente comprovada por métrica; isolamento de falha ou de cota; ciclo de release distinto; requisito de segurança diferente.

## Consequências
Menos infraestrutura inicial e menos pipelines; fronteiras preparadas para extração. Risco: erosão das fronteiras, mitigada por testes de arquitetura.

## Plano de implementação e de reversão
O esqueleto (AZ-014) é replicado nos cinco deployables. Extrair um módulo depois exige apenas mover o pacote e o schema, pois o acesso já é por API interna e eventos.

## Validação
Testes de arquitetura passando; nenhuma dependência cíclica entre módulos; métricas por módulo disponíveis para decidir futuras separações.
