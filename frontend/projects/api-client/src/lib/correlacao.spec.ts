import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { provideAerozapApi } from './configuracao';
import { CABECALHO_DE_CORRELACAO, interceptadorDaApi, novaCorrelacao } from './correlacao';

const URL_BASE = 'http://localhost:8080';

describe('interceptador da API', () => {
  let http: HttpTestingController;
  let cliente: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([interceptadorDaApi])),
        provideHttpClientTesting(),
        provideAerozapApi({ urlBase: URL_BASE, tokenDeAcesso: () => 'token-de-teste' }),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    cliente = TestBed.inject(HttpClient);
  });

  it('põe correlação e token nas chamadas da nossa API', () => {
    cliente.get(`${URL_BASE}/api/plataforma/v1/system/ping`).subscribe();
    const pedido = http.expectOne(`${URL_BASE}/api/plataforma/v1/system/ping`);

    expect(pedido.request.headers.get(CABECALHO_DE_CORRELACAO)).toMatch(
      /^[0-9a-f-]{36}$/i,
    );
    expect(pedido.request.headers.get('Authorization')).toBe('Bearer token-de-teste');
    pedido.flush({});
  });

  it('não vaza o token para endereços de terceiros', () => {
    cliente.get('https://exemplo.com/recurso').subscribe();
    const pedido = http.expectOne('https://exemplo.com/recurso');

    expect(pedido.request.headers.has('Authorization')).toBe(false);
    expect(pedido.request.headers.has(CABECALHO_DE_CORRELACAO)).toBe(false);
    pedido.flush({});
  });

  it('respeita a correlação que já vier definida', () => {
    cliente
      .get(`${URL_BASE}/api/plataforma/v1/system/ping`, {
        headers: { [CABECALHO_DE_CORRELACAO]: 'veio-de-fora' },
      })
      .subscribe();
    const pedido = http.expectOne(`${URL_BASE}/api/plataforma/v1/system/ping`);
    expect(pedido.request.headers.get(CABECALHO_DE_CORRELACAO)).toBe('veio-de-fora');
    pedido.flush({});
  });

  it('gera identificadores no formato UUID e sem repetir', () => {
    const gerados = new Set(Array.from({ length: 50 }, () => novaCorrelacao()));
    expect(gerados.size).toBe(50);
    for (const valor of gerados) {
      expect(valor).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i);
    }
  });
});
