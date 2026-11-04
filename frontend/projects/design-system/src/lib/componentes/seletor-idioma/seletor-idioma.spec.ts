import { TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach } from 'vitest';

import { I18nService, provideIdiomaInicial } from '../../i18n/i18n';
import { SeletorIdioma } from './seletor-idioma';

describe('SeletorIdioma', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideIdiomaInicial('pt-BR')] });
  });

  it('lista os idiomas pelo nome na própria língua e marca o ativo', async () => {
    const fixture = TestBed.createComponent(SeletorIdioma);
    await fixture.whenStable();

    const campo = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    expect([...campo.options].map((o) => o.textContent?.trim())).toEqual([
      'Português (Brasil)',
      'Español',
    ]);
    expect(campo.value).toBe('pt-BR');
  });

  it('troca o idioma do serviço e anuncia a mudança numa região viva', async () => {
    const fixture = TestBed.createComponent(SeletorIdioma);
    await fixture.whenStable();

    const campo = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    campo.value = 'es';
    campo.dispatchEvent(new Event('change'));
    await fixture.whenStable();

    expect(TestBed.inject(I18nService).idioma()).toBe('es');
    const regiao = fixture.nativeElement.querySelector('[aria-live="polite"]');
    expect(regiao.textContent).toContain('Español');
  });

  it('mostra o idioma ativo quando ele já vem definido de fora', async () => {
    const fixture = TestBed.createComponent(SeletorIdioma);
    await fixture.whenStable();
    TestBed.inject(I18nService).definirIdioma('es');
    await fixture.whenStable();

    const campo = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    expect(campo.value).toBe('es');
    expect(campo.options[campo.selectedIndex].textContent?.trim()).toBe('Español');
  });

  it('tem rótulo acessível mesmo sem texto visível', async () => {
    const fixture = TestBed.createComponent(SeletorIdioma);
    await fixture.whenStable();

    const campo = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    const rotulo = fixture.nativeElement.querySelector(`label[for="${campo.id}"]`);
    expect(rotulo.textContent?.trim()).toBe('Idioma');
  });
});
