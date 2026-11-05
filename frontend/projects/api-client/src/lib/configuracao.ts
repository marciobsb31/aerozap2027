import {
  EnvironmentProviders,
  InjectionToken,
  Provider,
  makeEnvironmentProviders,
} from '@angular/core';

/** Endereço base da API (o gateway). Sem barra no fim. */
export const AEROZAP_API_URL_BASE = new InjectionToken<string>('AEROZAP_API_URL_BASE');

/** Função que devolve o token de acesso atual, ou `null` quando ninguém está autenticado. */
export const AEROZAP_TOKEN_DE_ACESSO = new InjectionToken<() => string | null>(
  'AEROZAP_TOKEN_DE_ACESSO',
  { factory: () => () => null },
);

export interface OpcoesDoCliente {
  urlBase: string;
  /** Opcional: enquanto não há autenticação, as chamadas saem sem `Authorization`. */
  tokenDeAcesso?: () => string | null;
}

/** Registra o cliente da API na aplicação. */
export function provideAerozapApi(opcoes: OpcoesDoCliente): EnvironmentProviders {
  const provedores: Provider[] = [
    { provide: AEROZAP_API_URL_BASE, useValue: semBarrasNoFim(opcoes.urlBase) },
  ];
  if (opcoes.tokenDeAcesso) {
    provedores.push({ provide: AEROZAP_TOKEN_DE_ACESSO, useValue: opcoes.tokenDeAcesso });
  }
  return makeEnvironmentProviders(provedores);
}

/** Sem expressão regular: `/\/+$/` fica lenta em textos longos cheios de barras. */
export function semBarrasNoFim(url: string): string {
  let fim = url.length;
  while (fim > 0 && url[fim - 1] === '/') {
    fim--;
  }
  return url.slice(0, fim);
}
