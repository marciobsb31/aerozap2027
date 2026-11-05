/*
 * Superfície pública do design system AeroZap.
 *
 * O que não estiver listado aqui é detalhe interno e pode mudar sem aviso. Estilos vêm do arquivo
 * `design-system/estilos.scss`, importado uma vez por aplicação.
 */

// i18n
export * from './lib/i18n/idioma';
export { DICIONARIO_PT_BR } from './lib/i18n/dicionario.pt-br';
export type { ChaveDeTraducao, Dicionario } from './lib/i18n/dicionario.pt-br';
export { DICIONARIO_ES } from './lib/i18n/dicionario.es';
export { I18nService, IDIOMA_INICIAL, provideIdiomaInicial } from './lib/i18n/i18n';
export type { ParametrosDeTraducao } from './lib/i18n/i18n';
export { TraduzirPipe } from './lib/i18n/traduzir.pipe';

// componentes
export { Botao } from './lib/componentes/botao/botao';
export type { VarianteDeBotao } from './lib/componentes/botao/botao';
export { CampoTexto } from './lib/componentes/campo-texto/campo-texto';
export { Cartao } from './lib/componentes/cartao/cartao';
export { Emblema } from './lib/componentes/emblema/emblema';
export type { TomDoEmblema } from './lib/componentes/emblema/emblema';
export { PularParaConteudo } from './lib/componentes/pular-para-conteudo/pular-para-conteudo';
export { SeletorIdioma } from './lib/componentes/seletor-idioma/seletor-idioma';
