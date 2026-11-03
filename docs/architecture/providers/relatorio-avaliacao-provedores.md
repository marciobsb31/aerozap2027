# AZ-004 — Relatório de avaliação de provedores de voos

**Autor:** A4 Flight Engine Specialist · **Data:** 03/11/2026 · **Status:** proposta ao CTO (A0) e ao Product Owner · **Sem código de produção**
**Escopo:** Amadeus (Self-Service e Enterprise), Travelpayouts/Aviasales, Kiwi.com/Tequila, Skyscanner e alternativas (Duffel; seats.aero apenas para milhas).

## 0. Como ler este relatório

- **Nenhum provedor é considerado contratado ou disponível.** Tudo abaixo é avaliação documental feita em 03/11/2026.
- **NÃO VERIFICADO** = não encontrei a informação em fonte oficial legível. Não foi preenchido por suposição. Preços, cotas, prazos e cobertura só aparecem quando a fonte oficial os traz.
- Limitação da pesquisa: as páginas do portal `developers.amadeus.com` retornaram apenas metadados à ferramenta de leitura (conteúdo renderizado por JavaScript). O encerramento do Amadeus Self-Service foi confirmado por imprensa especializada e por citação do aviso do portal em terceiros, não pela leitura direta do aviso oficial. Tratar como **fonte secundária**.
- Fonte marcada "[3ª]" = terceiro (blog, imprensa, concorrente), usada só como indício, nunca como prova de preço ou cota.
- Nenhuma credencial foi solicitada nem usada. Nenhum teste real foi feito (AGENTS.md §2.13).

## 1. Resumo executivo

| Item | Conclusão |
|---|---|
| Provedores descartados | **Amadeus Self-Service** (encerrado), **Kiwi/Tequila** (somente por convite), **Duffel** (motor de venda/emissão; termos vedam metabusca), **seats.aero** (uso comercial exige aprovação escrita; só milhas) |
| Provedor nº 1 (condicional) | **Travelpayouts / Aviasales Data API** (única opção com acesso por token e sem barreira de tráfego declarada) |
| Plano B | **Skyscanner Travel API** (melhor aderência técnica, mas acesso por acordo comercial e critério de audiência >100 mil visitas/mês) |
| Contingência | **Amadeus Enterprise (Quick-Connect)**, via vendas; voltado a vendedores de viagem; elegibilidade para comparador NÃO VERIFICADA |
| Milhas | **NÃO VERIFICADO**. Não prometer ao usuário (seção 5) |

Achados que afetam requisitos do produto (informar ao CTO, conforme definição do A4):

1. **Barreira de entrada:** os dois provedores com dados "ao vivo" e deeplink de reserva (Skyscanner, busca em tempo real da Travelpayouts) exigem aprovação e metas (tráfego ou conversão) que um produto novo provavelmente não cumpre no lançamento.
2. **Alertas de preço (DTC §2.2) versus busca ao vivo:** Skyscanner e Travelpayouts proíbem chamadas automáticas (sem ação do usuário) na busca ao vivo. O monitoramento só pode usar dados em cache (Data API / Indicative), que não são reserváveis.
3. **Dado em cache não é preço reservável:** Aviasales Data API guarda 2 a 7 dias; Skyscanner Indicative até 4 dias. A UI deve exibir `lastCheckedAt` e rotular "preço indicativo, sujeito a alteração".
4. **Moeda:** a Aviasales devolve preços em rublos por padrão. Conversão para BRL deve registrar taxa e fonte (regra do A4); suporte nativo a BRL NÃO VERIFICADO.
5. **Deeplink em mensagens:** Skyscanner proíbe deeplink direto a companhia/OTA a partir de "banner, newsletter ou outro material promocional". Alertas no WhatsApp não devem trazer deeplink direto; devem levar à Web do AeroZap.

## 2. Descartados, com justificativa

| Provedor | Motivo | Fonte |
|---|---|---|
| **Amadeus Self-Service** | Portal self-service descontinuado. Cadastros novos pausados; chaves desativadas e portal inacessível em 17/07/2026 (hoje: encerrado). O portal passou a servir só a API Enterprise. Enterprise não foi afetada. | [3ª] https://www.phocuswire.com/amadeus-shut-down-self-service-apis-portal-developers · [3ª] https://alarabiatravel.blogspot.com/2026/02/the-weekend-wrap-up-amadeus-shutters.html · [3ª] https://github.com/abhinavmathur-atlan/mcp-travel-assistant/issues/4 (cita o aviso do portal) |
| **Kiwi.com / Tequila** | Em 30/05/2024 a Kiwi informou que novas parcerias na Tequila serão "somente por convite" e que o foco é em parceiros-chave. Acesso público à API e ao white label deixou de ser descrito como disponível. A página de ajuda da Travelpayouts sobre a API da Kiwi exige 50.000 MAU (página sem data; status NÃO VERIFICADO). | https://media.kiwi.com/articles-and-interviews/better-for-business-kiwi-com-takes-a-new-approach-to-partnerships/ · https://support.travelpayouts.com/hc/en-us/articles/360019237899-Kiwi-com-affiliate-program-API |
| **Duffel** | É plataforma para **vender/emitir** passagens (busca, seleção, pedido com pagamento). O AeroZap não vende (plano §10). O contrato proíbe uso como metabusca (cl. 2.5(d)), limita a razão busca/pedido (cl. 2.3; taxa excedente a partir de 1.500 buscas por pedido) e proíbe pedidos especulativos (cl. 2.5(j)). Um comparador sem pedidos esbarra nesses limites. Interpretação a confirmar com o jurídico. | https://duffel.com/services-agreement · https://duffel.com/pricing · https://duffel.com/docs/guides/quick-start |
| **seats.aero** (só milhas) | API Pro é para uso pessoal não comercial; "uso comercial ou de produção não é permitido sem aprovação escrita explícita"; 1.000 chamadas por dia; não suporta LATAM Pass; Smiles e TudoAzul não são mencionados. | https://docs.seats.aero/article/68-seatsaero-pro-api-access-limits-and-usage · https://seats.aero/terms · https://docs.seats.aero/article/80-does-seats-aero-support-latam-or-latam-pass-awards |

Não avaliados por falta de tempo (listar como pesquisa futura): Sabre, Travelport, Kayak e outros agregadores com API de parceiros. Não foram encontradas, nesta rodada, alternativas específicas para a América Latina com acesso aberto.

## 3. Avaliação por provedor

### 3.1 Travelpayouts / Aviasales (candidato nº 1, condicional)

1. **Acesso comercial:** a Data API usa token da conta e exige "partner agreement" aceito. Os requisitos dizem "access is allowed without restrictions, but you must have a token". Busca de passagens em tempo real exige solicitação por e-mail, aprovada afiliado a afiliado, com taxas mínimas de conversão (9% busca para clique em "Book" e 5% clique para compra). Prazo de aprovação: NÃO VERIFICADO (não informado). Fontes: https://support.travelpayouts.com/hc/en-us/articles/203956083-Requirements-for-Aviasales-data-API-access · https://support.travelpayouts.com/hc/en-us/articles/204529267-FAQ-about-Aviasales-API · https://support.travelpayouts.com/hc/en-us/articles/210995808-Requirements-for-Aviasales-Flight-Search-API-access
2. **Sandbox e custo de entrada:** sandbox NÃO VERIFICADO. Há um playground do GraphQL (exige token): https://support.travelpayouts.com/hc/en-us/articles/4417975783314-Aviasales-GraphQL-for-access-to-Flights-Data-API. Custo da API: não encontrei cobrança; modelo é de afiliado (item 4).
3. **Cobertura:** a Data API tem parâmetro `market`; se indeterminado, assume `ru`. Brasil, LatAm e low-cost: NÃO VERIFICADO. A Travelpayouts diz ter afiliados no Brasil, o que não prova cobertura de rotas. Fontes: https://support.travelpayouts.com/hc/en-us/articles/203956163-Aviasales-Data-API · https://www.travelpayouts.com/en/offers/aviasales-affiliate-program/
4. **Limites e preço:** limites por minuto (desde 14/06/2024): `/v3/prices_for_dates` 600; `/v3/get_latest_prices` 300; `/v1/prices/calendar` 300; `/v2/prices/month-matrix` 300; `/v2/prices/week-matrix` 60; GraphQL 60; excesso gera 429. Busca em tempo real: padrão de 200 consultas por hora por IP de usuário (alterável). Remuneração: 1,1% do preço do bilhete (40% dos 2,2% que a Aviasales recebe), faixa citada 1,1% a 1,3%; só reservas confirmadas; cookie de 30 dias; pagamento mensal entre os dias 10 e 20. Fontes: https://support.travelpayouts.com/hc/en-us/articles/4402565416594-API-rate-limits · https://support.travelpayouts.com/hc/en-us/articles/205895848-What-are-the-restrictions-on-API-requests · https://www.travelpayouts.com/en/offers/aviasales-affiliate-program/
5. **Exibição, atribuição, cache e redistribuição:** a Data API é recomendada para "informar usuários e gerar páginas de conteúdo"; cache do lado deles de 2 a 7 dias (7 dias para todas as consultas, 48 h em `prices_for_dates`); recomendam cache de 24 h do nosso lado e não usar preços expirados (`expires_at`). Na busca em tempo real: cada busca iniciada pelo usuário, botão "Book" em cada opção, resultados guardados por cerca de 15 minutos. Regras de exibição, uso comercial e redistribuição da Data API: **NÃO VERIFICADO** (a oferta pública `Travelpayouts Affiliate Agreement` não pôde ser lida). Fontes: páginas acima e https://support.travelpayouts.com/hc/en-us/articles/204529267-FAQ-about-Aviasales-API
6. **Reserva e deeplink:** a Aviasales não vende bilhetes; o usuário compra direto no fornecedor (agência ou companhia), sem taxa adicional ao usuário. Links levam ao site da Aviasales ou, na busca em tempo real, ao site da agência/companhia (link solicitado só no clique em "Book"; coleta automática de links é proibida e desativa a API; link expira em 15 minutos). A atribuição depende de passar pelo link com `marker`. Fontes: https://www.travelpayouts.com/en/offers/aviasales-affiliate-program/ · https://support.travelpayouts.com/hc/en-us/articles/203956173-Aviasales-Flights-Search-API-old-version · https://support.travelpayouts.com/hc/en-us/articles/5711895629714-Aviasales-affiliate-links
7. **Dados:** Data API: menores preços por datas, tendências, calendário mensal, ofertas especiais, busca por faixa de preço; resposta em JSON com `expires_at`. Preços em rublos por padrão. A versão antiga da busca em tempo real devolvia bagagem (`flights_baggage`, `flights_handbags`) e flags de reembolso/troca (`flight_additional_tariff_infos`); **essa versão deixa de funcionar em 15/06/2026**, e a nova (disponível desde 01/11/2025) não pôde ser lida: campos NÃO VERIFICADO. Duração/escalas na Data API: NÃO VERIFICADO (GraphQL cita `trip_duration`). Fontes: Data API e página da versão antiga acima.
8. **Múltiplos trechos e calendário:** calendário e matrizes de preços existem na Data API. Multi-trecho (open-jaw) existia na busca antiga e consta no título da nova ("real-time and multi-city search"), mas a nova não foi lida.
9. **Documentação e estabilidade:** documentação em central de ajuda, parte dela com versões antigas convivendo (risco de confusão); estabilidade NÃO VERIFICADO.
10. **Riscos:** (a) dados em cache 2 a 7 dias; (b) rublos e mercado `ru` por padrão; (c) busca em tempo real exige aprovação e metas de conversão; (d) comissão paga pelo provedor: rotular como afiliado e não alterar ranking (AGENTS.md §2.15); (e) termos de uso comercial/redistribuição não lidos; (f) um SaaS que reexibe dados em canal de mensagens pode não ser o uso pretendido ("gerar páginas de conteúdo"), então perguntar por escrito.

### 3.2 Skyscanner Travel API (plano B)

1. **Acesso comercial:** "Access to our Travel API is available through a commercial agreement only", mais aprovação da equipe de Parceiros. Critérios: negócio estabelecido com grande audiência (>100 mil visitas mensais) e alinhamento com a marca. Não dão acesso a: estudantes/indivíduos, startups sem plano robusto e produto pronto, sites de baixo tráfego, agências de desenvolvimento. Resposta por e-mail em até duas semanas. Fontes: https://skyscannerpartnersupport.zendesk.com/hc/en-us/articles/10881129136029-How-do-I-get-access-to-the-Travel-API · https://skyscannerpartnersupport.zendesk.com/hc/en-us/articles/10881149122717-What-is-the-acceptance-criteria-for-the-Travel-API · https://www.partners.skyscanner.net/product/travel-api
2. **Sandbox e custo:** sandbox/chave de teste: NÃO VERIFICADO (a página de autenticação não cita). Custo: "completely free to use", sem taxa por reserva nem mensalidade; modelo de participação na receita. Fonte: https://skyscannerpartnersupport.zendesk.com/hc/en-us/articles/4524639452573-How-much-does-it-cost-to-use-the-Skyscanner-Flights-API · https://developers.skyscanner.net/docs/getting-started/authentication
3. **Cobertura:** "160+ mercados" e conteúdo de "centenas de companhias e OTAs". Brasil, América Latina e companhias low-cost: NÃO VERIFICADO. Fonte: https://www.partners.skyscanner.net/product/travel-api
4. **Limites e preço:** padrão de 100 buscas (create) e 500 consultas (poll) por minuto na Live Prices; 500 por minuto na Indicative; limite por parceiro, ajustável com o gerente de contas. Percentual de comissão: NÃO VERIFICADO (não publicado). Fontes: https://skyscannerpartnersupport.zendesk.com/hc/en-us/articles/4687312848797-Which-services-can-I-access-and-what-are-the-rate-limits · https://developers.skyscanner.net/docs/faqs
5. **Termos:** exibir "Powered by" + logo Skyscanner com link; indicar chegada em dia diferente; mostrar companhia principal por trecho e se é direto ou com escala; chamadas Live Prices somente por ação do usuário e com origem/destino/datas definidos; sem chamadas automáticas; deeplink só de botão visível, em nova aba; proibido deeplink a partir de banner/newsletter/material promocional. Meta de imprecisão de preço abaixo de 5% dos redirecionamentos. Regras de cache da Live Prices: NÃO VERIFICADO. Fontes: https://developers.skyscanner.net/docs/getting-started/usage-guidelines · https://developers.skyscanner.net/docs/flights-live-prices/b2b-pricing-accuracy
6. **Reserva:** cada itinerário traz `deepLink` para a página de reserva do agente (OTA/companhia); quem processa a venda é o agente, não o AeroZap. Sessão expira em cerca de 1 hora; "Itinerary Refresh" devolve o preço atualizado (cache de 10 minutos). Fontes: https://developers.skyscanner.net/docs/flights-live-prices/overview · https://developers.skyscanner.net/docs/flights-live-prices/refresh-prices · https://developers.skyscanner.net/docs/faqs
7. **Dados:** itinerários com trechos/segmentos, duração em minutos, número de escalas, companhias, horários; tarifas com bagagem de mão e despachada e condições (mudança antecipada, cancelamento, pré-reserva de assento, elegibilidade a upgrade) em `pricingOptionFare`. Os dados de bagagem e atributos adicionais exigem onboarding com o gerente de contas; `includeBaggageData` está marcado como obsoleto. Fonte: https://developers.skyscanner.net/api/flights-live-pricing
8. **Multi-trecho e calendário:** até 6 trechos (`queryLegs`), 1 a 8 adultos, crianças, aeroportos próximos à origem. Calendário por data e por mês via Indicative Prices (preço mais barato visto por rota e data, "até 4 dias" de idade, 1 adulto econômica, **não reservável**). Fontes: https://developers.skyscanner.net/docs/flights-live-prices/query-object · https://developers.skyscanner.net/docs/flights-indicative-prices/use-cases · https://developers.skyscanner.net/docs/flights-indicative-prices/overview
9. **Documentação:** pública, detalhada e versionada (v3); estabilidade NÃO VERIFICADO.
10. **Riscos:** (a) critério de audiência pode barrar o AeroZap no lançamento (e a página afirma que só oferecem a API se as buscas gerarem reservas); (b) busca ao vivo proibida para monitoramento automático (alertas só com Indicative); (c) exigência de marca "Powered by" em WhatsApp precisa de solução; (d) deeplink em mensagem promocional vedado; (e) bagagem exige onboarding.

### 3.3 Amadeus Enterprise (Quick-Connect), contingência

1. **Acesso:** começa pelo contato com vendas; público-alvo declarado: vendedores de viagem (agências, OTAs, operadoras). Critérios de elegibilidade e termos contratuais: NÃO VERIFICADO. Fonte: https://amadeus.com/en/travel-sellers/products/quick-connect
2. **Teste e custo:** "developers can test the API" (ambiente: NÃO VERIFICADO). Integração estimada em 4 a 6 semanas (a partir da integração, não é prazo de aprovação). Preços: não publicados, "falar com vendas". Mesma fonte.
3. **Cobertura:** Flight Offers Search cobre "mais de 500 companhias, full-service e low-cost", com franquia de bagagem e terminal. Brasil/LatAm e quais low-cost: NÃO VERIFICADO.
4. **Limites e preço:** NÃO VERIFICADO.
5. **Exibição, cache e redistribuição:** NÃO VERIFICADO (contrato).
6. **Reserva:** a oferta inclui Flight Offers Price (confirma tarifa e preço final) e Flight Create Orders (reserva e pagamento): é fluxo de quem vende. Usar só busca/preço para comparar e redirecionar é NÃO VERIFICADO.
7. **Dados:** bagagem, preço final, ancilares (bagagem de 70+ companhias, assentos de 80+); duração/escalas/condições: NÃO VERIFICADO na página.
8. **Multi-trecho:** páginas citam viagens simples, só-ida combináveis e multidestino. Calendário/datas flexíveis: NÃO VERIFICADO.
9. **Documentação e estabilidade:** portal não legível pela ferramenta; NÃO VERIFICADO. O descontinuamento do Self-Service é sinal de estratégia voltada a clientes corporativos (inferência).
10. **Riscos:** custo e contrato desconhecidos; provável exigência de entidade com acreditação/relacionamento de venda; prazo de contratação desconhecido; elegibilidade para metabusca desconhecida.

## 4. Matriz comparativa

| Critério | Travelpayouts/Aviasales | Skyscanner | Amadeus Enterprise | Amadeus Self-Service | Kiwi/Tequila | Duffel |
|---|---|---|---|---|---|---|
| Situação | Aberto (Data API); busca real-time sob aprovação | Sob acordo comercial | Via vendas | **Encerrado** | **Só por convite** | Aberto, mas para venda |
| Aprovação | Token + aceite do acordo; real-time: por afiliado | Aprovação + >100 mil visitas/mês | Contato com vendas | n/a | Convite | Go-live imediato (sandbox, cartão, saldo) |
| Sandbox | NÃO VERIF. (playground GraphQL) | NÃO VERIF. | "pode testar" (detalhe NÃO VERIF.) | n/a | n/a | Sim (token de teste) |
| Custo de entrada | Sem cobrança encontrada; afiliado | Gratuita; receita compartilhada | NÃO VERIF. | n/a | n/a | US$ 3,00/pedido; 1% Managed Content (excedente de busca US$ 0,005) |
| Brasil/LatAm | NÃO VERIF. | NÃO VERIF. (160+ mercados) | NÃO VERIF. | n/a | n/a | LATAM e Azul listadas (Azul via Travelport) |
| Low-cost | NÃO VERIF. | NÃO VERIF. | Sim ("full-service e low-cost"; quais, NÃO VERIF.) | n/a | n/a | Azul (low-cost) listada |
| Tempo real | Só com aprovação | Sim | Sim | n/a | n/a | Sim |
| Dado em cache | 2 a 7 dias | Indicative até 4 dias | NÃO VERIF. | n/a | n/a | Ofertas expiram rápido |
| Bagagem | Antiga sim; nova NÃO VERIF. | Sim (com onboarding) | Franquia sim | n/a | n/a | Parcial ("Paid Bags" em breve na LATAM/Azul) |
| Condições tarifárias | Antiga: reembolso/troca; nova NÃO VERIF. | Sim | NÃO VERIF. | n/a | n/a | NÃO VERIF. |
| Multi-trecho | Sim (nova, título) | Até 6 trechos | Sim (multidestino) | n/a | n/a | NÃO VERIF. |
| Calendário de preços | Sim (cache) | Sim (Indicative) | NÃO VERIF. | n/a | n/a | NÃO VERIF. |
| Quem vende | Agência/companhia | Agente (OTA/companhia) | Quem integra (emissor) | n/a | n/a | O integrador (merchant) |
| Alertas automáticos | Data API: sim (cache). Real-time: proibido | Live: proibido. Indicative: ver confirmação | NÃO VERIF. | n/a | n/a | Cota busca/pedido |
| Monetização | 1,1% do bilhete (afiliado) | Participação na receita (taxa NÃO VERIF.) | NÃO VERIF. | n/a | n/a | Paga o integrador |
| Documentação | Boa, com versões legadas | Boa e pública | NÃO VERIF. | n/a | n/a | Boa |
| Aderência ao AeroZap | Média (cache) | Alta, se aprovado | Incerta | Nenhuma | Nenhuma | Baixa |

Fontes da matriz: as das seções 2 e 3; Duffel em https://duffel.com/pricing, https://help.duffel.com/hc/en-gb/articles/360019685579-How-do-I-go-live-once-I-ve-built-my-integration, https://duffel.com/flights/airlines/latam e https://duffel.com/flights/airlines/azul.

## 5. Milhas

**Veredito: NÃO VERIFICADO. Recomenda-se NÃO prometer pesquisa em milhas no lançamento** (AGENTS.md §2.14, DTC §8.2).

- Não encontrei fonte oficial e autorizada de disponibilidade de resgate (Smiles, LATAM Pass, TudoAzul) acessível a um SaaS. Não achei API pública de terceiros desses programas nas buscas.
- Os provedores avaliados tratam de tarifa em dinheiro. Os de busca da Travelpayouts e da Skyscanner não descrevem disponibilidade em milhas nas páginas lidas. Nas páginas da Duffel sobre LATAM e Azul, "Loyalty Programmes" aparece como "SOON" (em breve), o que não é evidência de disponibilidade de resgate.
- seats.aero é a ferramenta de disponibilidade de milhas encontrada, mas: uso comercial exige permissão escrita; sem suporte a LATAM Pass; Smiles/TudoAzul não confirmados; dados de voos além de 60 dias não podem ser exibidos publicamente a terceiros sem fluxo OAuth ou acordo; revendedores de milhas são vetados; a empresa diz não ter afiliação com companhias. Fontes: https://seats.aero/terms · https://docs.seats.aero/article/68-seatsaero-pro-api-access-limits-and-usage.
- Ferramentas de raspagem em marketplaces (ex.: Apify) apareceram nas buscas. Não são fontes autorizadas e podem violar termos dos programas. Descartadas.
- Conduta: manter a porta `MilesProvider` desabilitada ("não habilitado"). Nunca converter tarifa em pontos. Se o Product Owner quiser milhas, abrir negociação por escrito com seats.aero e com cada programa antes de qualquer promessa.

## 6. Recomendação

**Nº 1 — Travelpayouts / Aviasales Data API** (condicional a três confirmações por escrito):

1. termos de uso comercial, exibição e redistribuição da Data API em um SaaS multicanal (WhatsApp + Web);
2. cobertura real de rotas do Brasil/LatAm e suporte a BRL (testar com o token real);
3. forma de solicitar a busca em tempo real e como as metas de conversão (9% e 5%) se aplicam a um produto novo.

Motivos: acesso por token sem barreira de tráfego declarada; entrega calendário e preços em cache para o fluxo de alertas e datas flexíveis; o usuário compra no fornecedor (coerente com "oferta não é passagem emitida"). Custo: dado em cache, não reservável; usar sempre `lastCheckedAt`, `expires_at` e rotular "preço indicativo". Busca em tempo real só após aprovação.

**Plano B — Skyscanner Travel API:** melhor aderência técnica (tempo real, até 6 trechos, bagagem e condições, calendário, deeplink, atualização de preço). Inscrever já, em paralelo; resposta prometida em até duas semanas. Risco alto de recusa pelo critério de audiência no lançamento. Se aprovada, vira fonte principal para pesquisa ao vivo e a Aviasales fica para calendário e alertas.

**Contingência — Amadeus Enterprise (Quick-Connect):** abrir conversa com vendas só para obter preço, elegibilidade de comparador e ambiente de teste. Não bloquear o cronograma por ele.

Arquitetura: a porta `FlightProvider` já prevista isola tudo. Cada adaptador publica capacidades (`realTime`, `calendar`, `baggage`, `fareConditions`, `multiCity`, `cacheTtl`, `userInitiatedOnly`), de modo que o agregador e o scheduler de alertas respeitem as regras de cada provedor. Mudança de contrato `FlightOffer`/`FlightProvider` vai ao CTO (AGENTS.md §6.2): propor campo `dataKind` (`LIVE` | `INDICATIVE` | `DEMO`) e `isDemo`.

## 7. Desenvolver sem credencial: adaptador simulado DEMO (AZ-030)

- `DemoFlightProvider` implementa `FlightProvider`, ativo apenas nos perfis `local` e `test`. A aplicação **falha na inicialização** se ele for habilitado em produção.
- Toda oferta traz `provider = "DEMO"`, `isDemo = true`, `offerId` com prefixo `DEMO-`, companhia fictícia ("Demo Air"), `bookingUrl = null`. A Web e o WhatsApp exibem a faixa "DEMO — dados fictícios, não são passagens reais" em cada resultado e resumo (AGENTS.md §2.17). Nenhuma oferta DEMO entra em histórico de preço real nem em alerta real.
- Geração determinística por semente (rota + datas), para testes repetíveis; moeda e preço fictícios, jamais copiados de provedor real.
- Injeção de falhas configurável, cobrindo a definição de pronto do A4: timeout, falha parcial, resposta incompleta, preço inválido/negativo, oferta duplicada, 429, provedor lento.
- Fixtures de contrato de provedor real só serão criadas a partir de respostas obtidas em teste real autorizado; antes disso, os testes de contrato validam apenas o esquema interno do `FlightOffer`.
- Marco M3 sem credencial é aceito somente como "DEMO — não validado com provedor real" (plano §4, Fase 3).

## 8. O que o Product Owner precisa providenciar

| # | Ação | Prazo esperado |
|---|---|---|
| 1 | Criar conta Travelpayouts, aceitar o acordo de afiliado, obter o token e enviar as três perguntas da seção 6 por escrito | Prazo de aprovação NÃO VERIFICADO (não informado) |
| 2 | Enviar a candidatura à Skyscanner (empresa, site, proposta de negócio) em https://www.partners.skyscanner.net/contact/travel-api | Resposta por e-mail em até 2 semanas (fonte oficial) |
| 3 | Contatar vendas da Amadeus (Quick-Connect): preço, elegibilidade de comparador, ambiente de teste | NÃO VERIFICADO; integração estimada em 4 a 6 semanas após contratação |
| 4 | Ter pessoa jurídica (CNPJ) e site público com a proposta de produto para as candidaturas | A definir pelo PO |
| 5 | Revisão jurídica dos termos de cada provedor (cache, redistribuição, deeplink em WhatsApp, marca "Powered by") | Iniciar na Fase 0 |
| 6 | Decidir a política de comissão e rotulagem de links afiliados (sem efeito silencioso no ranking) | Antes do M3 |
| 7 | Decidir se milhas ficam fora do escopo inicial | Fim da Fase 0 |

## 9. Perguntas em aberto

1. A Data API da Aviasales pode ser usada em SaaS com resposta via WhatsApp e armazenamento do histórico de preços (observações) por mais de 7 dias?
2. A Aviasales cobre rotas domésticas do Brasil e companhias low-cost brasileiras? Aceita moeda BRL ou só converte a partir de rublos?
3. Quais campos a nova API de busca em tempo real da Travelpayouts (desde 01/11/2025) devolve (bagagem, condições, multi-trecho)?
4. Como exibir "Powered by Skyscanner" dentro de mensagens de WhatsApp? Alerta com link para a Web do AeroZap atende à regra de deeplink?
5. Monitoramento: Skyscanner aceita alertas movidos por Indicative Prices e confirmação ao vivo só quando o usuário clica?
6. O Amadeus Enterprise atende comparadores que não emitem bilhetes? Qual o custo mínimo?
7. Resultado parcial (um provedor caiu) e zero resultados consomem franquia? (decisão A1/A8/PO, AGENTS.md §3)
8. Metas de escala (pesquisas/minuto, alertas ativos) para dimensionar cotas por provedor (A6/A7).
9. Qual a política de retenção do histórico de preços compatível com a LGPD e com os termos dos provedores?
10. Vale pesquisar Sabre, Travelport e outros agregadores com API de parceiros (não avaliados nesta rodada)?

## 10. Fontes principais

Amadeus: https://amadeus.com/en/travel-sellers/products/quick-connect · https://www.phocuswire.com/amadeus-shut-down-self-service-apis-portal-developers
Travelpayouts: https://support.travelpayouts.com/hc/en-us/articles/203956163-Aviasales-Data-API · .../203956083-Requirements-for-Aviasales-data-API-access · .../4402565416594-API-rate-limits · .../210995808-Requirements-for-Aviasales-Flight-Search-API-access · .../204529267-FAQ-about-Aviasales-API · https://www.travelpayouts.com/en/offers/aviasales-affiliate-program/
Skyscanner: https://developers.skyscanner.net/docs/intro · https://www.partners.skyscanner.net/product/travel-api · https://skyscannerpartnersupport.zendesk.com/hc/en-us/articles/10881129136029
Kiwi: https://media.kiwi.com/articles-and-interviews/better-for-business-kiwi-com-takes-a-new-approach-to-partnerships/
Duffel: https://duffel.com/pricing · https://duffel.com/services-agreement
Milhas: https://seats.aero/terms · https://docs.seats.aero/article/68-seatsaero-pro-api-access-limits-and-usage
