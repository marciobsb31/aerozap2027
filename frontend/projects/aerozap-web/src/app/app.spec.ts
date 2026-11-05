import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAerozapApi } from 'api-client';
import { I18nService, provideIdiomaInicial } from 'design-system';
import { beforeEach, describe, expect, it } from 'vitest';

import { App } from './app';

describe('App (casca)', () => {
  let fixture: ComponentFixture<App>;

  beforeEach(async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAerozapApi({ urlBase: 'http://localhost:8080' }),
        provideIdiomaInicial('pt-BR'),
      ],
    });
    fixture = TestBed.createComponent(App);
    await fixture.whenStable();
  });

  const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

  it('tem um único h1 por página e marca o alvo do link de pular', () => {
    const principal = fixture.nativeElement.querySelector('main');
    expect(principal.id).toBe('conteudo-principal');
    expect(fixture.nativeElement.querySelector('a[href="#conteudo-principal"]')).toBeTruthy();
  });

  it('nomeia a navegação principal para a tecnologia assistiva', () => {
    const navegacao = fixture.nativeElement.querySelector('nav');
    expect(navegacao.getAttribute('aria-label')).toBe('Navegação principal');
  });

  it('troca o idioma da interface sem recarregar nem recriar a página', async () => {
    expect(texto()).toContain('Início');
    expect(texto()).not.toContain('Inicio\n');

    const i18n = TestBed.inject(I18nService);
    i18n.definirIdioma('es');
    await fixture.whenStable();

    // Mesma instância de componente, texto novo: é isso que "sem recarregar" significa.
    expect(texto()).toContain('Inicio');
    expect(texto()).toContain('Mis búsquedas');
    expect(document.documentElement.getAttribute('lang')).toBe('es');
  });

  it('volta ao português quando o idioma muda de novo', async () => {
    const i18n = TestBed.inject(I18nService);
    i18n.definirIdioma('es');
    await fixture.whenStable();
    i18n.definirIdioma('pt-BR');
    await fixture.whenStable();

    expect(texto()).toContain('Minhas buscas');
    expect(document.documentElement.getAttribute('lang')).toBe('pt-BR');
  });
});
