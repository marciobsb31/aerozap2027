import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';

import { PularParaConteudo } from './pular-para-conteudo';

describe('PularParaConteudo', () => {
  it('aponta para o id do conteúdo principal', async () => {
    const fixture = TestBed.createComponent(PularParaConteudo);
    await fixture.whenStable();
    const link = fixture.nativeElement.querySelector('a') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('#conteudo-principal');
  });
});
