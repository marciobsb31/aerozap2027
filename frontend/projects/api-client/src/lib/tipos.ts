import type { components, operations } from './gerado/openapi';

/**
 * Nomes de domínio para os tipos gerados.
 *
 * O gerador batiza os esquemas externos pelo nome do arquivo (`"flight-offer.schema"`), o que é
 * feio de ler e, pior, amarra o código ao nome do arquivo. Este arquivo é a única camada que
 * conhece esses nomes: se o contrato reorganizar os arquivos, muda-se aqui e nada mais.
 */
export interface RespostaDeApi<T> {
  data: T;
  meta: Meta;
}
export type Meta = components['schemas']['Meta'];

export type ProblemDetail = components['schemas']['problem-detail.schema'];
export type IntencaoDeBusca = components['schemas']['search-intent.schema'];
export type OfertaDeVoo = components['schemas']['flight-offer.schema'];
export type ResultadoDeProvedor = components['schemas']['provider-result.schema'];
export type PedidoDeReserva = components['schemas']['usage-reservation-request.schema'];
export type RespostaDeReserva = components['schemas']['usage-reservation-response.schema'];
export type EnvelopeDeEvento = components['schemas']['event-envelope.schema'];

/** Corpo de sucesso de `GET /api/plataforma/v1/system/ping`, extraído da operação gerada. */
export type RespostaDePing =
  operations['pingDaPlataforma']['responses']['200']['content']['application/json'];

export type { components, operations, paths } from './gerado/openapi';
