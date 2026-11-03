# Decisões em aberto — AeroZap (AZ-002)

**Autor:** A0 CTO Master, com A8 · **Data:** 03/11/2026 · **Status:** parcialmente decidido (D-02 e D-12 em 03/11/2026); demais aguardam o Product Owner
**Base:** `AGENTS.md` §3, PRD v1, relatórios AZ-004, AZ-005 e AZ-006, ADRs 0001 a 0004.

Cada decisão traz opções, recomendação, o que acontece se você não decidir e o que ela bloqueia. **Nada é decidido pelos agentes**: onde há recomendação padrão, ela vale apenas como premissa provisória, implementada de forma configurável.

## Como responder
Responda por ID, por exemplo: "D-02 aceito; D-03 aceito; D-06 opção (a) + (c); D-08 aceito e vou contratar BSP". O CTO registra as respostas, marca as ADRs como aprovadas e atualiza este arquivo.

## O mínimo para liberar a Fase 1 — **CUMPRIDO em 03/11/2026**
1. ~~Aprovar as ADRs 0001, 0002, 0003 e 0004~~ — **aprovadas pelo Product Owner** (D-02, D-12 e regra de franquia).
2. ~~Informar onde ficará o repositório remoto~~ — **`https://github.com/marciobsb31/aerozap2027`**.

Todo o restante pode andar em paralelo com as premissas provisórias abaixo.

## O que começar agora (tem prazo externo, não depende de código)
| Ação | Decisão | Por quê |
|---|---|---|
| Conta Travelpayouts e token; candidatura Skyscanner; contato com vendas da Amadeus | D-06 | Resposta da Skyscanner leva até 2 semanas; os demais prazos não são informados |
| Portfólio Meta Business, verificação da empresa, número de teste e de produção, meio de pagamento | D-08 | Prazo de verificação e de display name não informado pela Meta |
| Conta sandbox no gateway; definir entidade que vende (CNPJ ou CPF) e contador | D-09 | Prazo de aprovação da conta de produção não verificado |
| Arquivo da logo | D-01 | UI final |
| Chave de API de LLM e de transcrição, com teto mensal de gasto | D-13 | AZ-019 |
| Revisão jurídica: cláusula de IA da Meta, LGPD, termos dos provedores | D-08, D-11 | Começar na Fase 0 evita travar o M8 |

## Resumo

| ID | Decisão | Recomendação | Quando |
|---|---|---|---|
| D-01 | Logo original | Enviar SVG e PNG; placeholder até lá | Antes do M3 |
| D-02 | Versões Java e Angular | **DECIDIDO 03/11/2026:** ADR-0003 aprovada (Java 25 LTS + Spring Boot 4.1.x, fallback Java 21) | ~~Libera a Fase 1~~ feito |
| D-03 | Trimestre do Free | Trimestre-calendário, fuso America/Sao_Paulo, configurável | Antes de AZ-022 |
| D-04 | "Pesquisa concluída com sucesso" | Opção B: parcial com oferta consome; zero resultados não | Antes de AZ-022 |
| D-05 | Planos e preços | Configuráveis; fixar o preço após medir o custo por pesquisa (M3) | Antes de AZ-070 |
| D-06 | **Provedores de voos** | **(a) Aviasales como "preço indicativo" + (c) ampliar pesquisa**; candidatar-se à Skyscanner e à Amadeus já | **Agora** |
| D-07 | Milhas | Fora do lançamento | Fim da Fase 0 |
| D-08 | **WhatsApp** | Cloud API direta; vetar Baileys; tratar o risco da política de IA | **Agora** |
| D-09 | **Gateway de pagamento** | Stripe nº 1, Asaas plano B (se Pix recorrente for obrigatório, inverte) | Conta: agora |
| D-10 | Escala-alvo e orçamento | Metas de beta; revisar após o M5 | Antes de AZ-080 |
| D-11 | Retenção e LGPD | Tabela proposta, configurável; revisão jurídica | Antes do M4 |
| D-12 | Estrutura do repo, deployables, Git remoto | **DECIDIDO 03/11/2026:** ADR-0001 e ADR-0002 aprovadas; remoto `github.com/marciobsb31/aerozap2027` | ~~Libera a Fase 1~~ feito |
| D-13 | LLM e transcrição, teto de gasto | Chave de teste + teto; escolha por avaliação em pt/es | Fase 1 |
| D-14 | Hospedagem de produção | Decidir na Fase 3, com dois candidatos de A6 | Fase 3 |
| D-15 | Escopo excluído e beta fechado | Confirmar o escopo; decidir o beta após o M4 | Após o M4 |
| D-N1 a D-N7 | Decisões novas do PRD | Padrões do PRD (tabela ao final) | Antes das tarefas citadas |

---

## D-01 — Logo original e identidade visual

- **Situação:** o arquivo da logo não foi fornecido; o DTC exige preservar a logo e o slogan "Search. Compare. Choose. Travel.".
- **Opções:** (a) enviar o arquivo agora; (b) seguir com placeholder identificado e substituir depois.
- **Recomendação:** (a). Preferir SVG (vetor) e PNG de alta resolução, com versões para fundo claro e escuro e, se existir, guia de marca (cores e tipografia).
- **Se não decidir:** A2 usa placeholder rotulado e não redesenha a marca; a UI não pode ser considerada final.
- **Bloqueia:** nada na Fase 1; necessário antes do marco M3 (primeira exposição pública da Web).
- **Prepara:** A2 + A8. **Ação do Product Owner:** enviar o arquivo.

## D-02 — Versões de Java, Spring Boot e Angular

- **Situação:** DTC pede Java 21 ou 25 e Angular em versão suportada.
- **Fatos verificados (03/11/2026):** Spring Boot 4.1.2 aceita Java 17 a 27; Angular 20 sai do suporte em 28/11/2026. Fontes na ADR-0003.
- **Opções:** Java 21 ou Java 25.
- **Recomendação:** **Java 25 LTS + Spring Boot 4.1.x**, com spike de compatibilidade em AZ-014 e fallback para Java 21. Angular: major estável mais recente com pelo menos 12 meses de suporte restante.
- **Se não decidir:** vale a recomendação, por ser reversível.
- **Bloqueia:** AZ-014. **Prepara:** A1 + A6. **Detalhe:** ADR-0003.

## D-03 — Trimestre do plano Free

- **Situação:** 5 consultas por trimestre, saldo compartilhado entre canais. O DTC propõe trimestre-calendário, sujeito a confirmação.
- **Opções:**
  - (a) **Trimestre-calendário** (jan–mar, abr–jun, jul–set, out–dez): renovação igual para todos, simples de operar e de explicar. Contra: quem cria conta no fim do trimestre recebe pouco tempo de uso, e quem usa tudo no fim do trimestre recupera as 5 consultas dias depois.
  - (b) **Janela móvel de 3 meses desde o cadastro:** mais justo por usuário. Contra: renovação individual, mais complexo no suporte e nos relatórios.
- **Recomendação:** (a), com os limites de trimestre calculados no fuso **America/Sao_Paulo**, regra de início/fim documentada na tela de consumo e **período configurável** no Admin. A quantidade (5) também fica configurável.
- **Se não decidir:** vale (a).
- **Bloqueia:** AZ-022. **Prepara:** A8 + A1.

## D-04 — O que é "pesquisa concluída com sucesso" (consome franquia?)

- **Situação:** o DTC diz que falha não consome, mas também pede "degradação controlada" (devolver os provedores que responderam). Não está definido o que acontece com resultado parcial e com zero resultados.
- **Cenários:**

| Cenário | Opção A | Opção B (recomendada) | Opção C |
|---|---|---|---|
| Falha interna ou de todos os provedores | Não consome | Não consome | Não consome |
| Parcial: ao menos uma oferta, algum provedor falhou | Não consome | **Consome** (informando as fontes que falharam) | Consome |
| Todos responderam, zero ofertas | Não consome | **Não consome** | Consome |
| Todos responderam, com ofertas | Consome | Consome | Consome |

- **Prós e contras:** A é a mais amigável, mas um provedor instável viraria Free ilimitado. C é a mais simples e previsível, mas cobra por resultado vazio ou incompleto e gera reclamação. B cobra só por valor entregue e protege contra o abuso de A; exige métrica de falha por provedor.
- **Recomendação:** **B** (a mesma do PRD, RN-12). As chaves de configuração são `usage.consume_on_partial` (`NEVER` / `IF_ANY_OFFER` / `ALWAYS`) e `usage.consume_on_zero_results` (`NEVER` / `ALWAYS`); falha total nunca consome. Veja ADR-0004.
- **Se não decidir:** vale A.
- **Bloqueia:** AZ-022 e AZ-032 (a política é configurável, então não impede o início). **Prepara:** A8 + A1 + A4.

## D-05 — Planos e preços

- **Situação:** hipótese do DTC §3.3 — mensal R$ 39,90; semestral R$ 179,40 (equivale a R$ 29,90/mês); anual R$ 297,00 (R$ 24,75/mês, interpretação "R$ 297,00 pelo ano em 12 × R$ 24,75"). Não é tabela aprovada.
- **Pontos a confirmar:** o anual é R$ 297,00 à vista ou 12 × R$ 24,75? Há parcelamento? Qual a política de renovação, cancelamento, reembolso e período de tolerância na inadimplência?
- **Fato que falta:** o **custo por pesquisa** (provedor + IA + transcrição) só será conhecido depois que o provedor nº 1 for definido e medido (M3). Preço sem esse número é aposta de margem.
- **Recomendação:** lançar com planos **totalmente configuráveis no Admin**, usar a hipótese como ponto de partida, e **validar o preço mínimo com o custo por pesquisa medido no M3**. Decidir o preço final antes de AZ-070 (checkout).
- **Se não decidir:** A1 constrói o modelo sem valores fixos; nada bloqueia até a Fase 7.
- **Bloqueia:** AZ-070. **Prepara:** A8 (com dados de A4).

## D-06 — Provedores de voos (a decisão mais importante da Fase 0)

- **Situação:** relatório completo em `docs/architecture/providers/relatorio-avaliacao-provedores.md`. **Nenhum provedor está contratado.**
- **O que a pesquisa encontrou:**

| Provedor | Situação | Fonte |
|---|---|---|
| Amadeus Self-Service | Descontinuado em 17/07/2026. Corroborado por imprensa especializada (PhocusWire e outras), mas **o aviso oficial no portal não pôde ser lido**. Confirme você mesmo em developers.amadeus.com | Imprensa (secundária) |
| Kiwi/Tequila | Novas parcerias "somente por convite" desde 30/05/2024 | Oficial Kiwi |
| Duffel | Plataforma para **vender** passagens; o contrato veda uso como metabusca (interpretação a confirmar com o jurídico) | Contrato Duffel |
| Travelpayouts/Aviasales | Acesso por token e aceite do acordo de afiliado. **Preços em cache de 2 a 7 dias, não reserváveis.** Busca em tempo real só por aprovação, com metas de conversão (9% busca→clique, 5% clique→compra) | Oficial |
| Skyscanner Travel API | Tempo real, até 6 trechos, bagagem e condições. **Exige acordo comercial e audiência acima de 100 mil visitas/mês**; resposta à candidatura em até 2 semanas | Oficial |
| Amadeus Enterprise (Quick-Connect) | Via vendas, voltado a quem vende viagem; elegibilidade para comparador e preço **não verificados** | Oficial (parcial) |

- **O que isso muda no produto (ponto crítico):** no lançamento, o AeroZap provavelmente só poderá oferecer **"preços indicativos"** (encontrados recentemente, não reserváveis), e não ofertas ao vivo. As ofertas continuam vindo de fonte real, mas "real" não é "ao vivo". Isso afeta: (1) os filtros por horário, bagagem e condições tarifárias, que dependem do que o provedor devolve; (2) o cartão de oferta, cuja "última verificação" será a data do cache; (3) os alertas, que só poderão usar dados em cache; (4) a expectativa do usuário, que deve ser comunicada com clareza ("preço indicativo, sujeito a alteração").
- **Opções:**
  - **(a)** Começar com a Aviasales Data API como "preços indicativos", com link ao vendedor, e buscar tempo real depois (aprovação da Travelpayouts ou Skyscanner ao atingir a audiência).
  - **(b)** Só iniciar a pesquisa real quando houver Skyscanner ou Amadeus Enterprise. Risco de atraso sem prazo, pois o critério de audiência pode barrar um produto novo.
  - **(c)** Antes de decidir, ampliar a pesquisa para provedores não avaliados (Sabre, Travelport, Kayak e outros agregadores com API de parceiros).
- **Recomendação:** **(a) + (c) em paralelo**, e já hoje enviar a candidatura à Skyscanner e contatar vendas da Amadeus. O marco M3 passa a ser aceito como "preço indicativo, rotulado", e o PRD (RF-022 a RF-036) é ajustado pelo A8 e pelo A4 conforme o provedor escolhido. Antes de integrar, obter **por escrito** da Travelpayouts: (1) o uso comercial em SaaS multicanal com resposta no WhatsApp e armazenamento de histórico; (2) cobertura de rotas do Brasil e suporte a BRL (o padrão é rublo); (3) como a busca em tempo real se aplica a um produto novo.
- **Se não decidir:** A4 desenvolve contra o adaptador DEMO (AZ-030), rotulado em toda a interface; o M3 só vale como DEMO.
- **Bloqueia:** AZ-031 e o escopo final de AZ-037. **Prepara:** A4 + A8. Quando você decidir, o CTO registra a **ADR-0006**.
- **Ação do Product Owner:** criar conta e token na Travelpayouts; candidatar-se à Skyscanner; contatar vendas da Amadeus; ter CNPJ e site público; revisão jurídica dos termos; confirmar o encerramento do Amadeus Self-Service no portal oficial.

## D-07 — Milhas

- **Situação:** não foi encontrada fonte oficial e autorizada de disponibilidade de resgate (Smiles, LATAM Pass, TudoAzul) acessível a um SaaS. A única ferramenta encontrada (seats.aero) exige aprovação escrita para uso comercial e não confirma LATAM Pass, Smiles nem TudoAzul. Raspagem não é fonte autorizada.
- **Opções:** (a) fora do lançamento; (b) negociar por escrito com seats.aero e com cada programa antes de qualquer promessa.
- **Recomendação:** **(a)**. Manter a porta `MilesProvider` desabilitada e **nunca** converter tarifa em pontos. Abrir (b) só se milhas for prioridade comercial.
- **Se não decidir:** vale (a); os requisitos de milhas do PRD (RF-030 e RF-039) ficam sem marco.
- **Prepara:** A4.

## D-08 — WhatsApp: adaptador, custo e política de IA

- **Situação:** relatório em `docs/architecture/whatsapp/avaliacao-whatsapp.md`.
- **Recomendação:** **Cloud API oficial da Meta, integração direta**, como padrão de produção. O Evolution API só como alternativa, no modo Cloud API. O modo Baileys (WhatsApp Web, não oficial) fica **vetado** em produção, salvo ADR com aceitação formal de risco sua.
- **Pontos críticos encontrados:**
  1. **Política de IA da Meta.** Desde 15/01/2026 os termos restringem "AI Providers" (assistentes de IA de uso geral), a critério da Meta, quando a IA é a funcionalidade principal. O AeroZap é, em princípio, busca de passagens com IA acessória, mas **quem classifica é a Meta**. A mudança foi noticiada por vários veículos (por exemplo TechCrunch e 9to5Mac, em outubro de 2025), mas o texto oficial da cláusula **não foi lido** (a página devolveu uma versão antiga); o resumo vem de fonte secundária. **Mitigações:** restringir o assistente a viagens aéreas, vetar uso dos dados do WhatsApp para treinar modelos nos contratos de LLM e transcrição, e manter canal humano de contato. O jurídico precisa ler o texto oficial.
  2. **Custo.** Desde 01/10/2026 as mensagens de serviço (respostas na janela de 24 h) são cobradas após as primeiras 1.000 por número por mês, e é preciso meio de pagamento cadastrado na Meta. Os valores em BRL não foram transcritos; precisam ser coletados do rate card oficial.
  3. **Alertas de preço fora da janela** exigem template aprovado. A Meta pode classificar como *utility* ou como *marketing* (mais caro, com limite por usuário). Orçar o pior caso.
  4. **Limite inicial** de 250 usuários únicos por dia fora da janela, até verificar a empresa.
  5. **Identidade.** Com *usernames*, o telefone pode não vir no webhook (BSUID). O vínculo telefone-conta (AZ-021) precisa suportar os dois.
- **Perguntas para você:** (1) aceita a Cloud API direta como padrão e o veto ao Baileys? (2) integração direta ou por um BSP (provedor de soluções)? (3) qual orçamento mensal de mensagens para alertas? (4) alertas fora da janela serão do Free ou só do Premium (ligado à D-N3)? (5) quem do jurídico lê a cláusula de IA?
- **Se não decidir:** A5 constrói sobre a porta `WhatsAppProvider` com o número de teste da Meta; nada bloqueia até o M4, mas verificação da empresa e templates têm prazo **não informado** pela Meta: comece agora.
- **Bloqueia:** AZ-040 (conta de teste), AZ-052 (templates). **Prepara:** A5. Quando você decidir, o CTO registra a **ADR-0005**.
- **Ação do Product Owner:** portfólio Meta Business, verificação da empresa, número de produção e de teste, moeda BRL, meio de pagamento, textos de opt-in e política de privacidade.

## D-09 — Gateway de pagamento

- **Situação:** relatório em `docs/architecture/billing/avaliacao-gateway-pagamento.md` (Stripe, Mercado Pago, Asaas, Pagar.me, Iugu e Vindi; Paddle como pista).
- **Recomendação:** **Stripe** (Checkout hospedado, modo assinatura, cartão) como nº 1, atrás de uma porta `PaymentGateway`. **Asaas** como plano B. **Mercado Pago** para uma eventual fase 2 na América Latina. Pagar.me, Iugu e Vindi não foram recomendados por falta de evidência verificável, não por demérito comprovado.
- **A pergunta que decide:** **Pix recorrente ou boleto são obrigatórios no lançamento?** Se sim, o Asaas passa a nº 1.
- **Fatos que pesam:**
  1. **Stripe com conta no Brasil não tem Pix Automático** (só Pix avulso); boleto existe, mas sem cobrança automática recorrente.
  2. **Repasse do cartão doméstico:** 30 dias no Stripe; D+32 no Asaas. Isso afeta seu caixa.
  3. **Nota fiscal:** o Stripe não emite NFS-e do seu serviço; o Asaas emite pela API. Isso exige um contador definindo município, inscrição e regime.
  4. **Asaas e cartão:** enviar o cartão pela API deixaria o backend do AeroZap no escopo PCI, contra a regra de não armazenar dados de cartão; só dá para usar checkout hospedado. O webhook do Asaas usa token fixo (sem assinatura criptográfica documentada).
  5. **Taxas** (Stripe: cartão doméstico 3,99% + R$ 0,39; Asaas: cartão à vista 2,99% + R$ 0,49) vêm das páginas oficiais, **sem data de vigência** em parte delas. Confirmar por escrito.
- **Perguntas em aberto (relatório §7):** parcelamento do semestral e do anual (cobrança única, parcelamento no cartão ou recorrência mensal com compromisso anual); renovação automática e aviso prévio; troca de plano no meio do ciclo; política de estorno; **período de tolerância na inadimplência** (padrão provisório: 0 dias); disputa e chargeback; mudança de preço para assinantes atuais; cupons e período de teste; moeda para o público hispanofalante; destino do saldo Free ao assinar no meio do trimestre; retenção dos registros de cobrança.
- **Se não decidir:** A1 constrói o domínio de assinatura e a porta `PaymentGateway` sem adaptador concreto; tolerância 0 dias; nada bloqueia até a Fase 7, mas **abrir a conta e o sandbox leva prazo não verificado**.
- **Bloqueia:** AZ-070. **Prepara:** A1 + A8. Quando você decidir, o CTO registra a **ADR-0007**.
- **Ação do Product Owner:** definir a entidade que vende (CNPJ ou CPF), representante legal com CPF, conta bancária em BRL; abrir a conta sandbox; contratar contador para NFS-e; jurídico para termos, cancelamento e reembolso; confirmar as taxas por escrito.

## D-10 — Escala-alvo e orçamento de infraestrutura

- **Situação:** o DTC não fixa números de usuários simultâneos ou pesquisas por minuto, e orienta não prometer capacidade sem teste de carga.
- **O que preciso do Product Owner:** (1) usuários ativos esperados em 6 e 12 meses; (2) pico de pesquisas simultâneas esperado; (3) orçamento mensal de infraestrutura, provedores e IA; (4) disponibilidade mínima aceitável.
- **Recomendação:** definir metas de **beta fechado** primeiro e revisá-las após o M5. Os SLOs de referência do DTC (99,5% mensal; aceitação de pesquisa p95 abaixo de 2 s) permanecem como metas a validar.
- **Se não decidir:** A6 e A7 usam um perfil provisório proposto por A0 na AZ-080; nenhuma capacidade é prometida.
- **Bloqueia:** AZ-080 (não antes). **Prepara:** A6 + A7.

## D-11 — Retenção de dados e LGPD

- **Situação:** o DTC exige política explícita de retenção para mensagens, áudios, transcrições, logs e histórico, e revisão jurídica.
- **Proposta de ponto de partida (a validar com o jurídico, não é parecer):**

| Dado | Retenção proposta |
|---|---|
| Áudio | Apagar logo após a transcrição; no máximo 24 h em caso de falha |
| Transcrição | 30 dias, para suporte e correção de interpretação |
| Contexto de conversa para a IA | Expira após inatividade (por exemplo 24 h) |
| Mensagens | 90 dias |
| Histórico de pesquisas e alertas | Enquanto a conta existir; exclusão a pedido |
| Logs de aplicação | 30 dias, sem dados pessoais desnecessários |
| Auditoria administrativa | Prazo definido com o jurídico |

- **Recomendação:** adotar a tabela como ponto de partida técnico, com prazos **configuráveis**, e contratar revisão jurídica da política de privacidade, dos termos de uso e das bases legais **a partir da Fase 4** (áudio e WhatsApp).
- **Se não decidir:** A1 e A5 implementam retenção configurável com estes padrões; o lançamento público fica condicionado à revisão jurídica (AZ-081).
- **Bloqueia:** AZ-043 (áudio) para valores finais; M8. **Prepara:** A8 + A1 + jurídico.

## D-12 — Estrutura do repositório, deployables e hospedagem do Git

- **Situação:** ADR-0001 (estrutura) e ADR-0002 (cinco deployables) propostas. O diretório não é um repositório Git.
- **Recomendação:** aprovar as duas ADRs. Sobre onde hospedar o Git: o DTC sugere GitHub Actions ou equivalente para CI/CD; **GitHub** é a opção de menor atrito, mas a escolha é sua.
- **O que preciso do Product Owner:** aprovar as ADRs e informar onde criar o repositório remoto (organização/conta) e quem tem acesso.
- **Decidido em 03/11/2026:** ADR-0001 e ADR-0002 aprovadas; remoto `https://github.com/marciobsb31/aerozap2027`.
- **Nota:** o primeiro push exige credenciais do GitHub (a conta ainda não está vinculada à sessão do Claude).

## D-13 — Provedor de LLM e de transcrição, e teto de gasto

- **Situação:** a IA usa abstração de provedores de LLM e de transcrição (A3). É preciso um provedor inicial, chave de API e teto de gasto.
- **Opções:** escolher um provedor de LLM e um de transcrição por avaliação em português e espanhol (AZ-019), mantendo a abstração para trocar.
- **Recomendação:** o Product Owner fornece chave de teste e define um **teto mensal de gasto**; A3 compara candidatos no conjunto dourado pt/es e recomenda com custo e latência medidos. A escolha final sai da AZ-019.
- **Se não decidir:** A3 desenvolve contra um provedor simulado e o conjunto dourado fica sem métricas reais.
- **Bloqueia:** métricas de AZ-019. **Prepara:** A3. **Ação do Product Owner:** chave e teto.

## D-14 — Hospedagem de produção, domínio e orçamento

- **Situação:** nenhuma definição. O DTC indica Docker Compose para desenvolvimento e plataforma de containers (Kubernetes ou serviço gerenciado) para produção, sem exigir Kubernetes no primeiro deploy.
- **Recomendação:** adiar a escolha até a Fase 3, com dois candidatos apresentados por A6 (um serviço gerenciado de containers e uma VM com Compose para o beta), comparados por custo e operação. Registrar o domínio e a conta de hospedagem a partir da Fase 3.
- **Se não decidir:** nada bloqueia até a Fase 3. **Prepara:** A6.

## D-15 — Escopo fora do plano e beta fechado

- **Situação:** o plano propõe deixar fora de escopo: emissão e reserva de passagens (o AeroZap leva ao link de compra), app móvel nativo, outros canais além de WhatsApp e Web, e conversão automática de tarifa em milhas. Também propõe a opção de beta fechado com usuários Free após o M5.
- **Recomendação:** confirmar o escopo excluído; sobre o beta, decidir após o M4 com base na experiência e no custo por pesquisa medido.
- **Se não decidir:** vale o escopo do plano; beta fica em aberto.
- **Prepara:** A8 + A0.
## Decisões descobertas na escrita do PRD (D-N1 a D-N7)

Registradas por A8 no PRD v1 (§10). Cada uma tem uma recomendação padrão que vale se você não decidir; nenhuma bloqueia a Fase 1.

| ID | Pergunta | Recomendação de A8 | Bloqueia |
|---|---|---|---|
| D-N1 | O visitante sem conta pode pesquisar? | Exigir cadastro para executar a pesquisa (a franquia é por usuário); o visitante vê o formulário. Alternativa: uma busca DEMO | AZ-037 |
| D-N2 | Número de WhatsApp não vinculado a conta | Orientar a criar ou vincular a conta, sem pesquisar | AZ-044 |
| D-N3 | Alertas no plano Free e limites por plano | O DTC cita alertas só no Premium. Recomendado: limite pequeno no Free, definido depois de medir o custo por alerta | AZ-050 |
| D-N4 | Refinar uma pesquisa consome franquia? | Filtrar, ordenar e reabrir uma pesquisa do histórico não consome; mudar rota, datas ou passageiros cria nova pesquisa e consome (RN-13) | AZ-022 |
| D-N5 | Variante do espanhol | Espanhol neutro latino-americano, com revisão por falante nativo | AZ-016 |
| D-N6 | Modelo de receita por comissão e rótulo "Patrocinado" | Define se o aviso de comissão aparece. Depende da escolha do provedor (D-06): a Travelpayouts remunera por comissão sobre a venda | AZ-037 |
| D-N7 | Moeda de cobrança e troca de modalidade no meio do período | v1 em BRL; regras de troca dependem do gateway (D-09) | AZ-070 |

---

## ADRs que dependem das suas respostas
| ADR | Assunto | Depende de |
|---|---|---|
| ADR-0005 | Provedor WhatsApp e política de IA | D-08 |
| ADR-0006 | Provedor de voos e "preço indicativo" | D-06 |
| ADR-0007 | Gateway de pagamento (porta `PaymentGateway`) | D-09 |

Os relatórios de origem já trazem o conteúdo sugerido para cada uma; o CTO as redige quando você decidir.
