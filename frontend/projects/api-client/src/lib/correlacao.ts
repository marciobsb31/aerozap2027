import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AEROZAP_API_URL_BASE, AEROZAP_TOKEN_DE_ACESSO } from './configuracao';

export const CABECALHO_DE_CORRELACAO = 'X-Correlation-Id';

/**
 * Gera um identificador de correlação por requisição.
 *
 * `crypto.randomUUID` existe em todo navegador que o produto suporta, mas só em contexto seguro;
 * o caminho alternativo cobre `http://` em desenvolvimento, onde a função não é exposta.
 */
export function novaCorrelacao(): string {
  const cripto: Crypto | undefined = globalThis.crypto;
  if (typeof cripto?.randomUUID === 'function') {
    return cripto.randomUUID();
  }
  const bytes = new Uint8Array(16);
  if (typeof cripto?.getRandomValues === 'function') {
    cripto.getRandomValues(bytes);
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = [...bytes].map((b) => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/**
 * Acrescenta correlação e token às chamadas da API.
 *
 * Só mexe no que vai para a nossa API: um interceptador que grude `Authorization` em qualquer
 * endereço vaza o token do cliente para terceiros no primeiro recurso externo que a página buscar.
 */
export const interceptadorDaApi: HttpInterceptorFn = (requisicao, seguir) => {
  const urlBase = inject(AEROZAP_API_URL_BASE, { optional: true });
  if (!urlBase || !requisicao.url.startsWith(urlBase)) {
    return seguir(requisicao);
  }

  const token = inject(AEROZAP_TOKEN_DE_ACESSO)();
  let cabecalhos = requisicao.headers;
  if (!cabecalhos.has(CABECALHO_DE_CORRELACAO)) {
    cabecalhos = cabecalhos.set(CABECALHO_DE_CORRELACAO, novaCorrelacao());
  }
  if (token && !cabecalhos.has('Authorization')) {
    cabecalhos = cabecalhos.set('Authorization', `Bearer ${token}`);
  }
  return seguir(requisicao.clone({ headers: cabecalhos }));
};
