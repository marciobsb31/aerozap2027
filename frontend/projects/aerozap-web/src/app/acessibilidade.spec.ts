import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAerozapApi } from 'api-client';
import { I18nService, provideIdiomaInicial } from 'design-system';
import axe, { type Result } from 'axe-core';
import { beforeEach, describe, expect, it } from 'vitest';

import { App } from './app';
import { Busca } from './paginas/busca/busca';
import { Inicio } from './paginas/inicio/inicio';
import { NaoEncontrada } from './paginas/nao-encontrada/nao-encontrada';

/**
 * Acessibilidade verificada por máquina, em cima do DOM que a aplicação realmente produz.
 *
 * O axe não substitui teste com pessoas — ele pega a classe de defeito que é barata de evitar e
 * caríssima de descobrir depois: campo sem rótulo, botão sem nome, hierarquia de título quebrada,
 * `aria` apontando para id que não existe. O que ele não vê (ordem de foco que faz sentido, texto
 * que explica o suficiente) continua sendo trabalho de revisão.
 *
 * `color-contrast` fica desligado porque o jsdom não calcula layout nem resolve variáveis CSS, e a
 * regra devolveria "indeterminado" em tudo. O contraste da paleta é conferido nos tokens.
 */

const URL_BASE = 'http://localhost:8080';

async function violacoes(elemento: Element): Promise<Result[]> {
  const resultado = await axe.run(elemento, {
    rules: { 'color-contrast': { enabled: false } },
    resultTypes: ['violations'],
  });
  return resultado.violations;
}

function descrever(encontradas: Result[]): string {
  return encontradas
    .map((v) => `${v.id} (${v.impact}): ${v.help}\n  ${v.nodes.map((n) => n.html).join('\n  ')}`)
    .join('\n');
}

describe('acessibilidade das telas', () => {
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
  });

  async function montar<T>(componente: Type<T>): Promise<ComponentFixture<T>> {
    const fixture = TestBed.createComponent(componente);
    await fixture.whenStable();
    return fixture;
  }

  it('a casca da aplicação não tem violações conhecidas', async () => {
    const fixture = await montar(App);
    const encontradas = await violacoes(fixture.nativeElement);
    expect(encontradas, descrever(encontradas)).toEqual([]);
  });

  it('a tela inicial não tem violações conhecidas, respondendo ou não a plataforma', async () => {
    const fixture = await montar(Inicio);
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${URL_BASE}/api/plataforma/v1/system/ping`).flush({
      data: { service: 'platform-core', status: 'ok' },
      meta: { correlationId: 'abc', timestamp: '2026-11-04T18:20:03Z' },
    });
    await fixture.whenStable();

    const comSucesso = await violacoes(fixture.nativeElement);
    expect(comSucesso, descrever(comSucesso)).toEqual([]);
  });

  it('a tela inicial com erro de validação continua acessível', async () => {
    const fixture = await montar(Inicio);
    const http = TestBed.inject(HttpTestingController);
    http
      .expectOne(`${URL_BASE}/api/plataforma/v1/system/ping`)
      .flush({ title: 'Indisponível', status: 503 }, { status: 503, statusText: 'erro' });
    await fixture.whenStable();

    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(
      new Event('submit', { cancelable: true }),
    );
    await fixture.whenStable();

    const encontradas = await violacoes(fixture.nativeElement);
    expect(encontradas, descrever(encontradas)).toEqual([]);
  });

  it('as páginas de busca e de endereço inexistente não têm violações conhecidas', async () => {
    for (const componente of [Busca, NaoEncontrada] as Type<unknown>[]) {
      const fixture = await montar(componente);
      const encontradas = await violacoes(fixture.nativeElement);
      expect(encontradas, `${componente.name}: ${descrever(encontradas)}`).toEqual([]);
    }
  });

  it('em espanhol a interface segue sem violações', async () => {
    const fixture = await montar(App);
    TestBed.inject(I18nService).definirIdioma('es');
    await fixture.whenStable();

    const encontradas = await violacoes(fixture.nativeElement);
    expect(encontradas, descrever(encontradas)).toEqual([]);
  });
});
