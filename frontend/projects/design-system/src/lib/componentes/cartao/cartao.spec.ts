import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { Cartao } from './cartao';

@Component({
  imports: [Cartao],
  template: `<az-cartao [titulo]="titulo()" [nivelDoTitulo]="nivel()"><p>Conteúdo</p></az-cartao>`,
})
class Hospedeiro {
  readonly titulo = signal<string | undefined>('Exemplos');
  readonly nivel = signal<2 | 3 | 4>(2);
}

describe('Cartao', () => {
  it('usa o nível de título informado para não quebrar a hierarquia da página', async () => {
    const fixture = TestBed.createComponent(Hospedeiro);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('h2').textContent).toContain('Exemplos');

    fixture.componentInstance.nivel.set(3);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('h3')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('h2')).toBeNull();
  });

  it('sem título, não insere cabeçalho vazio', async () => {
    const fixture = TestBed.createComponent(Hospedeiro);
    fixture.componentInstance.titulo.set(undefined);
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('h2, h3, h4')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Conteúdo');
  });
});
