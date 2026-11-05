import { TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach } from 'vitest';

import { DICIONARIO_ES } from './dicionario.es';
import { DICIONARIO_PT_BR } from './dicionario.pt-br';
import { I18nService, provideIdiomaInicial } from './i18n';
import { IDIOMAS, idiomaMaisProximo } from './idioma';

describe('I18nService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  function servico(): I18nService {
    TestBed.configureTestingModule({ providers: [provideIdiomaInicial('pt-BR')] });
    return TestBed.inject(I18nService);
  }

  it('devolve o texto do idioma ativo', () => {
    const i18n = servico();
    expect(i18n.t('inicio.busca.enviar')).toBe('Buscar passagens');
  });

  it('troca de idioma sem recriar o serviço', () => {
    const i18n = servico();
    i18n.definirIdioma('es');
    expect(i18n.idioma()).toBe('es');
    expect(i18n.t('inicio.busca.enviar')).toBe('Buscar vuelos');
  });

  it('interpola parâmetros e deixa intacto o que não recebeu valor', () => {
    const i18n = servico();
    expect(i18n.t('rodape.direitos', { ano: 2026 })).toContain('2026');
    expect(i18n.t('rodape.direitos')).toContain('{ano}');
  });

  it('lembra a escolha e marca o idioma no documento', () => {
    const i18n = servico();
    i18n.definirIdioma('es');
    expect(localStorage.getItem('aerozap.idioma')).toBe('es');
    expect(document.documentElement.getAttribute('lang')).toBe('es');
  });

  it('ignora idioma desconhecido em vez de quebrar a interface', () => {
    const i18n = servico();
    i18n.definirIdioma('fr' as never);
    expect(i18n.idioma()).toBe('pt-BR');
  });

  it('resolve variantes regionais para o idioma mais próximo', () => {
    expect(idiomaMaisProximo(['es-AR', 'en'])).toBe('es');
    expect(idiomaMaisProximo(['pt-PT'])).toBe('pt-BR');
    expect(idiomaMaisProximo(['ja'])).toBeNull();
  });
});

describe('dicionários', () => {
  it('cobrem exatamente as mesmas chaves em todos os idiomas', () => {
    const origem = Object.keys(DICIONARIO_PT_BR).sort();
    expect(Object.keys(DICIONARIO_ES).sort()).toEqual(origem);
  });

  it('não deixam texto vazio nem chave repetida como valor', () => {
    for (const idioma of IDIOMAS) {
      const dicionario = idioma === 'pt-BR' ? DICIONARIO_PT_BR : DICIONARIO_ES;
      for (const [chave, texto] of Object.entries(dicionario)) {
        expect(texto.trim(), `${idioma}/${chave}`).not.toBe('');
        expect(texto, `${idioma}/${chave}`).not.toBe(chave);
      }
    }
  });

  it('mantêm os mesmos marcadores de interpolação nas duas línguas', () => {
    const marcadores = (texto: string) => (texto.match(/\{\w+\}/g) ?? []).sort();
    for (const chave of Object.keys(DICIONARIO_PT_BR) as (keyof typeof DICIONARIO_PT_BR)[]) {
      expect(marcadores(DICIONARIO_ES[chave]), chave).toEqual(marcadores(DICIONARIO_PT_BR[chave]));
    }
  });
});
