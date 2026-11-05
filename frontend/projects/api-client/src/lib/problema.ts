import { HttpErrorResponse } from '@angular/common/http';

import type { ProblemDetail } from './tipos';

/** Tipos de problema que a API pode devolver (`urn:aerozap:problem:*`). */
export const TIPO_DE_PROBLEMA_DESCONHECIDO = 'urn:aerozap:problem:unknown';

/**
 * Erro da API já traduzido para algo que a interface pode usar.
 *
 * O ganho sobre o `HttpErrorResponse` cru é o `correlationId`: é o que a pessoa lê na tela e
 * repete para o suporte, e o que liga aquela tela ao log e ao rastro do pedido. Sem isso, "deu
 * erro" não tem como ser investigado.
 */
export class ProblemaDaApi extends Error {
  readonly tipo: string;
  readonly status: number;
  readonly detalhe: string | null;
  readonly correlacao: string | null;
  readonly semResposta: boolean;

  constructor(problema: {
    tipo: string;
    titulo: string;
    status: number;
    detalhe?: string | null;
    correlacao?: string | null;
    semResposta?: boolean;
  }) {
    super(problema.titulo);
    this.name = 'ProblemaDaApi';
    this.tipo = problema.tipo;
    this.status = problema.status;
    this.detalhe = problema.detalhe ?? null;
    this.correlacao = problema.correlacao ?? null;
    this.semResposta = problema.semResposta ?? false;
  }

  /** Verdadeiro quando tentar de novo tem chance de dar certo (rede, limite, indisponibilidade). */
  get vaiAdiantarTentarDeNovo(): boolean {
    return this.semResposta || this.status === 429 || this.status === 503 || this.status === 504;
  }
}

/** Converte a resposta de erro do Angular em `ProblemaDaApi`, aceitando corpo fora do padrão. */
export function problemaDe(erro: unknown): ProblemaDaApi {
  if (erro instanceof ProblemaDaApi) {
    return erro;
  }
  if (!(erro instanceof HttpErrorResponse)) {
    return new ProblemaDaApi({
      tipo: TIPO_DE_PROBLEMA_DESCONHECIDO,
      titulo: 'Falha inesperada no cliente',
      status: 0,
      semResposta: true,
    });
  }

  // status 0 significa que a requisição não chegou (rede, CORS, cancelamento). Não é erro do
  // servidor, e tratar como 500 esconderia um problema de conectividade.
  if (erro.status === 0) {
    return new ProblemaDaApi({
      tipo: TIPO_DE_PROBLEMA_DESCONHECIDO,
      titulo: 'Sem resposta do servidor',
      status: 0,
      semResposta: true,
    });
  }

  const corpo = erro.error as Partial<ProblemDetail> | string | null;
  if (corpo && typeof corpo === 'object' && typeof corpo.title === 'string') {
    return new ProblemaDaApi({
      tipo: typeof corpo.type === 'string' ? corpo.type : TIPO_DE_PROBLEMA_DESCONHECIDO,
      titulo: corpo.title,
      status: typeof corpo.status === 'number' ? corpo.status : erro.status,
      detalhe: typeof corpo.detail === 'string' ? corpo.detail : null,
      correlacao: typeof corpo.correlationId === 'string' ? corpo.correlationId : null,
    });
  }

  return new ProblemaDaApi({
    tipo: TIPO_DE_PROBLEMA_DESCONHECIDO,
    titulo: erro.statusText || 'Erro na chamada à API',
    status: erro.status,
    correlacao: erro.headers?.get('X-Correlation-Id') ?? null,
  });
}
