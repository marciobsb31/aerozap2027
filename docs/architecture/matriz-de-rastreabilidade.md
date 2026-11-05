# Matriz de rastreabilidade — requisito → teste → evidência

> **Arquivo gerado.** A fonte é `tests/rastreabilidade/matriz.json`; este documento
> sai dela por `bash scripts/rastreabilidade.sh`. Editar aqui é trabalho perdido:
> a CI regera e compara.

Requisitos do PRD: **104**. Cobertos **7**, parciais **26**, pendentes **71**. Atualizada em 2026-11-05.

A contagem não é nota de desempenho. O produto está no marco M1 e a maior parte dos
requisitos ainda não tem código — um requisito pendente com nota honesta vale mais do
que um verde que não corresponde a teste nenhum. O que a matriz garante é que nenhum
requisito some do radar e que nenhum status exista sem arquivo de teste que o sustente.

| Símbolo | Status | Significado |
| --- | --- | --- |
| ● | COBERTO | o requisito está implementado e há teste automatizado que falha se ele quebrar |
| ◐ | PARCIAL | parte do requisito tem teste; a nota diz o que falta e em que marco entra |
| ○ | PENDENTE | ainda não implementado; nenhum teste é esperado |

## M1

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ● | **RF-074** Logo: placeholder identificado até o arquivo original; slogan preservado | Must | `app.spec.ts` | Placeholder de logo desenhado em CSS e identificado como provisório no README do frontend. |
| ● | **RF-094** Toda UI e mensagem em pt-BR (padrão) e es desde a primeira versão; sem texto fixo no código; chaves completas verificadas no CI | Must | `sem-strings-fixas.spec.ts`<br>`i18n.spec.ts` | Varredura falha com qualquer texto fora do dicionário; chave faltando em um idioma não compila. |
| ● | **RF-102** Minimização dos dados enviados à IA; logs sem tokens, senhas, áudio ou dados pessoais desnecessários | Must | `MascaradorDeDadosSensiveisTest.java`<br>`MensagemMascaradaConverterTest.java` | Mascaramento no log provado em 21 casos; a minimização do que vai para a IA é reforçada pela porta de LLM. |

## M2

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ◐ | **RF-001** Cadastro com credenciais (método a confirmar; PROPOSTA e-mail + senha) | Must | `AutenticacaoIntegrationTest.java` | Cadastro com e-mail e senha funciona de ponta a ponta. O método continua PROPOSTA no PRD; está isolado em configuração para não virar migração se mudar. |
| ● | **RF-002** Login/logout com access token curto e refresh token rotacionado; limitação de tentativas (brute force) | Must | `AutenticacaoIntegrationTest.java`<br>`RotacaoConcorrenteIntegrationTest.java`<br>`EmissorDeAcessoTest.java` | Acesso curto, refresh rotacionado com detecção de reuso que derruba a família, e limite de tentativas por conta. A rotação é provada também sob 16 renovações simultâneas. |
| ○ | **RF-003** Recuperação de acesso | Must | — | Sem implementação ainda; entra no marco M2. |
| ◐ | **RF-004** Perfil do usuário e preferências de viagem | Must | `AutenticacaoIntegrationTest.java` | Perfil e idioma existem; preferências de viagem, moeda e fuso entram com a AZ-024. |
| ○ | **RF-005** Vínculo telefone-conta com verificação de posse do número | Must | — | Sem implementação ainda; entra no marco M2. |
| ◐ | **RF-007** Isolamento: `user_id` vem do token; acesso a recurso de outro usuário é negado | Must | `AutenticacaoIntegrationTest.java`<br>`SegurancaIntegrationTest.java` | O identificador vem do cabeçalho posto pelo gateway, nunca do corpo. O teste de acesso cruzado entre dois usuários é da AZ-025. |
| ◐ | **RF-009** Plano Free com franquia de N consultas por período (padrão N=5, **configurável**) | Must | `FranquiaIntegrationTest.java` | A franquia é dado do plano no banco, não constante no código. Publicar mudança sem deploy é a AZ-023. |
| ◐ | **RF-010** Saldo único compartilhado entre WhatsApp e Web; sem regras de consumo por canal | Must | `FranquiaIntegrationTest.java` | O saldo é do usuário e não tem regra por canal. Com um canal só existindo hoje, a prova entre WhatsApp e Web é do M4. |
| ◐ | **RF-011** Tipo de período configurável: trimestre-calendário (padrão, HIPÓTESE) ou janela móvel desde o cadastro | Must | `TipoDePeriodoTest.java` | Trimestre-calendário e janela móvel implementados e testados nas bordas da virada. A janela móvel precisa da data de cadastro, que depende da API interna da identidade. |
| ● | **RF-012** Débito único por pesquisa concluída com sucesso, idempotente (UNIQUE chave de idempotência + search_id) | Must | `FranquiaConcorrenciaIntegrationTest.java`<br>`FranquiaIntegrationTest.java` | Débito único por chave de idempotência, provado inclusive com 32 chamadas idênticas simultâneas devolvendo a mesma reserva. |
| ● | **RF-013** Reserva → confirmação/liberação; falha interna ou de provedor não consome; reserva órfã expira | Must | `FranquiaConcorrenciaIntegrationTest.java`<br>`ExpiracaoDeReservasIntegrationTest.java` | Reserva, confirmação e liberação; falha não consome; reserva órfã volta sozinha e não credita duas vezes. A última consulta disputada por 32 requisições autoriza uma. |
| ◐ | **RF-014** Política configurável de "pesquisa concluída com sucesso" (parcial, zero resultados) | Must | `FranquiaIntegrationTest.java` | A política de conclusão é configuração (APENAS_COM_OFERTAS por padrão). Só terá efeito observável quando a pesquisa existir, no M3. |
| ◐ | **RF-015** Consulta de saldo, período e histórico de consumo pelo usuário (Web e WhatsApp); suporte vê o mesmo no Admin (RF-077) | Must | `FranquiaIntegrationTest.java` | Saldo e período consultáveis pela API interna; a tela é da AZ-024 e o histórico de consumo, do M6. |
| ○ | **RF-018** Planos, preço, duração, desconto, parcelamento, renovação e benefícios configuráveis sem novo deploy do backend | Must | — | Sem implementação ainda; entra no marco M2. |
| ○ | **RF-070** Tela de plano e consumo: saldo, período, histórico, assinatura | Must | — | Sem implementação ainda; entra no marco M2. |
| ○ | **RF-071** Preferências: idioma, moeda, país, fuso e canal de notificação | Must | — | Sem implementação ainda; entra no marco M2. |
| ◐ | **RF-095** Idioma, moeda, fuso e país tratados separadamente | Must | `i18n.spec.ts` | Idioma é tratado separadamente desde já; moeda, fuso e país entram com o perfil, no M2. |
| ● | **RF-099** Aceite versionado e registrado de Termos de Uso e Política de Privacidade no cadastro | Must | `AutenticacaoIntegrationTest.java` | Aceite de Termos e de Política gravado com versão, data e origem no mesmo ato do cadastro. |

## M3

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ◐ | **RF-016** Saldo zero bloqueia nova pesquisa sem consumir e oferece Premium (antes do M7: "Premium em breve", PROPOSTA) | Must | `FranquiaIntegrationTest.java` | Saldo zero recusa com 409 usage-exhausted sem consumir nada. A oferta do Premium na interface é do M3. |
| ◐ | **RF-019** Pesquisa nacional/internacional por origem, destino, datas, passageiros (adultos, crianças, bebês), cabine e moeda; ida e ida e volta | Must | `SearchIntentTest.java`<br>`PassengersTest.java` | Os campos da pesquisa e suas regras estão no contrato e validados; a execução da pesquisa é do M3. |
| ○ | **RF-022** Filtros por preço, duração, escalas, horários, companhias e aeroportos, sobre resultado já obtido | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-023** Ordenação por menor preço, menor duração e melhor equilíbrio preço/tempo | Must | — | Sem implementação ainda; entra no marco M3. |
| ◐ | **RF-024** Cada oferta mostra fonte, horário da última verificação, moeda, condições e link de continuidade; preço revalidado antes do redirecionamento quando possível | Must | `OfertaETrechosTest.java`<br>`ContratosPublicadosTest.java` | Fonte, horário de verificação, moeda e link são obrigatórios no contrato da oferta. |
| ◐ | **RF-025** Pesquisa assíncrona: `search_id` + status (contrato define estados; DTC cita PENDING/PROCESSING/COMPLETED); acompanhamento por polling/SSE; mesmo contrato nos dois canais | Must | `ContratosPublicadosTest.java` | Envelope e estados publicados; o fluxo assíncrono é do M3. |
| ◐ | **RF-026** Validação de entrada (IATA, datas, passageiros, ambiguidades) com mensagens de erro claras | Must | `SearchIntentTest.java`<br>`ExtratorDeDatasTest.java` | Validação de IATA, datas e passageiros no contrato e no interpretador; as mensagens de erro na tela são do M3. |
| ◐ | **RF-027** Degradação controlada: mostrar resultados dos provedores disponíveis e indicar fontes que falharam | Must | `ProviderResultTest.java` | Falha de provedor é resultado tipado, não exceção — é o que permite mostrar o que deu certo e dizer o que falhou. |
| ○ | **RF-028** Histórico de pesquisas e ofertas consultadas, paginado, só do próprio usuário | Must | — | Sem implementação ainda; entra no marco M3. |
| ◐ | **RF-029** Adaptador simulado sempre rotulado DEMO em toda interface e mensagem | Must | `OfertaETrechosTest.java` | O campo demo é obrigatório na oferta e sobe até a interface; o rótulo na tela é do M3. |
| ◐ | **RF-031** Nenhum preço, horário, companhia, disponibilidade ou link que não venha de provedor | Must | `OfertaETrechosTest.java` | Nenhum preço existe no contrato sem provedor declarado. |
| ○ | **RF-032** Resultado vazio: estado informativo com sugestões (alterar datas/aeroportos) | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-033** Executar pesquisa exige usuário autenticado; visitante vê página e formulário | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-034** Consolidar e deduplicar ofertas preservando origem e condições de cada fonte | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-035** Destaques "mais barato", "melhor custo-benefício" e "mais rápido", com algoritmo determinístico documentado e critério visível ao usuário | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-036** Comparar ofertas lado a lado (preço, duração, escalas, fonte, condições disponíveis); limite de itens definido no design | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-038** Qualquer destaque patrocinado é rotulado; remuneração não altera ranking ou recomendação | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-066** Página inicial pública com proposta de valor, slogan e chamada para cadastro | Should | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-067** Formulário de pesquisa com validação inline e estados processando, parcial, erro, vazio e saldo esgotado | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-068** Cartões de oferta legíveis, com rótulo "oferta, não passagem emitida" | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-069** Pesquisa em linguagem natural na Web | Should | — | Sem implementação ainda; entra no marco M3. |
| ◐ | **RF-073** Responsivo e acessível (WCAG 2.1 AA: contraste, teclado, leitor de tela, linguagem simples) | Must | `acessibilidade.spec.ts` | axe-core sobre o DOM real em cinco cenários, pt e es. A auditoria WCAG completa das telas de pesquisa é do M3. |
| ○ | **RF-075** Aviso de transparência (comissão, ofertas não emitidas, termos e privacidade) em resultados e rodapé | Must | — | Sem implementação ainda; entra no marco M3. |
| ○ | **RF-097** Formatação localizada de data, número e moeda; horários de voo no fuso do aeroporto | Must | — | Sem implementação ainda; entra no marco M3. |

## M4

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ○ | **RF-006** Trocar ou desvincular telefone com nova verificação | Should | — | Sem implementação ainda; entra no marco M4. |
| ◐ | **RF-040** Interpretar texto em linguagem natural como `SearchIntent` estruturado validado por schema (Web: mesmo motor, Should em M3) | Must | `ConjuntoDouradoTest.java` | Interpretador v0 determinístico com 42 casos pt/es; o motor com LLM é do M4. |
| ◐ | **RF-041** Pedir apenas dados essenciais faltantes, uma pergunta por vez | Must | `ConjuntoDouradoTest.java` | Dado incompleto gera pergunta, nunca pesquisa inventada — mês sem dia é o caso que prova. |
| ○ | **RF-042** Contexto de conversa com expiração e refinamentos (datas, passageiros, bagagem, escalas, orçamento, preferências) | Must | — | Sem implementação ainda; entra no marco M4. |
| ○ | **RF-043** Áudio: validar formato, tamanho, duração e origem; transcrever; interpretar; validar | Must | — | Sem implementação ainda; entra no marco M4. |
| ◐ | **RF-044** Confirmar interpretação com o usuário em baixa confiança ou ambiguidade material, antes de pesquisar | Must | `ConjuntoDouradoTest.java` | Ambiguidade vira pergunta no v0; a confirmação com a pessoa é do M4. |
| ○ | **RF-045** Áudio ininteligível ou IA/transcrição indisponível: mensagem amigável pedindo texto ou novo áudio, sem consumo | Must | — | Sem implementação ainda; entra no marco M4. |
| ◐ | **RF-046** IA não autoriza pagamento, não altera plano/permissão, não executa SQL nem acessa credenciais; ferramentas allowlisted; defesa a prompt injection; nunca expõe dados de outros usuários | Must | `GuardaDeInjecaoTest.java` | Tipo selado sem forma que expresse "mude o plano", mais guarda de injeção. Avaliação de injeção indireta é do M4. |
| ○ | **RF-048** Usuário pode pedir para encerrar/esquecer o contexto da conversa | Should | — | Sem implementação ainda; entra no marco M4. |
| ◐ | **RF-049** Registrar versão do prompt, modelo, tokens, latência e custo por interação | Must | `ConjuntoDouradoTest.java` | Registro de modelo, versão de prompt, tokens, latência e custo existe na porta de LLM; a métrica em produção é do M4. |
| ○ | **RF-050** Webhook valida assinatura/origem, responde rápido e processa em fila; adaptador trocável sem afetar IA/pesquisa | Must | — | Sem implementação ainda; entra no marco M4. |
| ◐ | **RF-051** Deduplicação por id externo; reentrega não cria duas mensagens nem duas pesquisas | Must | `ConsumidorIdempotenteConcorrenciaIntegrationTest.java`<br>`OutboxConcorrenciaIntegrationTest.java` | Reentrega não duplica efeito, provado com 24 entregas simultâneas. A deduplicação por id do WhatsApp é do M4. |
| ○ | **RF-052** Identificar telefone→conta; número não vinculado recebe orientação para criar conta/vincular e não pesquisa | Must | — | Sem implementação ainda; entra no marco M4. |
| ○ | **RF-053** Resposta com resumo das melhores opções, "buscando…" se demorar, e link para detalhes na Web | Must | — | Sem implementação ainda; entra no marco M4. |
| ○ | **RF-054** Continuar na Web a pesquisa iniciada no WhatsApp (mesmo `search_id` e histórico) sem novo consumo | Must | — | Sem implementação ainda; entra no marco M4. |
| ○ | **RF-055** Franquia esgotada: mensagem clara, data de renovação e link para assinar | Must | — | Sem implementação ainda; entra no marco M4. |
| ○ | **RF-056** Respeitar janela de atendimento e templates aprovados | Must | — | Sem implementação ainda; entra no marco M4. |
| ○ | **RF-096** WhatsApp responde no idioma preferido; na ausência, idioma da mensagem (PROPOSTA) | Must | — | Sem implementação ainda; entra no marco M4. |
| ○ | **RF-098** Templates de WhatsApp e e-mails transacionais em pt/es; copy es revisada por falante nativo | Must | — | Sem implementação ainda; entra no marco M4. |
| ○ | **RF-100** Aviso de uso de áudio antes do primeiro áudio; exigir ciência ou apenas informar conforme parecer jurídico (configurável) | Must | — | Sem implementação ainda; entra no marco M4. |
| ○ | **RF-101** Retenção configurável por categoria (mensagens, áudio, transcrição, logs, histórico) com expurgo automático | Must | — | Sem implementação ainda; entra no marco M4. |

## M5

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ○ | **RF-047** Criar e gerir alertas por linguagem natural | Should | — | Sem implementação ainda; entra no marco M5. |
| ○ | **RF-058** Usuário pode parar notificações proativas (opt-out) por resposta ou na Web | Must | — | Sem implementação ainda; entra no marco M5. |
| ○ | **RF-059** Criar alerta por rota, período, moeda, preço-alvo e preferências, por conversa ou Web | Must | — | Sem implementação ainda; entra no marco M5. |
| ○ | **RF-060** Pausar, reativar, editar e cancelar alertas | Must | — | Sem implementação ainda; entra no marco M5. |
| ○ | **RF-061** Verificações em intervalos configuráveis, respeitando cotas e termos dos provedores; registrar histórico de verificações e variações | Must | — | Sem implementação ainda; entra no marco M5. |
| ○ | **RF-062** Notificar queda, preço ≤ alvo ou mudança relevante, com cooldown e deduplicação; trazer fonte, horário e link | Must | — | Sem implementação ainda; entra no marco M5. |
| ○ | **RF-063** Opt-in explícito registrado antes de notificar | Must | — | Sem implementação ainda; entra no marco M5. |
| ○ | **RF-064** Limite de alertas ativos e frequência por plano, configurável | Must | — | Sem implementação ainda; entra no marco M5. |
| ○ | **RF-065** Histórico de preço da rota na Web | Should | — | Sem implementação ainda; entra no marco M5. |
| ○ | **RF-072** Gestão de alertas | Must | — | Sem implementação ainda; entra no marco M5. |

## M6

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ◐ | **RF-008** Papéis administrativos separados dos planos; MFA obrigatório para admin | Must | `AutenticacaoIntegrationTest.java` | Papéis são separados do plano e concedidos, nunca escolhidos no cadastro. MFA de administrador é do M6. |
| ○ | **RF-020** Múltiplos destinos, conforme cobertura dos provedores | Could | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-021** Datas flexíveis, períodos próximos, aeroportos alternativos e calendário de preços, quando suportados | Should | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-037** Comparação avançada (ex.: períodos próximos) | Could | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-057** Status de entrega (enviado/entregue/lido/falha) quando o provedor informar | Could | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-076** Dashboard: usuários, MRR, conversão, pesquisas, custo por pesquisa e disponibilidade | Must | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-077** Usuários: busca, detalhes, status, plano, consumo (saldo e histórico para suporte), bloqueio com efeito imediato nos dois canais | Must | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-078** Planos: preço, duração, benefícios, regras e publicação sem deploy | Must | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-080** Provedores: credenciais mascaradas, ambiente, cotas, prioridade, teste e saúde | Must | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-081** WhatsApp: adaptador, número, status, webhook e logs operacionais | Must | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-082** IA: modelo, prompts versionados, custos e métricas | Should | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-083** Alertas: filas, execuções, falhas e reprocessamento autorizado | Should | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-084** Auditoria: ator, ação, recurso, data, IP, resultado; toda alteração administrativa auditada | Must | — | Sem implementação ainda; entra no marco M6. |
| ○ | **RF-085** Exportação de relatórios | Could | — | Sem implementação ainda; entra no marco M6. |

## M7

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ○ | **RF-017** Premium sem contador comercial enquanto vigente e adimplente, com rate limit técnico anti-abuso | Must | — | Sem implementação ainda; entra no marco M7. |
| ○ | **RF-079** Assinaturas e pagamentos: status, eventos, reembolsos e conciliação | Should | — | Sem implementação ainda; entra no marco M7. |
| ○ | **RF-086** Escolha de modalidade e checkout no gateway; nenhum dado de cartão armazenado no AeroZap | Must | — | Sem implementação ainda; entra no marco M7. |
| ○ | **RF-087** Recorrência e renovação conforme modalidade | Must | — | Sem implementação ainda; entra no marco M7. |
| ◐ | **RF-088** Webhooks de pagamento idempotentes; pagamento falho não libera Premium; evento repetido não duplica benefício | Must | `ConsumidorIdempotenteConcorrenciaIntegrationTest.java` | O mecanismo que impede benefício duplicado está provado; o webhook de pagamento é do M7. |
| ○ | **RF-089** Cancelamento com acesso até o fim do período pago | Must | — | Sem implementação ainda; entra no marco M7. |
| ○ | **RF-090** Inadimplência com período de tolerância e suspensão configuráveis | Must | — | Sem implementação ainda; entra no marco M7. |
| ○ | **RF-091** Estorno/reembolso e conciliação | Should | — | Sem implementação ainda; entra no marco M7. |
| ○ | **RF-092** Web mostra assinatura, próxima cobrança e histórico de pagamentos; estado "confirmando pagamento" | Must | — | Sem implementação ainda; entra no marco M7. |
| ○ | **RF-093** Cupons de desconto geridos no Admin | Could | — | Sem implementação ainda; entra no marco M7. |

## M8

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ○ | **RF-103** Direitos do titular: acesso/exportação, correção e exclusão de conta e dados, com canal de contato; guarda mínima de registros exigidos por lei (a validar com jurídico) | Must | — | Sem implementação ainda; entra no marco M8. |
| ○ | **RF-104** Revisão jurídica de LGPD, termos e política concluída antes do release | Must | — | Sem implementação ainda; entra no marco M8. |

## Sem marco — (bloqueado: decisão Milhas)

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ○ | **RF-030** Pesquisa em milhas só com fonte autorizada e dado verificável (programa, pontos, taxas, classe, timestamp) | Could | — | Bloqueado por decisão em aberto; sem marco até a decisão. |

## Sem marco — (com RF-030)

| | Requisito | Prio | Teste | Nota |
| --- | --- | --- | --- | --- |
| ○ | **RF-039** Dinheiro e milhas em colunas separadas, sem conversão automática | Could | — | Bloqueado por decisão em aberto; sem marco até a decisão. |

## Caminhos completos dos testes citados

- `apps/api-gateway/src/test/java/br/com/aerozap/apigateway/SegurancaIntegrationTest.java`
- `apps/conversation-ai/src/test/java/br/com/aerozap/conversationai/interpretacao/ConjuntoDouradoTest.java`
- `apps/conversation-ai/src/test/java/br/com/aerozap/conversationai/interpretacao/ExtratorDeDatasTest.java`
- `apps/conversation-ai/src/test/java/br/com/aerozap/conversationai/interpretacao/GuardaDeInjecaoTest.java`
- `apps/platform-core/src/test/java/br/com/aerozap/platformcore/ConsumidorIdempotenteConcorrenciaIntegrationTest.java`
- `apps/platform-core/src/test/java/br/com/aerozap/platformcore/OutboxConcorrenciaIntegrationTest.java`
- `apps/platform-core/src/test/java/br/com/aerozap/platformcore/franquia/ExpiracaoDeReservasIntegrationTest.java`
- `apps/platform-core/src/test/java/br/com/aerozap/platformcore/franquia/FranquiaConcorrenciaIntegrationTest.java`
- `apps/platform-core/src/test/java/br/com/aerozap/platformcore/franquia/FranquiaIntegrationTest.java`
- `apps/platform-core/src/test/java/br/com/aerozap/platformcore/franquia/TipoDePeriodoTest.java`
- `apps/platform-core/src/test/java/br/com/aerozap/platformcore/identidade/AutenticacaoIntegrationTest.java`
- `apps/platform-core/src/test/java/br/com/aerozap/platformcore/identidade/EmissorDeAcessoTest.java`
- `apps/platform-core/src/test/java/br/com/aerozap/platformcore/identidade/RotacaoConcorrenteIntegrationTest.java`
- `frontend/projects/aerozap-web/src/app/acessibilidade.spec.ts`
- `frontend/projects/aerozap-web/src/app/app.spec.ts`
- `frontend/projects/aerozap-web/src/app/sem-strings-fixas.spec.ts`
- `frontend/projects/design-system/src/lib/i18n/i18n.spec.ts`
- `libs/java/event-contracts/src/test/java/br/com/aerozap/contracts/ContratosPublicadosTest.java`
- `libs/java/event-contracts/src/test/java/br/com/aerozap/contracts/search/OfertaETrechosTest.java`
- `libs/java/event-contracts/src/test/java/br/com/aerozap/contracts/search/PassengersTest.java`
- `libs/java/event-contracts/src/test/java/br/com/aerozap/contracts/search/ProviderResultTest.java`
- `libs/java/event-contracts/src/test/java/br/com/aerozap/contracts/search/SearchIntentTest.java`
- `libs/java/observability-starter/src/test/java/br/com/aerozap/observability/logging/MascaradorDeDadosSensiveisTest.java`
- `libs/java/observability-starter/src/test/java/br/com/aerozap/observability/logging/MensagemMascaradaConverterTest.java`
