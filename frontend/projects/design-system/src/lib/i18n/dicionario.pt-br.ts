/**
 * Dicionário de origem. Toda chave nasce aqui, e o tipo `ChaveDeTraducao` sai deste objeto: um
 * idioma que esqueça uma chave não compila, o que transforma "faltou traduzir" em erro de build em
 * vez de texto faltando na tela.
 *
 * Convenção das chaves: `area.elemento` em minúsculas, sem acento, com `{algo}` para os trechos
 * que entram em tempo de execução.
 */
export const DICIONARIO_PT_BR = {
  'app.nome': 'AeroZap',
  'app.descricao': 'Busca de passagens aéreas por conversa, no WhatsApp.',

  'navegacao.pular': 'Pular para o conteúdo',
  'navegacao.principal': 'Navegação principal',
  'navegacao.inicio': 'Início',
  'navegacao.buscas': 'Minhas buscas',
  'navegacao.ajuda': 'Ajuda',

  'idioma.rotulo': 'Idioma',
  'idioma.alterado': 'Idioma alterado para {idioma}.',

  'canal.rotulo': 'Canal do WhatsApp',
  'canal.conectado': 'Conectado',
  'canal.desconectado': 'Desconectado',

  'inicio.titulo': 'Diga para onde você quer ir.',
  'inicio.subtitulo':
    'Escreva como você falaria com uma pessoa. O AeroZap entende a frase, procura nas companhias e avisa quando o preço chega onde você quer.',
  'inicio.busca.rotulo': 'O que você está procurando?',
  'inicio.busca.dica': 'Ex.: de São Paulo para Lisboa em julho, dois adultos, até R$ 4.000',
  'inicio.busca.vazio': 'Escreva o que você procura para continuar.',
  'inicio.busca.enviar': 'Buscar passagens',
  'inicio.busca.enviando': 'Procurando…',
  'inicio.exemplos.titulo': 'Exemplos que funcionam',
  'inicio.exemplos.um': 'Quero ir para Buenos Aires no feriado, saindo de Curitiba.',
  'inicio.exemplos.dois': 'Me avise se Recife ficar abaixo de 800 reais em março.',
  'inicio.exemplos.tres': 'Dois adultos e uma criança, Guarulhos para Madri, volta em 10 dias.',

  'busca.titulo': 'Procurando por “{consulta}”',
  'busca.aviso.titulo': 'Ainda não procuramos de verdade',
  'busca.aviso.texto':
    'A consulta já chega aqui inteira. A busca nas companhias e o acompanhamento de preço entram no próximo marco; até lá esta tela guarda o pedido.',
  'busca.voltar': 'Voltar ao início',
  'busca.semConsulta': 'Nenhuma consulta informada.',

  'estado.titulo': 'Estado do serviço',
  'estado.verificando': 'Verificando…',
  'estado.disponivel': 'Plataforma respondendo',
  'estado.indisponivel': 'Plataforma sem resposta',
  'estado.correlacao': 'Correlação: {correlacao}',
  'estado.verificarDeNovo': 'Verificar de novo',

  'naoEncontrada.titulo': 'Esta página não existe',
  'naoEncontrada.texto': 'O endereço pode ter mudado de lugar. O início continua no mesmo lugar.',

  'rodape.direitos': '© {ano} AeroZap. Todos os direitos reservados.',
  'rodape.contratos': 'Contratos públicos da API',

  'comum.carregando': 'Carregando',
  'comum.obrigatorio': 'obrigatório',
  'comum.erro.titulo': 'Algo deu errado',
  'comum.erro.generico': 'Não conseguimos concluir agora. Tente de novo em instantes.',
  'comum.erro.correlacao': 'Código para suporte: {correlacao}',
} as const;

export type ChaveDeTraducao = keyof typeof DICIONARIO_PT_BR;

/** Todo idioma precisa cobrir exatamente as chaves de origem — nem a menos, nem a mais. */
export type Dicionario = Readonly<Record<ChaveDeTraducao, string>>;
