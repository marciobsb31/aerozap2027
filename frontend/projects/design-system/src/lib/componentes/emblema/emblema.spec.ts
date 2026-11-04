import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { Emblema, TomDoEmblema } from './emblema';

@Component({
  imports: [Emblema],
  template: `<az-emblema [tom]="tom()">Conectado</az-emblema>`,
})
class Hospedeiro {
  readonly tom = signal<TomDoEmblema>('sucesso');
}

describe('Emblema', () => {
  it('não deixa a cor sozinha com o significado: o texto diz o estado', async () => {
    const fixture = TestBed.createComponent(Hospedeiro);
    await fixture.whenStable();

    const emblema = fixture.nativeElement.querySelector('.az-emblema') as HTMLElement;
    expect(emblema.classList.contains('az-emblema--sucesso')).toBe(true);
    expect(emblema.textContent?.trim()).toBe('Conectado');
    expect(fixture.nativeElement.querySelector('.az-emblema__ponto').getAttribute('aria-hidden'))
      .toBe('true');
  });
});
