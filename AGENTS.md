# AGENTS.md — Regras comuns da equipe de agentes AeroZap

Este arquivo vale para **todos** os agentes (A0–A8). Cada agente tem seu próprio arquivo em `agentes/`, com identidade, propriedade, limites e prompt principal. O que estiver aqui prevalece sobre qualquer arquivo individual.

**Projeto:** AeroZap — plataforma de pesquisa, comparação e monitoramento de passagens aéreas (WhatsApp + Web Angular + Admin).
**Idioma de trabalho:** Português (Brasil). Código, nomes de API e eventos em inglês; textos para o usuário final sempre via i18n (pt-BR e es).
**Product Owner (humano):** o dono do projeto. Decisões comerciais e de risco são dele; os agentes propõem, não decidem.

---

## 1. Fonte de verdade (ordem de precedência)

1. Este `AGENTS.md` (regras inegociáveis — só mudam com aprovação do Product Owner).
2. ADRs aprovadas pelo CTO em `docs/decisions/`.
3. PRD em `docs/prd/`.
4. `AeroZap_Documentacao_Tecnica_Completa.pdf` (DTC v1.0, 03/11/2026) — baseline técnico: serviços, fluxos, modelo de dados, roadmap, riscos.
5. `agentes/*.md` e o manual de engenharia v1.0.

Se duas fontes conflitarem: **pare, registre o conflito e escalone ao CTO**. Não escolha em silêncio.

## 2. Regras inegociáveis

1. Não reutilizar código antigo sem avaliação e autorização.
2. Preservar a logo original e o slogan **"Search. Compare. Choose. Travel."**. Não redesenhar a marca. Enquanto a logo não for fornecida, usar placeholder claramente identificado.
3. Português e Espanhol desde a primeira versão (idioma padrão: pt-BR). Idioma, moeda, fuso e país são tratados separadamente.
4. WhatsApp e Web compartilham conta, histórico e franquia. Não existem regras de consumo diferentes por canal.
5. Falha interna ou de provedor não consome franquia.
6. Concorrência ou repetição (webhook, retry, duplo clique) não duplica consumo nem efeito de negócio. Idempotência é obrigatória.
7. Nunca inventar passagens, preços, disponibilidade, horários, companhias ou links de compra.
8. Uma oferta de voo **não é** passagem emitida, reserva confirmada ou cartão de embarque.
9. Nenhum dado de cartão de crédito é armazenado. Pagamento via gateway com tokenização.
10. Nenhum acesso cruzado entre usuários. O `user_id` vem sempre do token autenticado, nunca do cliente.
11. Nenhuma credencial em Git, logs, imagens Docker, respostas de API ou prompts. Segredos em secret manager, mascarados no painel.
12. A IA interpreta e recomenda; o Backend valida e executa. A IA nunca autoriza pagamento, altera plano/permissão, executa SQL nem acessa credenciais.
13. Toda integração externa respeita os termos do fornecedor e só é considerada funcional após teste real autorizado.
14. Milhas: só exibir disponibilidade com fonte autorizada e dado verificável. Nunca estimar nem converter tarifa em dinheiro para pontos.
15. Nenhuma remuneração comercial altera silenciosamente ranking ou recomendação. Qualquer destaque patrocinado é rotulado.
16. Dados pessoais: minimização, finalidade, retenção definida e conformidade com a LGPD. Logs sem tokens, senhas, áudio ou dados pessoais desnecessários.
17. Dados de demonstração são sempre identificados como demonstração, nunca apresentados como reais.
18. Toda entrega é testada. Toda decisão estrutural é documentada em ADR.

## 3. Decisões em aberto — não assumir

Vêm do DTC §20 e da revisão do manual. Enquanto não decididas pelo Product Owner, os agentes implementam de forma **configurável** e registram a premissa adotada.

| Tema | Situação | Prepara |
|---|---|---|
| Logo original | Arquivo ainda não anexado ao repositório | A2 + A8 |
| Versão do Java | 21 ou 25 (LTS), após validar compatibilidade das dependências | A1 + A6 |
| Trimestre do plano Free | Proposta: trimestre-calendário (jan/abr/jul/out); confirmar | A8 + A1 |
| Definição de "pesquisa concluída com sucesso" | Em aberto: resultado parcial (alguns provedores falharam) e zero resultados consomem franquia? | A8 + A1 + A4 |
| Preços (R$ 39,90 mensal / R$ 179,40 semestral / R$ 297,00 anual) | Hipótese comercial, não tabela aprovada. Devem ser configuráveis no Admin | A8 |
| WhatsApp (Cloud API, Evolution API ou ambos) | Avaliar estabilidade, termos, custo e conformidade | A5 |
| Provedores de voos | Confirmar acesso comercial, limites, custos e direitos de exibição | A4 |
| Milhas | Confirmar programas e fontes autorizadas | A4 |
| Gateway de pagamento | Escolher; definir renovação, parcelamento, estorno e inadimplência | A1 |
| Escala-alvo | Usuários ativos, pesquisas/minuto, pico e orçamento de infra | A6 + A7 |
| Retenção | Prazos para mensagens, áudio, transcrição, logs e histórico | A8 + A1 + jurídico |
| Estrutura de diretórios | Proposta no item 5 abaixo; formalizar em ADR-0001 | A0 + A6 |

O CTO mantém essa lista viva em `docs/decisions/decisoes-em-aberto.md`.

## 4. Como cada agente trabalha

1. Ler a TASK, este arquivo, seu próprio arquivo em `agentes/`, as seções relevantes do DTC e os contratos existentes.
2. Inspecionar o repositório antes de escrever código. Verificar o que já existe.
3. Mudança estrutural ou de contrato compartilhado: propor ao CTO **antes** de implementar.
4. Trabalhar somente nos módulos que possui. Precisa de mudança em módulo alheio? Abra uma TASK para o dono.
5. Implementar em branch própria, com commits pequenos e objetivos, prefixados pelo ID (`AZ-012: ...`).
6. Escrever testes e atualizar a documentação dos contratos públicos.
7. Subir o que construiu e deixar **rodando e verificável**: comando documentado, evidências (logs, prints, relatórios de teste). O Product Owner deve apenas verificar, não montar.
8. Entregar um `DELIVERY REPORT` e mover a tarefa para `REVIEW`.

## 5. Estrutura do repositório (proposta, a formalizar em ADR-0001)

O manual e o DTC §17 divergem. Proposta de reconciliação: usar a estrutura do DTC (monorepo com serviços independentes) mais a pasta de documentação do manual.

```text
aerozap/
├── AGENTS.md
├── CLAUDE.md
├── agentes/                 # definição dos agentes e templates
├── apps/                    # serviços de backend (um diretório por serviço)
├── frontend/                # aerozap-web e aerozap-admin
├── libs/                    # java/event-contracts, typescript/design-system, api-client
├── infrastructure/          # docker, compose, kubernetes (quando justificado), monitoring
├── tests/                   # E2E, carga e segurança transversais
└── docs/
    ├── prd/
    ├── architecture/
    ├── api-contracts/
    ├── decisions/           # ADRs
    └── runbooks/
```

Cada serviço tem seu próprio build, imagem Docker, configuração, testes e documentação. Nenhum serviço acessa tabelas de outro. Bibliotecas compartilhadas não carregam regras de negócio de um domínio para outro.

## 6. Governança

1. Cada agente trabalha só nos módulos atribuídos; ninguém sobrescreve alteração de outro.
2. Mudança de contrato compartilhado (REST/OpenAPI, eventos, `SearchIntent`, `FlightOffer`, `FlightProvider`, `WhatsAppProvider`) exige aprovação do CTO e aviso aos consumidores. Contratos são versionados e compatíveis retroativamente.
3. Nenhum agente remove funcionalidade do PRD nem introduz tecnologia estrutural sem justificativa aprovada.
4. Mudança em banco compartilhado ou migração Flyway passa por avaliação do CTO.
5. Toda alteração relevante tem testes; toda alteração estrutural tem documentação e plano de reversão.
6. O QA (A7) valida antes da aprovação final; o CTO (A0) aprova mudanças arquiteturais e o DONE.
7. Ninguém envia direto para `main`.

Branches: `main`, `develop`, `feature/AZ-001-authentication`, `bugfix/AZ-004-usage-control`, `hotfix/AZ-005-security`.

## 7. Protocolo de comunicação

Templates em `agentes/templates/`: `task.md`, `delivery-report.md`, `adr.md`.

| Estado | Significado |
|---|---|
| BACKLOG | Ainda não iniciada |
| READY | Pronta para execução (dependências e contratos definidos) |
| IN_PROGRESS | Em desenvolvimento |
| BLOCKED | Bloqueada por dependência ou decisão |
| REVIEW | Aguardando revisão (CTO e QA) |
| REWORK | Precisa de ajustes |
| DONE | Aprovada e integrada |

Cada agente mantém no máximo duas tarefas em `IN_PROGRESS`. Tarefas paralelas só quando tocam módulos disjuntos.

## 8. Severidade de problemas

| Nível | Exemplos | Efeito |
|---|---|---|
| Crítico | Vazamento de dados, acesso cruzado, cobrança/benefício indevido, credencial exposta | Bloqueia a entrega; CTO notificado na hora |
| Alto | Consumo duplicado de franquia, webhook sem validação, perda de mensagem | Bloqueia release |
| Médio | Falha de resiliência, erro de validação, teste ausente em fluxo importante | Corrigir antes do DONE ou registrar risco aceito pelo CTO |
| Baixo | Inconsistência visual, texto, melhoria | Backlog |
