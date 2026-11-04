import { Pipe, PipeTransform, inject } from '@angular/core';

import { ChaveDeTraducao } from './dicionario.pt-br';
import { I18nService, ParametrosDeTraducao } from './i18n';

/**
 * `{{ 'inicio.titulo' | azT }}` para quem preferir pipe a chamada de método.
 *
 * O pipe é impuro de propósito: um pipe puro memoriza pelos argumentos, e como a chave não muda
 * quando o idioma troca, ele devolveria o texto antigo justamente no momento em que precisa mudar.
 */
@Pipe({ name: 'azT', pure: false })
export class TraduzirPipe implements PipeTransform {
  private readonly i18n = inject(I18nService);

  transform(chave: ChaveDeTraducao, parametros?: ParametrosDeTraducao): string {
    return this.i18n.t(chave, parametros);
  }
}
