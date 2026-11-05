import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach } from 'vitest';

import { CampoTexto } from './campo-texto';

@Component({
  imports: [CampoTexto],
  template: `
    <az-campo-texto
      rotulo="Para onde?"
      [(valor)]="valor"
      [dica]="dica()"
      [erro]="erro()"
      [obrigatorio]="true"
      textoObrigatorio="obrigatório"
    />
  `,
})
class Hospedeiro {
  readonly valor = signal('');
  readonly dica = signal<string | undefined>('Ex.: Lisboa em julho');
  readonly erro = signal<string | undefined>(undefined);
}

describe('CampoTexto', () => {
  let fixture: ComponentFixture<Hospedeiro>;

  beforeEach(async () => {
    fixture = TestBed.createComponent(Hospedeiro);
    await fixture.whenStable();
  });

  const campo = () => fixture.nativeElement.querySelector('input') as HTMLInputElement;
  const rotulo = () => fixture.nativeElement.querySelector('label') as HTMLLabelElement;

  it('liga o rótulo ao campo pelo id', () => {
    expect(campo().id).toBeTruthy();
    expect(rotulo().getAttribute('for')).toBe(campo().id);
  });

  it('descreve o campo pela dica quando não há erro', () => {
    const descrito = campo().getAttribute('aria-describedby');
    expect(descrito).toBeTruthy();
    expect(fixture.nativeElement.querySelector(`#${descrito}`).textContent).toContain('Lisboa');
    expect(campo().getAttribute('aria-invalid')).toBe('false');
  });

  it('ao surgir erro, troca a descrição, marca aria-invalid e anuncia por role=alert', async () => {
    fixture.componentInstance.erro.set('Escreva o que você procura.');
    await fixture.whenStable();

    const alerta = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alerta.textContent).toContain('Escreva');
    expect(campo().getAttribute('aria-invalid')).toBe('true');
    expect(campo().getAttribute('aria-describedby')).toBe(alerta.id);
  });

  it('marca o campo como obrigatório para a tecnologia assistiva', () => {
    expect(campo().getAttribute('aria-required')).toBe('true');
    expect(rotulo().textContent).toContain('obrigatório');
  });

  it('propaga o que foi digitado para quem hospeda o campo', async () => {
    campo().value = 'Lisboa';
    campo().dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(fixture.componentInstance.valor()).toBe('Lisboa');
  });
});
