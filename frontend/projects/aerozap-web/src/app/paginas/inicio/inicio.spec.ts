import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { provideAerozapApi } from 'api-client';
import { I18nService, provideIdiomaInicial } from 'design-system';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { Inicio } from './inicio';

const URL_BASE = 'http://localhost:8080';
const CAMINHO_DO_PING = `${URL_BASE}/api/plataforma/v1/system/ping`;

describe('Inicio', () => {
  let fixture: ComponentFixture<Inicio>;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAerozapApi({ urlBase: URL_BASE }),
        provideIdiomaInicial('pt-BR'),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Inicio);
  });

  const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';
  const campo = () => fixture.nativeElement.querySelector('input') as HTMLInputElement;
  const formulario = () => fixture.nativeElement.querySelector('form') as HTMLFormElement;

  function responderPing(corpo?: unknown): void {
    const pedido = http.expectOne(CAMINHO_DO_PING);
    if (corpo === undefined) {
      pedido.flush(
        { type: 'urn:aerozap:problem:unavailable', title: 'Indisponível', status: 503 },
        { status: 503, statusText: 'Service Unavailable' },
      );
    } else {
      pedido.flush(corpo);
    }
  }

  it('consulta o estado da plataforma ao abrir e mostra que está respondendo', async () => {
    await fixture.whenStable();
    responderPing({
      data: { service: 'platform-core', status: 'ok' },
      meta: { correlationId: 'abc', timestamp: '2026-11-04T18:20:03Z' },
    });
    await fixture.whenStable();

    expect(texto()).toContain('Plataforma respondendo');
  });

  it('quando a plataforma não responde, diz isso e oferece tentar de novo', async () => {
    await fixture.whenStable();
    responderPing();
    await fixture.whenStable();

    expect(texto()).toContain('Plataforma sem resposta');
    const tentarDeNovo = [...fixture.nativeElement.querySelectorAll('button')].find(
      (b: HTMLButtonElement) => b.textContent?.includes('Verificar de novo'),
    ) as HTMLButtonElement;
    expect(tentarDeNovo).toBeTruthy();

    tentarDeNovo.click();
    await fixture.whenStable();
    responderPing({
      data: { service: 'platform-core', status: 'ok' },
      meta: { correlationId: 'def', timestamp: '2026-11-04T18:21:03Z' },
    });
    await fixture.whenStable();
    expect(texto()).toContain('Plataforma respondendo');
  });

  it('não navega com o campo vazio e explica o que falta', async () => {
    await fixture.whenStable();
    responderPing();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    formulario().dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    expect(navegar).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'Escreva o que você procura',
    );
  });

  it('leva a consulta para a página de busca', async () => {
    await fixture.whenStable();
    responderPing();
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    campo().value = 'de São Paulo para Lisboa em julho';
    campo().dispatchEvent(new Event('input'));
    await fixture.whenStable();
    formulario().dispatchEvent(new Event('submit', { cancelable: true }));
    await fixture.whenStable();

    expect(navegar).toHaveBeenCalledWith(['/busca'], {
      queryParams: { q: 'de São Paulo para Lisboa em julho' },
    });
  });

  it('o texto da tela acompanha a troca de idioma', async () => {
    await fixture.whenStable();
    responderPing({
      data: { service: 'platform-core', status: 'ok' },
      meta: { correlationId: 'abc', timestamp: '2026-11-04T18:20:03Z' },
    });
    await fixture.whenStable();
    expect(texto()).toContain('Buscar passagens');

    TestBed.inject(I18nService).definirIdioma('es');
    await fixture.whenStable();

    expect(texto()).toContain('Buscar vuelos');
    expect(texto()).toContain('La plataforma responde');
  });
});
