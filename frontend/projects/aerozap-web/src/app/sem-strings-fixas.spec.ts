import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * Guarda o critério "nenhuma string de interface escrita no código".
 *
 * Revisão humana não sustenta isso: basta um `<p>Enviando…</p>` para a tela ficar meio traduzida,
 * e meio traduzida é o pior dos estados — passa pelo olho de quem fala português e quebra para
 * quem fala espanhol. Então quem verifica é a suíte: o teste varre os modelos da aplicação e falha
 * ao encontrar texto que não venha do dicionário.
 *
 * Fora do escopo de propósito: o design system, onde os textos padrão existem para ser
 * sobrescritos pela aplicação, e os próprios dicionários.
 */

const RAIZ_DA_APLICACAO = 'projects/aerozap-web/src';

/** Atributos que chegam à tela como texto; aqui só valem ligados a uma expressão. */
const ATRIBUTOS_DE_TEXTO = [
  'rotulo',
  'dica',
  'erro',
  'exemplo',
  'textoObrigatorio',
  'rotuloAcessivel',
  'titulo',
  'placeholder',
  'title',
  'alt',
  'aria-label',
  'label',
];

function arquivos(diretorio: string, extensao: string): string[] {
  const achados: string[] = [];
  for (const entrada of readdirSync(diretorio, { withFileTypes: true })) {
    const caminho = join(diretorio, entrada.name);
    if (entrada.isDirectory()) {
      achados.push(...arquivos(caminho, extensao));
    } else if (entrada.name.endsWith(extensao)) {
      achados.push(caminho);
    }
  }
  return achados;
}

/** Remove tudo que não é texto exibido: comentários, interpolações e a sintaxe de controle. */
function semInterpolacaoNemControle(modelo: string): string {
  return modelo
    .replace(/<!--[\s\S]*?-->/g, ' ')
    .replace(/\{\{[\s\S]*?\}\}/g, ' ')
    .replace(/@(if|else if|for|switch|case|defer)\b[^{\n]*\{/g, ' ')
    .replace(/@(else|default|empty|placeholder|loading|error)\b[^{\n]*\{/g, ' ')
    .replace(/[{}]/g, ' ');
}

/** Texto solto entre tags, ignorando espaços, pontuação e entidades. */
function textoEntreTags(modelo: string): string[] {
  const achados: string[] = [];
  for (const [, trecho] of modelo.matchAll(/>([^<]+)</g)) {
    const limpo = trecho.replace(/&[a-z]+;/gi, ' ').trim();
    if (/\p{L}{2,}/u.test(limpo)) {
      achados.push(limpo);
    }
  }
  return achados;
}

/** Atributo de texto com valor literal em vez de ligação. */
function atributosLiterais(modelo: string): string[] {
  const achados: string[] = [];
  for (const atributo of ATRIBUTOS_DE_TEXTO) {
    const expressao = new RegExp(`(?<![[\\w-])${atributo}\\s*=\\s*"([^"]*)"`, 'gi');
    for (const [, valor] of modelo.matchAll(expressao)) {
      if (/\p{L}{2,}/u.test(valor)) {
        achados.push(`${atributo}="${valor}"`);
      }
    }
  }
  return achados;
}

describe('internacionalização da aerozap-web', () => {
  const modelos = arquivos(RAIZ_DA_APLICACAO, '.html').filter(
    (caminho) => !caminho.endsWith('index.html'),
  );

  it('encontra os modelos da aplicação (senão este teste não prova nada)', () => {
    expect(modelos.length).toBeGreaterThanOrEqual(4);
  });

  it('não tem texto de interface escrito nos modelos', () => {
    const problemas: string[] = [];
    for (const caminho of modelos) {
      const modelo = semInterpolacaoNemControle(readFileSync(caminho, 'utf8'));
      for (const trecho of textoEntreTags(modelo)) {
        problemas.push(`${caminho}: "${trecho}"`);
      }
      for (const trecho of atributosLiterais(modelo)) {
        problemas.push(`${caminho}: ${trecho}`);
      }
    }
    expect(problemas, `texto fora do dicionário:\n${problemas.join('\n')}`).toEqual([]);
  });

  it('nenhum componente da aplicação esconde o modelo dentro do TypeScript', () => {
    const comModeloEmbutido = arquivos(RAIZ_DA_APLICACAO, '.ts')
      .filter((caminho) => !caminho.endsWith('.spec.ts'))
      .filter((caminho) => /\btemplate\s*:/.test(readFileSync(caminho, 'utf8')));
    expect(comModeloEmbutido).toEqual([]);
  });

  it('o idioma do documento é declarado no index.html', () => {
    const indice = readFileSync(join(RAIZ_DA_APLICACAO, 'index.html'), 'utf8');
    expect(indice).toMatch(/<html[^>]*\slang="pt-BR"/);
  });
});
