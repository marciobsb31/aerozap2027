/**
 * Idiomas que o produto fala. A ordem é a que aparece no seletor.
 *
 * `pt-BR` é o idioma de origem (as chaves são escritas nele) e também o retorno quando o pedido
 * não existe: é melhor mostrar português do que mostrar a chave crua para a pessoa.
 */
export const IDIOMAS = ['pt-BR', 'es'] as const;

export type Idioma = (typeof IDIOMAS)[number];

export const IDIOMA_PADRAO: Idioma = 'pt-BR';

/** Nome de cada idioma no próprio idioma: ninguém procura "espanhol" numa interface em espanhol. */
export const NOME_DO_IDIOMA: Readonly<Record<Idioma, string>> = {
  'pt-BR': 'Português (Brasil)',
  es: 'Español',
};

export function ehIdiomaConhecido(valor: unknown): valor is Idioma {
  return typeof valor === 'string' && (IDIOMAS as readonly string[]).includes(valor);
}

/**
 * Resolve o idioma a partir de uma etiqueta BCP 47 vinda do navegador ou de um cabeçalho.
 * Compara apenas a parte principal, para que `es-AR`, `es-MX` e `es-419` caiam todos em `es`.
 */
export function idiomaMaisProximo(etiquetas: readonly string[]): Idioma | null {
  for (const etiqueta of etiquetas) {
    if (ehIdiomaConhecido(etiqueta)) {
      return etiqueta;
    }
    const principal = etiqueta.toLowerCase().split('-')[0];
    const achado = IDIOMAS.find((idioma) => idioma.toLowerCase().split('-')[0] === principal);
    if (achado) {
      return achado;
    }
  }
  return null;
}
