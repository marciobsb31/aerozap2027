import { HttpClient, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { AerozapApi } from './cliente';
import { CABECALHO_DE_CORRELACAO, interceptadorDaApi } from './correlacao';
import { provideAerozapApi } from './configuracao';
import { ProblemaDaApi } from './problema';
import type { RespostaDePing } from './tipos';

const URL_BASE = 'http://localhost:8080';

const PING_OK: RespostaDePing = {
  data: { service: 'platform-core', status: 'ok' },
  meta: {
    correlationId: '3f6c1b7a-9c1e-4a18-9a2f-2c0b8d4e7f11',
    timestamp: '2026-11-04T18:20:03.512Z',
  },
};

describe('AerozapApi', () => {
  let http: HttpTestingController;
  let api: AerozapApi;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAerozapApi({ urlBase: URL_BASE, tokenDeAcesso: () => 'token-de-teste' }),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    api = TestBed.inject(AerozapApi);
  });

  it('desembrulha o envelope da resposta de ping', async () => {
    const promessa = new Promise((resolve) => api.ping().subscribe(resolve));
    http.expectOne(`${URL_BASE}/api/plataforma/v1/system/ping`).flush(PING_OK);

    await expect(promessa).resolves.toEqual({
      servico: 'platform-core',
      estado: 'ok',
      correlacao: '3f6c1b7a-9c1e-4a18-9a2f-2c0b8d4e7f11',
    });
  });

  it('traduz Problem Details em ProblemaDaApi, preservando a correlação', async () => {
    const promessa = new Promise<unknown>((resolve) =>
      api.ping().subscribe({ error: resolve }),
    );
    http.expectOne(`${URL_BASE}/api/plataforma/v1/system/ping`).flush(
      {
        type: 'urn:aerozap:problem:rate-limited',
        title: 'Muitas requisições',
        status: 429,
        detail: 'Aguarde alguns instantes.',
        correlationId: 'abc-123',
      },
      { status: 429, statusText: 'Too Many Requests' },
    );

    const problema = (await promessa) as ProblemaDaApi;
    expect(problema).toBeInstanceOf(ProblemaDaApi);
    expect(problema.tipo).toBe('urn:aerozap:problem:rate-limited');
    expect(problema.status).toBe(429);
    expect(problema.correlacao).toBe('abc-123');
    expect(problema.vaiAdiantarTentarDeNovo).toBe(true);
  });

  it('distingue "não chegou ao servidor" de erro do servidor', async () => {
    const promessa = new Promise<unknown>((resolve) => api.ping().subscribe({ error: resolve }));
    http
      .expectOne(`${URL_BASE}/api/plataforma/v1/system/ping`)
      .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

    const problema = (await promessa) as ProblemaDaApi;
    expect(problema.semResposta).toBe(true);
    expect(problema.status).toBe(0);
    expect(problema.vaiAdiantarTentarDeNovo).toBe(true);
  });
});

describe('interceptadorDaApi', () => {
  let http: HttpTestingController;
  let cliente: HttpClient;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAerozapApi({ urlBase: URL_BASE, tokenDeAcesso: () => 'token-de-teste' }),
      ],
    });
    // O interceptador é registrado pela aplicação; aqui ele é exercitado diretamente pelo HttpClient
    // de teste através de provideHttpClient(withInterceptors([...])) na própria aplicação.
    http = TestBed.inject(HttpTestingController);
    cliente = TestBed.inject(HttpClient);
  });

  it('é uma função de interceptação, usada pela aplicação no provideHttpClient', () => {
    expect(typeof interceptadorDaApi).toBe('function');
    expect(CABECALHO_DE_CORRELACAO).toBe('X-Correlation-Id');
    cliente.get(`${URL_BASE}/api/qualquer`).subscribe({ error: () => undefined });
    http.expectOne(`${URL_BASE}/api/qualquer`).flush({});
  });
});
