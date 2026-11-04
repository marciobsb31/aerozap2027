import type { Dicionario } from './dicionario.pt-br';

/**
 * Espanhol neutro (es-419 e Espanha): sem voseo e sem gíria regional, porque o mesmo texto atende
 * Buenos Aires e Madri. Chave faltando não compila.
 */
export const DICIONARIO_ES: Dicionario = {
  'app.nome': 'AeroZap',
  'app.descricao': 'Búsqueda de vuelos por conversación, en WhatsApp.',

  'navegacao.pular': 'Saltar al contenido',
  'navegacao.principal': 'Navegación principal',
  'navegacao.inicio': 'Inicio',
  'navegacao.buscas': 'Mis búsquedas',
  'navegacao.ajuda': 'Ayuda',

  'idioma.rotulo': 'Idioma',
  'idioma.alterado': 'Idioma cambiado a {idioma}.',

  'canal.rotulo': 'Canal de WhatsApp',
  'canal.conectado': 'Conectado',
  'canal.desconectado': 'Desconectado',

  'inicio.titulo': 'Dinos a dónde quieres ir.',
  'inicio.subtitulo':
    'Escribe como le hablarías a una persona. AeroZap entiende la frase, busca en las aerolíneas y te avisa cuando el precio llega a donde quieres.',
  'inicio.busca.rotulo': '¿Qué estás buscando?',
  'inicio.busca.dica': 'Ej.: de Buenos Aires a Lisboa en julio, dos adultos, hasta 900 dólares',
  'inicio.busca.vazio': 'Escribe lo que buscas para continuar.',
  'inicio.busca.enviar': 'Buscar vuelos',
  'inicio.busca.enviando': 'Buscando…',
  'inicio.exemplos.titulo': 'Ejemplos que funcionan',
  'inicio.exemplos.um': 'Quiero ir a Buenos Aires en el feriado, saliendo de Curitiba.',
  'inicio.exemplos.dois': 'Avísame si Recife baja de 800 reales en marzo.',
  'inicio.exemplos.tres': 'Dos adultos y un niño, Guarulhos a Madrid, vuelta en 10 días.',

  'busca.titulo': 'Buscando «{consulta}»',
  'busca.aviso.titulo': 'Todavía no buscamos de verdad',
  'busca.aviso.texto':
    'La consulta ya llega aquí completa. La búsqueda en las aerolíneas y el seguimiento de precio entran en el próximo hito; hasta entonces esta pantalla guarda el pedido.',
  'busca.voltar': 'Volver al inicio',
  'busca.semConsulta': 'No se indicó ninguna consulta.',

  'estado.titulo': 'Estado del servicio',
  'estado.verificando': 'Verificando…',
  'estado.disponivel': 'La plataforma responde',
  'estado.indisponivel': 'La plataforma no responde',
  'estado.correlacao': 'Correlación: {correlacao}',
  'estado.verificarDeNovo': 'Verificar de nuevo',

  'naoEncontrada.titulo': 'Esta página no existe',
  'naoEncontrada.texto': 'La dirección puede haber cambiado de lugar. El inicio sigue donde estaba.',

  'rodape.direitos': '© {ano} AeroZap. Todos los derechos reservados.',
  'rodape.contratos': 'Contratos públicos de la API',

  'comum.carregando': 'Cargando',
  'comum.obrigatorio': 'obligatorio',
  'comum.erro.titulo': 'Algo salió mal',
  'comum.erro.generico': 'No pudimos completar la acción. Inténtalo de nuevo en unos instantes.',
  'comum.erro.correlacao': 'Código para soporte: {correlacao}',
};
