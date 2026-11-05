import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach } from 'vitest';

import { Botao } from './botao';

@Component({
  imports: [Botao],
  template: `
    <az-botao
      [variante]="variante()"
      [carregando]="carregando()"
      [desabilitado]="desabilitado()"
      (acionar)="cliques = cliques + 1"
      >Buscar</az-botao
    >
  `,
})
class Hospedeiro {
  readonly variante = signal<'primario' | 'secundario' | 'texto'>('primario');
  readonly carregando = signal(false);
  readonly desabilitado = signal(false);
  cliques = 0;
}

describe('Botao', () => {
  let fixture: ComponentFixture<Hospedeiro>;

  beforeEach(async () => {
    fixture = TestBed.createComponent(Hospedeiro);
    await fixture.whenStable();
  });

  function elemento(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('button');
  }

  it('renderiza um <button> nativo com o texto projetado', () => {
    expect(elemento().tagName).toBe('BUTTON');
    expect(elemento().textContent?.trim()).toBe('Buscar');
  });

  it('emite o evento ao clicar', () => {
    elemento().click();
    expect(fixture.componentInstance.cliques).toBe(1);
  });

  it('enquanto carrega, fica desabilitado, marca aria-busy e não emite', async () => {
    fixture.componentInstance.carregando.set(true);
    await fixture.whenStable();

    expect(elemento().disabled).toBe(true);
    expect(elemento().getAttribute('aria-busy')).toBe('true');

    elemento().click();
    expect(fixture.componentInstance.cliques).toBe(0);
  });

  it('não emite quando desabilitado por regra', async () => {
    fixture.componentInstance.desabilitado.set(true);
    await fixture.whenStable();
    elemento().click();
    expect(fixture.componentInstance.cliques).toBe(0);
  });

  it('aplica a classe da variante escolhida', async () => {
    fixture.componentInstance.variante.set('secundario');
    await fixture.whenStable();
    expect(elemento().classList.contains('az-botao--secundario')).toBe(true);
  });
});
