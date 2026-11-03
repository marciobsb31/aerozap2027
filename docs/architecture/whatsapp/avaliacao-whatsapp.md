# AZ-005 — Avaliação do WhatsApp: Cloud API oficial (Meta) x Evolution API

**Autor:** A5 (WhatsApp Integration Engineer) · **Data:** 03/11/2026 · **Status:** proposta para decisão do CTO e do Product Owner (gera ADR)
**Base:** DTC v1.0 §6.1, §6.2, §7, §13, §20; `AGENTS.md` §2 (regras 6, 13, 16) e §3; `agentes/05-whatsapp-integration-engineer.md`.
**Método:** pesquisa na web em 03/11/2026, só em páginas da Meta/WhatsApp e da Evolution. Cada afirmação traz a URL. "NÃO VERIFICADO" = não confirmado em fonte oficial lida. Documentação de preço e política da Meta muda a cada trimestre; reconfirmar antes de contratar. Nada aqui é parecer jurídico.

---

## 1. Resumo e recomendação

1. **Adaptador padrão de produção: WhatsApp Cloud API oficial (Meta), integração direta.** É o único caminho sem violação de termos e sem risco de banimento do número por uso de cliente não oficial.
2. **Evolution API:** usar apenas como adaptador alternativo. O modo Cloud API da Evolution é aceitável, mas não acrescenta nada que justifique uma peça a mais em produção. O modo Baileys (WhatsApp Web) fica **proibido em produção** sem ADR com aceitação formal de risco pelo Product Owner. Serve, no máximo, para laboratório com número descartável.
3. **A política de IA da Meta precisa de decisão consciente.** Desde 15/01/2026 os termos restringem "AI Providers" (assistentes de IA de uso geral). O AeroZap é, em princípio, um serviço de busca de passagens com IA acessória, mas a classificação é da Meta. Ver §2.8.
4. **Custo mudou em 01/10/2026:** mensagens de serviço (resposta dentro da janela) deixaram de ser grátis, salvo 1.000 por número por mês. Ver §2.2.
5. **Identificação por telefone não é mais garantida** (BSUID/usernames). Impacta o AZ-021. Ver §2.7.

---

## 2. WhatsApp Cloud API (Meta)

### 2.1 Requisitos de conta

| Item | O que a documentação diz | Fonte |
|---|---|---|
| Portfólio empresarial (Meta Business) | Obrigatório. Contém a conta WhatsApp, a conta de mensagens e o app. | https://developers.facebook.com/documentation/business-messaging/whatsapp/about-the-platform |
| Verificação da empresa | Portfólios novos começam com limite de 250 usuários únicos/dia sem ela (messaging-limits); a Meta a trata como fator de "funcionalidade melhorada" (ex.: mais vazão). É um dos caminhos para subir o limite de mensagens, e dá acesso a mais números (2 para 20) e a mais templates (250 para até 6.000). **Prazo de análise: NÃO VERIFICADO** (a página oficial lida não informa). | about-the-platform; https://developers.facebook.com/documentation/business-messaging/whatsapp/messaging-limits ; https://developers.facebook.com/documentation/business-messaging/whatsapp/business-phone-numbers/phone-numbers ; https://developers.facebook.com/documentation/business-messaging/whatsapp/templates/overview |
| Número | Próprio, recebe SMS ou voz, verificado por código. Número já em uso no app WhatsApp só serve depois de removido da conta. Short codes não são aceitos. Fixo e VoIP: "não recomendado" para SMS, aceitável por voz. | phone-numbers (acima) |
| Display name | Obrigatório no registro; estados `APPROVED`, `PENDING_REVIEW`, `DECLINED` etc. Critérios de aprovação ficam em página separada. **Regras do nome: NÃO VERIFICADO** (a página oficial de ajuda bloqueia leitura automática). | phone-numbers; https://www.facebook.com/business/help/338047025165344 |
| Selo de conta oficial (OBA) | Opcional: empresa verificada, display name aprovado, 30 dias de plataforma, 2FA no número. Sem prazo de análise informado. | https://developers.facebook.com/documentation/business-messaging/whatsapp/official-business-accounts/ |
| Ambiente de teste | Ao começar na Cloud API são criados automaticamente conta, conta de mensagens e número de teste, com limites relaxados e sem meio de pagamento para templates. | about-the-platform |
| Moeda | A moeda é definida na criação da conta e não muda. BRL disponível desde 01/07/2026 para empresas com "Sold-To" Brasil; contas não-BRL devem migrar até 30/06/2027. | https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing |

### 2.2 Modelo de preço vigente (fonte: https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing)

- **Por mensagem entregue** desde 01/07/2025 (substituiu conversa). Meta não cobra mensagens do usuário para a empresa.
- **Categorias:** templates (marketing, utility, authentication) e não-template (service, dentro da janela). Cada mensagem tem uma só categoria.
- **Preço depende do código de país do destinatário** (+55 tem tarifa própria na tabela). **Valores em BRL: NÃO VERIFICADO.** Estão nos arquivos CSV/PDF "rate cards" da própria página e no seletor interativo em business.whatsapp.com/products/platform-pricing. O A5 não os transcreve para não inventar.
- **Quando é grátis:**
  - Janela de ponto de entrada gratuito (anúncio Click-to-WhatsApp): respostas gratuitas por até 7 dias.
  - Camada gratuita de serviço: as primeiras 1.000 mensagens de serviço entregues por número por mês; zera no início do mês, sem acúmulo.
- **Mudança de 01/10/2026 (já em vigor hoje):** mensagens de serviço acima da camada gratuita e mensagens utility enviadas em resposta dentro da janela passaram a ser cobradas. Mensagens de serviço seguem o preço de utility/authentication do mercado, sem faixa de volume. Exige **meio de pagamento cadastrado até 30/09/2026, ou as mensagens de serviço deixam de ser entregues.** Fonte: https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing/non-template-messages
- **Faixas de volume:** só para utility e authentication, por mercado, por portfólio, zeram todo mês.
- **Cadência de mudanças:** só no dia 1º do trimestre; aviso mínimo de 1 mês (tarifas), 3 (adicionais) e 6 (modelo). Alertas de revisão: próxima janela 01/01/2027.
- **Onde cairia o alerta de preço (decisão para o A8 e o Product Owner):**
  - Dentro da janela de 24 h (usuário falou há pouco): mensagem de serviço, cobrada pela regra acima.
  - Fora da janela (caso típico de alerta): precisa de template. Se for **utility** ("seu alerta atingiu o valor-alvo", enviado por solicitação do usuário), é cobrado como utility. Se tiver oferta, promoção ou incentivo à compra, vira **marketing** (mais caro, com limite por usuário). A documentação define utility como "não promocional e específico ou solicitado pelo usuário", e diz que conteúdo misto é marketing. Fonte: https://developers.facebook.com/documentation/business-messaging/whatsapp/templates/template-categorization
  - **A documentação não traz exemplo de alerta de preço. A classificação final é da Meta (NÃO VERIFICADO)**: tratar como utility é hipótese. Recategorização utility para marketing com 1 dia de aviso (instantânea após advertência). Redigir o template factual, sem "aproveite", sem desconto, sem cross-sell. Orçar o pior caso (marketing).

### 2.3 Janela de atendimento e templates

- Janela de **24 horas** abre quando o usuário envia mensagem ou liga e reinicia a cada nova mensagem dele. Dentro dela: qualquer tipo de mensagem sem aprovação prévia. Fora: **só templates aprovados** (erro `131047` = mais de 24 h desde a última resposta). Fontes: https://developers.facebook.com/documentation/business-messaging/whatsapp/messages/send-messages ; https://developers.facebook.com/documentation/business-messaging/whatsapp/support/error-codes
- **Categorias:** marketing (padrão para conteúdo misto), utility (não promocional, ligado a ação ou solicitação do usuário), authentication (apenas OTP, sem URL, mídia ou emoji). Fonte: template-categorization.
- **Aprovação:** revisão automática e manual; decisão "em até 24 horas"; apelação também em até 24 h; notificação por WhatsApp Manager, e-mail e webhook `message_template_status_change`. Fonte: https://developers.facebook.com/documentation/business-messaging/whatsapp/templates/template-review/
- **Limites:** 100 templates criados por hora por conta de mensagens; 250 templates para portfólio não verificado (até 6.000 se verificado e com display name aprovado). Templates não são traduzidos pela Meta: **pt-BR e es são templates separados** e cada idioma conta no limite. Qualidade (alta/média/baixa) pode pausar (`132015`) ou desativar (`132016`) o template. Fonte: templates/overview.
- **Entrega:** TTL padrão de 30 dias para mensagens não-autenticação. Ordem de entrega **não é garantida**. Fonte: send-messages.

### 2.4 Opt-in e política de mensagens

- Só contatar quem forneceu o número e deu opt-in claro, nomeando a empresa; opt-out deve ser respeitado; opt-in por categoria é boa prática. Fontes: https://developers.facebook.com/documentation/business-messaging/whatsapp/getting-opt-in ; https://business.whatsapp.com/policy
- A política permite **automação dentro da janela de 24 h** e exige "caminhos de escalonamento claros" (pode ser e-mail, site, formulário, não só atendente humano no chat). Fonte: https://business.whatsapp.com/policy
- Para o AeroZap: opt-in explícito para alertas na criação do alerta (Web e conversa), separado da conversa reativa; comando de saída ("PARAR") e registro do consentimento (prova, data, texto, canal).

### 2.5 Limites de mensagens por dia e vazão

- **Limite de usuários únicos por 24 h (fora da janela), por portfólio:** 250 inicial, depois 2.000, 10.000, 100.000, ilimitado. Subir exige verificar a empresa ou enviar 2.000 mensagens de template entregues a números únicos em 30 dias com boa qualidade; aumento automático em até 6 horas. Conversas iniciadas pelo usuário não contam. Fonte: https://developers.facebook.com/documentation/business-messaging/whatsapp/messaging-limits ; (mudança para nível de portfólio em 08/10/2025: changelog https://developers.facebook.com/documentation/business-messaging/whatsapp/changelog)
- **Vazão:** 80 mensagens/s por número (entrada + saída); até 1.000 mps por upgrade automático (exige limite ilimitado, 100 mil usuários únicos em 24 h, qualidade ao menos amarela). Erro `130429`. Par de remetente-destinatário: cerca de 1 mensagem a cada 6 s; erro `131056`. Fontes: https://developers.facebook.com/documentation/business-messaging/whatsapp/throughput ; about-the-platform
- Alertas em massa começam limitados a 250 usuários únicos/dia: relevante para o beta (M5). Planejar a verificação da empresa antes.

### 2.6 Webhooks

- **Verificação (GET):** `hub.mode=subscribe`, `hub.challenge` devolvido, `hub.verify_token` conferido. **Assinatura:** `X-Hub-Signature-256` = `sha256=` + SHA-256 do payload com o App Secret ("você não precisa validar, mas deveria"). Responder `200`. Fonte: https://developers.facebook.com/docs/graph-api/webhooks/getting-started/
- **Reentrega:** retenta imediatamente e depois com frequência decrescente por **36 horas**; sem confirmação, descarta. A Meta diz que o servidor deve deduplicar. Esta página é de webhooks do Graph API em geral; **a confirmação específica para WhatsApp: NÃO VERIFICADO.** Prazo máximo de resposta: **NÃO VERIFICADO.**
- **Endpoint:** HTTPS com certificado válido (sem autoassinado); **um endpoint por app Meta**. A Meta usa mTLS na saída (CA `meta-outbound-api-ca-2025-12.pem`, CN `client.webhooks.fbclientcerts.com`) e documenta a verificação como opcional/endurecimento. Fonte: https://developers.facebook.com/docs/whatsapp/cloud-api/guides/set-up-webhooks/ e a página do Graph API acima.
- **Status:** `sent/delivered/read/failed` chegam por webhook; resposta de envio bem-sucedida **não** indica entrega; `failed` pode chegar com atraso. Fonte: send-messages.
- **Idempotência no envio:** não encontrei chave de idempotência na API de envio (**NÃO VERIFICADO**). Tratar no AeroZap (outbox + chave própria + dedupe por id retornado).

### 2.7 Mídia e áudio recebidos

- Webhook traz o **id da mídia**; `GET /MEDIA_ID` com token devolve `url`, `mime_type`, `sha256`, `file_size`. Baixar a URL com o token. **URL expira em 5 minutos**; em 404, pedir nova URL. **ID de webhook expira em 7 dias** (ID de upload, 30 dias). Mídia recebida: até 100 MB; acima disso chega erro `131052`. Fonte: https://developers.facebook.com/documentation/business-messaging/whatsapp/business-phone-numbers/media
- Formatos de áudio (envio): AAC, AMR, MP3, M4A, OGG somente codec Opus mono; 16 MB. **Formato exato do áudio de voz recebido (nota de voz) e flag `voice`: NÃO VERIFICADO** nesta rodada; validar com mensagens reais no número de teste e registrar no teste do AZ-043.
- Implicação: baixar o áudio imediatamente na fila (não no webhook), conferir tamanho e MIME, gravar em storage privado com expiração (DTC §6.2) e descartar o conteúdo conforme retenção.

### 2.8 Mensagens interativas

| Tipo | Limites | Fonte |
|---|---|---|
| Botões de resposta | até 3 botões; título 20 caracteres, único; corpo 1.024; rodapé 60; ID 256 | https://developers.facebook.com/documentation/business-messaging/whatsapp/messages/interactive-reply-buttons-messages |
| Lista | até 10 seções e 10 linhas no total; título de seção e de linha 24; descrição 72; botão 20; corpo 4.096 | https://developers.facebook.com/documentation/business-messaging/whatsapp/messages/interactive-list-messages |

Uso previsto: confirmar interpretação de áudio ("Sim / Corrigir"), escolher aeroporto ambíguo, aceitar alertas. Resultados de voos: texto enxuto + link para a Web (a lista de 10 linhas é pequena).

### 2.9 Uso com IA e chatbots (política vigente) — ponto crítico

- **O que é oficial (Meta):** a página de preços de "AI Providers" diz que a definição vem dos *WhatsApp Business Solution Terms* atualizados em **15/01/2026** ("provedores e desenvolvedores de tecnologias de IA/ML", incluindo LLMs, plataformas de IA generativa e assistentes de IA de uso geral). AI Providers só podem oferecer assistentes de uso geral onde a Meta é legalmente obrigada a permitir; no **Brasil (+55) há cobrança por mensagem de serviço de AI Providers desde 11/03/2026**, ainda em vigor. Fontes: https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing/ai-providers.md ; https://developers.facebook.com/documentation/business-messaging/whatsapp/changelog (entrada de 12/05/2026: cobrança no Brasil continua).
- **Cláusula completa: NÃO VERIFICADO diretamente.** A página oficial dos termos (https://www.whatsapp.com/legal/business-solution-terms) retornou à ferramenta uma versão antiga (modificada em 12/06/2018) sem o texto de IA. O texto abaixo vem de fonte **secundária** (https://www.learnmind.ai/blog-post/whatsapp-business-ai-chatbot-2026-policy-guide) e precisa ser conferido pelo jurídico no texto oficial: AI Providers ficam "estritamente proibidos de acessar ou usar" a solução, direta ou indiretamente, **"quando tais tecnologias forem a funcionalidade principal (e não incidental ou acessória)"**; o escopo final é "a critério exclusivo da Meta"; é permitido contratar um AI Provider como prestador terceiro; não se pode permitir que dados da solução sejam usados para treinar ou melhorar modelos de IA, exceto modelo ajustado de uso exclusivo.
- **Análise para o AeroZap (opinião técnica, não parecer):**
  - Função principal = pesquisar, comparar e monitorar passagens. A IA (A3) só interpreta e resume. Isto cabe em "incidental ou acessória", mas **a classificação é da Meta**, a critério dela.
  - Risco de enquadramento sobe se o bot responder perguntas gerais. **Restringir o escopo** do assistente a viagens aéreas; recusar e redirecionar fora dele (reforça também o AGENTS.md regra 12 e a defesa contra prompt injection).
  - **Dados:** conteúdo de conversas do WhatsApp não pode treinar nem melhorar modelos. Contratos com o provedor de LLM e de transcrição (A3) devem vedar treino/retenção com dados do AeroZap. Registrar em ADR.
  - **Escalonamento:** política exige caminho de contato humano claro (e-mail ou formulário bastam). Incluir "ajuda" nas respostas e no perfil do número. Fonte: https://business.whatsapp.com/policy
  - Mensagens de serviço de AI Providers têm preço próprio no Brasil. Se a Meta enquadrar o AeroZap assim, o custo muda: **NÃO VERIFICADO**; perguntar à Meta/BSP (§7).
- **Meta Business Agent** (IA própria da Meta, cobrada por token desde 01/08/2026) não é usado. Fonte: non-template-messages.

### 2.10 Identidade do usuário: BSUID e usernames (impacta AZ-021)

BSUIDs aparecem nos webhooks desde abril/2026; usernames estão sendo liberados ao longo de 2026. **Se o usuário ativar username, o telefone (`wa_id`) pode não vir no webhook**, salvo interação nos últimos 30 dias. O envio por BSUID é suportado a partir de julho/2026; templates de autenticação com um toque exigem telefone. O BSUID é escopo do portfólio e muda se o usuário trocar de número. Fonte: https://developers.facebook.com/documentation/business-messaging/whatsapp/business-scoped-user-ids/
Recomendação: modelo de dados com `phone_e164` (nullable) **e** `bsuid` (por portfólio), vínculo telefone-conta provado por código no fluxo Web/WhatsApp, e tratamento de `previous_user_id`. Avisar o A1 (contrato de vínculo).

### 2.11 Privacidade e dados

- A Meta pode coletar e usar dados da solução (conta, uso, diagnóstico), transferir para os EUA e outros países, e a empresa deve dar os avisos necessários e honrar pedidos de parar/optar por sair. Fonte: https://www.whatsapp.com/legal/WhatsApp-Terms-for-WhatsApp-Business-Platform (efetivo em 23/09/2026).
- Proibido usar dados para criar ou enriquecer perfis de usuários individuais do WhatsApp; prestador terceiro só processa sob instrução da empresa (versão antiga dos termos; reconferir). Fonte: https://www.whatsapp.com/legal/business-solution-terms
- Para o AeroZap/LGPD (DTC §13): política de privacidade deve mencionar o WhatsApp/Meta como operador/controlador e a transferência internacional. **Base legal, papéis (controlador/operador) e retenção: decisão do jurídico (NÃO VERIFICADO).**

---

## 3. Evolution API

| Aspecto | Achado | Fonte |
|---|---|---|
| Modos de conexão | (a) **Baileys**: "API gratuita baseada no WhatsApp Web", não oficial; (b) **Cloud API** (integração `WHATSAPP-BUSINESS`, token, sem QR Code). | https://github.com/evolution-foundation/evolution-api ; https://docs.evolutionfoundation.com.br/en/evolution-api ; https://docs.evolutionfoundation.com.br/evolution-api/integrations/cloudapi |
| Licença | Apache 2.0 **com condições adicionais**: manter logo e copyright, exibir aviso de uso a administradores; descumprir "pode exigir licença comercial". Telemetria de uso de rotas e versão. | https://github.com/evolution-foundation/evolution-api-baileys |
| Versões | Estável mais recente listada: v2.3.7 (data: ano **NÃO VERIFICADO**); 2.4.0-rc1/rc2 em mai/2026 são pré-lançamento ("não usar em produção sem mais testes"). **2.4.0-rc1 exige ativação de licença** (sem ela, HTTP 503 `LICENSE_REQUIRED`). Baileys ainda em 7.0.0-rc (candidata). Já houve falha crítica de path traversal sem autenticação (corrigida na v2.3.3). | https://github.com/evolution-foundation/evolution-api/releases |
| Termos do WhatsApp (Baileys) | Os termos de serviço vedam acessar o serviço por meios não autorizados/automatizados, engenharia reversa e mensagens em massa/automáticas ilegítimas; as diretrizes citam "unofficial clients" e preveem suspensão temporária ou permanente. README e docs da Evolution **não mencionam o risco**. | https://www.whatsapp.com/legal/terms-of-service ; https://www.whatsapp.com/legal/messaging-guidelines |
| Risco de bloqueio | Alto no modo Baileys: suspensão da conta/número é ação prevista. Não há template, janela nem opt-in aplicados pela plataforma. Probabilidade e velocidade de banimento: **NÃO VERIFICADO** (sem dado oficial). | idem |
| Modo Cloud API | Faz proxy da Cloud API: exige BM aprovado, app, número verificado e token permanente; webhook da Meta aponta para `API_URL/webhook/meta`, validado por `WA_BUSINESS_TOKEN_WEBHOOK`. Notas de versão citam endpoints de template da Meta (v2.3.7). A página não detalha mídia, áudio, interativas nem status. **Validação de `X-Hub-Signature-256` pela Evolution: NÃO VERIFICADO.** | docs Cloud API (acima); releases |
| Manutenção | Mantida ativamente (2.629 commits), mas o repositório e a documentação migraram para `evolution-foundation`/`docs.evolutionfoundation.com.br`, e a licença mudou na 2.4. Risco de governança: o modelo comercial futuro é incerto. | releases; README |
| Botões/listas no Baileys | Notas de versão mostram ajustes contínuos ("legacy list messages" para Web/Desktop): recurso interativo instável. | releases |

**O que o modo Cloud API acrescenta:** painel multi-instância, integrações prontas (RabbitMQ, SQS, Chatwoot) e uma API única para os dois modos. **O que não acrescenta:** nada de conformidade, preço ou limite; continua valendo tudo do §2. Em contrapartida adiciona um intermediário que vê todas as mensagens, mais um componente a operar e atualizar, e a dependência de licença.

---

## 4. Comparativo

| Critério | Cloud API (direta) | Evolution — modo Cloud API | Evolution — Baileys |
|---|---|---|---|
| Conformidade com os termos | Sim | Sim (é a mesma Cloud API) | **Não confirmada; risco de violação** |
| Risco de bloqueio do número | Baixo (qualidade e opt-in) | Baixo | **Alto** |
| Templates, janela, opt-in | Impostos e suportados | Idem, suporte parcial na camada | Inexistentes (sem proteção) |
| Custo | Por mensagem (§2.2) | Idem + hospedagem e operação | Sem tarifa Meta; custo de risco |
| Estabilidade | SLA de plataforma, status page oficial | Depende da Meta + Evolution | Depende de engenharia reversa do cliente web |
| Segurança | Assinatura HMAC, mTLS opcional | Camada extra para proteger | Sessão de número no servidor |
| Interativas, status | Documentados | Parcialmente documentados | Instáveis |
| Manutenção | Só versionar a Graph API | Atualizar Evolution, licença, CVEs | Alta |
| Elegibilidade comercial/escala | Verificação, tiers, BRL | Idem | Não escala com segurança |

**Decisão proposta:** Cloud API direta como padrão; Evolution (modo Cloud API) como adaptador de contingência e para ambiente local/demonstração, sem uso em produção por padrão; Baileys vetado.

### Conteúdo da ADR (ADR-0004 sugerida, "Provedor WhatsApp")
1. Decisão: Cloud API oficial direta como adaptador padrão; porta `WhatsAppProvider` mantida.
2. Veto do modo Baileys em produção; exceção só com aceitação de risco assinada pelo Product Owner, número descartável e prazo.
3. Custo: premissas de preço por categoria, meio de pagamento, tabela BRL anexada (valores a coletar), e o cenário "alerta = marketing".
4. Política de IA: escopo do bot restrito a passagens aéreas; leitura jurídica da cláusula de AI Providers; proibição de usar dados do WhatsApp para treino; cláusulas no contrato de LLM/transcrição.
5. Identidade: BSUID + telefone; fluxo de prova de posse.
6. Opt-in/opt-out e retenção de mensagens e áudio (com A8, A1, jurídico).
7. Plano de reversão: troca de adaptador por configuração; sem exposição do provedor ao AI Orchestrator.

---

## 5. Checklist do Product Owner (itens com prazo externo)

| # | Providenciar | Prazo informado em fonte oficial |
|---|---|---|
| 1 | Conta/portfólio Meta Business da empresa (dados legais batendo com os documentos) | Sem prazo informado |
| 2 | **Verificação da empresa** (documentos: lista oficial NÃO VERIFICADO; página de ajuda bloqueada à leitura) | Prazo **NÃO VERIFICADO** |
| 3 | Número de produção dedicado (não pode estar no app WhatsApp; recebe SMS/voz) e número de teste | Sem prazo |
| 4 | Display name escolhido e aprovado | Prazo **NÃO VERIFICADO** |
| 5 | Moeda **BRL**, com Sold-To Brasil, definida na criação (não muda depois) | Disponível desde 01/07/2026 |
| 6 | **Meio de pagamento** cadastrado na Meta (crédito ou cartão) | Obrigatório já em vigor; sem ele, mensagens de serviço não são entregues (desde 01/10/2026) |
| 7 | App Meta, token permanente (usuário do sistema), App Secret e verify token no secret manager | Sem prazo |
| 8 | Templates pt-BR e es enviados para aprovação (alerta de preço, opt-in, janela expirada, boas-vindas) | Decisão em **até 24 h** por template (apelação também 24 h) |
| 9 | Texto de opt-in, política de privacidade, termos e canal de contato humano (escalonamento) | Sem prazo; jurídico |
| 10 | Revisão jurídica da cláusula de IA e da LGPD (papéis Meta/AeroZap, transferência internacional) | Sem prazo |
| 11 | Decidir BSP (provedor de soluções) x integração direta, se desejar suporte; efeito em custo: **NÃO VERIFICADO** | — |
| 12 | Aprovar a ADR de WhatsApp | — |

Observação: o Plano de Implementação (§3) já pede iniciar esses itens na Fase 0. Para o M4, o mínimo é o número de teste, que não exige verificação.

---

## 6. Desenho resumido do adaptador atrás de `WhatsAppProvider` (DTC §7.2)

Sem código; a interface do DTC permanece. Sugestões de acréscimo de contrato (a aprovar pelo CTO): `parseInbound(...)` normalizado e `getMedia(...)`, pois a porta só cobre envio.

```text
Meta ──HTTPS──> Webhook Controller ──> (1) valida assinatura ──> (2) persiste evento bruto ──> (3) 200 OK
                                                                        │
                                                         fila de entrada (RabbitMQ)
                                                                        │
                    Webhook Processor ──> dedupe por id externo ──> normaliza ──> evento interno
                                                  │                                     │
                                       tabela de eventos (UNIQUE)         Conversation Router (A3/A1)
```

1. **Validação (Webhook Controller):** conferir `X-Hub-Signature-256` sobre o **corpo bruto** com o App Secret, comparação em tempo constante; GET de verificação com `verify_token`. Falha = 401/403, métrica e log sem payload. Opcional: mTLS no balanceador. Rate limit por origem. Corpo máximo limitado.
2. **Resposta rápida:** gravar o evento bruto (inclusive os de status) e responder `200`. Nada de IA, banco pesado ou download de mídia na requisição.
3. **Deduplicação:** índice único por (`provider`, id externo da mensagem ou do status+id+estado). A Meta reentrega por até 36 h; evento repetido é descartado sem efeito de negócio (AGENTS.md regra 6). Ordem não é garantida: usar timestamp do provedor e máquina de estados monotônica (`sent→delivered→read`, `failed`).
4. **Fila e falhas:** retentativa com backoff e jitter, DLQ com reprocessamento controlado, prazo de reprocesso menor que a janela de reentrega quando possível. Evento de fila em falha não perde dado, pois o bruto está persistido.
5. **Identificação:** resolver usuário por telefone **e** BSUID (§2.10); desconhecido = fluxo de vínculo com prova de posse; `user_id` nunca vem do payload (AGENTS.md regra 10).
6. **Áudio (pipeline AZ-043):** evento de áudio vai para um worker que (a) obtém a URL pelo id, (b) baixa em até 5 min com o token, (c) valida MIME, tamanho e duração, (d) grava em storage privado com URL assinada e expiração, (e) chama a abstração de transcrição do A3, (f) apaga conforme a retenção. Download com falha = pedir ao usuário que reenvie (erro `131052`). Nunca logar o áudio.
7. **Envio (Notification Service):** outbox + chave de idempotência própria (a API não oferece, §2.6), checagem de janela (24 h): dentro = texto/interativa; fora = template aprovado por idioma; sem opt-in = bloqueia. Tratar `131047`, `131049`, `131056`, `130429`, `131026`, `132015/132016` com ações distintas. Rate limit por telefone e por integração (par remetente-destinatário ≈ 1 msg/6 s). Mensagens em sequência só após `delivered` quando a ordem importar.
8. **Saúde da conexão:** webhooks de qualidade do número e de tier de volume, expiração de token (`190`), erro de assinatura e profundidade da fila, expostos como métricas.
9. **Segredos:** App Secret, verify token e access token no secret manager, mascarados no Admin.
10. **Adaptador Evolution:** mesma porta; no modo Cloud API recebe o webhook da Meta e reenvia ao AeroZap, com autenticação própria entre os dois serviços. Não considerar válida a assinatura Meta se a Evolution não a repassar (NÃO VERIFICADO).

Testes exigidos (Definição de Pronto): assinatura inválida, duplicado, fora de ordem, falha de fila, áudio (formato e expiração), janela e template, e continuidade WhatsApp para Web. Teste real autorizado com número de teste (AGENTS.md regra 13).

---

## 7. Riscos e perguntas em aberto

| # | Risco / pergunta | Severidade (AGENTS.md §8) | Encaminhamento |
|---|---|---|---|
| R1 | Meta pode enquadrar o AeroZap como "AI Provider" (decisão a critério dela) | Alto | Jurídico lê o texto oficial; limitar o bot a passagens aéreas; consultar Meta/BSP; ADR |
| R2 | Texto oficial da cláusula de IA não foi lido diretamente (§2.9) | Médio | Product Owner/jurídico extrai do navegador; A5 atualiza esta avaliação |
| R3 | Custo por mensagem real do Brasil (BRL) não transcrito; mensagens de serviço agora cobradas | Alto (margem, DTC §20.1) | Coletar rate card BRL em CSV e montar custo por usuário/pesquisa com A8 |
| R4 | Alerta de preço pode ser reclassificado como marketing (custo e limite por usuário) | Médio | Template factual; monitorar recategorização; orçar pior caso |
| R5 | Limite inicial de 250 usuários únicos/dia fora da janela trava alertas em escala | Médio | Verificação da empresa cedo; fila com priorização |
| R6 | Telefone pode não vir no webhook (BSUID) e quebra o vínculo telefone-conta | Alto | Ajustar contrato com A1 antes do AZ-021 |
| R7 | Formato real do áudio de voz recebido não confirmado | Médio | Teste com número de teste; decisão do conversor/transcrição com A3 |
| R8 | Prazo de verificação da empresa e do display name desconhecido | Médio | Iniciar na Fase 0; sem prazo prometido |
| R9 | Evolution: licença nova (2.4), telemetria e governança incertas | Médio | Não adotar em produção; reavaliar se virar contingência |
| R10 | Webhook: prazo de resposta e reentrega específica do WhatsApp não confirmados | Médio | Teste real; processar sempre em fila |
| R11 | Meta e documentação mudam por trimestre (próxima janela 01/01/2027) | Baixo | Revisão trimestral desta avaliação |
| R12 | Retenção de mensagens, áudio, transcrição e papel LGPD | Alto | Jurídico e A8 (decisão em aberto no AGENTS.md §3) |

**Perguntas ao Product Owner:** (1) Aceita tornar a Cloud API direta o padrão e vetar o Baileys? (2) Haverá BSP ou integração direta? (3) Qual orçamento mensal de mensagens para alertas? (4) Os alertas fora da janela entram no Free ou só no Premium (regra 4 do AGENTS.md: sem regra diferente por canal)? (5) Quem do jurídico validará a cláusula de IA?

---

## 8. Fontes consultadas (03/11/2026)

Meta for Developers (`developers.facebook.com/documentation/business-messaging/whatsapp/`): `pricing`, `pricing/non-template-messages`, `pricing/ai-providers.md`, `changelog`, `about-the-platform`, `messaging-limits`, `throughput`, `templates/overview`, `templates/template-review/`, `templates/template-categorization`, `messages/send-messages`, `messages/interactive-reply-buttons-messages`, `messages/interactive-list-messages`, `business-phone-numbers/phone-numbers`, `business-phone-numbers/media`, `business-scoped-user-ids/`, `official-business-accounts/`, `getting-opt-in`, `support/error-codes`; `developers.facebook.com/docs/graph-api/webhooks/getting-started/`; `developers.facebook.com/docs/whatsapp/cloud-api/guides/set-up-webhooks/`.
WhatsApp: `business.whatsapp.com/policy`, `whatsapp.com/legal/WhatsApp-Terms-for-WhatsApp-Business-Platform`, `whatsapp.com/legal/meta-terms-whatsapp-business`, `whatsapp.com/legal/business-solution-terms` (retornou versão antiga), `whatsapp.com/legal/terms-of-service`, `whatsapp.com/legal/messaging-guidelines`.
Evolution: `github.com/evolution-foundation/evolution-api` (+ `/releases`, `evolution-api-baileys`); `docs.evolutionfoundation.com.br` (Evolution API, Cloud API).
Fonte secundária (apenas para o texto da cláusula de IA, §2.9): `learnmind.ai`.
Não lidas por bloqueio: páginas de ajuda `facebook.com/business/help/*` (verificação da empresa e display name); página interativa de tarifas.
