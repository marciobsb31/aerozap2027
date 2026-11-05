import { describe, expect, it } from 'vitest';

import type { IntencaoDeBusca, OfertaDeVoo, ProblemDetail, RespostaDePing } from './tipos';

/**
 * Teste de tipo, não de comportamento: se o contrato OpenAPI mudar de forma incompatível, estes
 * literais param de compilar — e o `ng test` compila antes de rodar. É a trava que impede o
 * frontend de continuar acreditando num contrato que a API já não cumpre.
 */
describe('tipos gerados do contrato', () => {
  it('a intenção de busca aceita o mínimo publicado e tipa o opcional', () => {
    const minima: IntencaoDeBusca = {
      origin: 'GRU',
      destination: 'LIS',
      departureDate: '2026-07-10',
      passengers: { adults: 2 },
    };
    const completa: IntencaoDeBusca = {
      ...minima,
      returnDate: '2026-07-24',
      cabin: 'ECONOMY',
      currency: 'BRL',
      maxPrice: 4000,
      locale: 'es',
    };
    expect(completa.passengers.adults).toBe(2);
    expect(completa.locale).toBe('es');
  });

  it('a oferta de voo não exige mais o $defs do esquema', () => {
    const oferta: OfertaDeVoo = {
      offerId: 'of-1',
      provider: 'demo',
      demo: true,
      totalPrice: { amount: 3980.5, currency: 'BRL' },
      itineraries: [
        {
          segments: [
            {
              origin: 'GRU',
              destination: 'LIS',
              departure: '2026-07-10T22:40:00Z',
              arrival: '2026-07-11T10:05:00Z',
              marketingCarrier: 'TP',
              flightNumber: '088',
            },
          ],
        },
      ],
      checkedAt: '2026-11-04T18:20:03Z',
    };
    expect(oferta.itineraries[0].segments[0].marketingCarrier).toBe('TP');
  });

  it('o problema segue o formato RFC 9457 com correlação', () => {
    const problema: ProblemDetail = {
      type: 'urn:aerozap:problem:validation',
      title: 'Requisição inválida',
      status: 422,
      correlationId: 'abc',
    };
    expect(problema.status).toBe(422);
  });

  it('a resposta de ping só admite o estado publicado', () => {
    const resposta: RespostaDePing = {
      data: { service: 'platform-core', status: 'ok' },
      meta: { correlationId: 'abc', timestamp: '2026-11-04T18:20:03Z' },
    };
    expect(resposta.data.status).toBe('ok');
  });
});
