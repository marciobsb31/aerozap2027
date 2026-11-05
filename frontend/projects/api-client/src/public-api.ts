/*
 * Superfície pública do cliente de API do AeroZap.
 *
 * Os tipos de domínio são gerados do contrato OpenAPI; o que está escrito à mão aqui é só a cola
 * com o Angular (injeção, interceptador, tradução de erro).
 */

export { AerozapApi } from './lib/cliente';
export {
  AEROZAP_API_URL_BASE,
  AEROZAP_TOKEN_DE_ACESSO,
  provideAerozapApi,
} from './lib/configuracao';
export type { OpcoesDoCliente } from './lib/configuracao';
export {
  CABECALHO_DE_CORRELACAO,
  interceptadorDaApi,
  novaCorrelacao,
} from './lib/correlacao';
export { ProblemaDaApi, TIPO_DE_PROBLEMA_DESCONHECIDO, problemaDe } from './lib/problema';
export type {
  EnvelopeDeEvento,
  IntencaoDeBusca,
  Meta,
  OfertaDeVoo,
  PedidoDeReserva,
  ProblemDetail,
  RespostaDeApi,
  RespostaDePing,
  RespostaDeReserva,
  ResultadoDeProvedor,
  components,
  operations,
  paths,
} from './lib/tipos';
