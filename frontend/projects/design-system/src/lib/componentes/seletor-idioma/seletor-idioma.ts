import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';

import { I18nService } from '../../i18n/i18n';
import { IDIOMAS, Idioma, NOME_DO_IDIOMA, ehIdiomaConhecido } from '../../i18n/idioma';

/**
 * Troca o idioma da interface sem recarregar a página.
 *
 * É um `<select>` nativo porque o componente de lista do sistema operacional já funciona com
 * teclado, toque e leitor de tela em todas as plataformas — reimplementar isso em `div` é o
 * caminho mais curto para um seletor que não abre no celular.
 *
 * A mudança também é anunciada numa região viva: para quem usa leitor de tela, a tela inteira
 * mudou de língua, e isso precisa ser dito em voz alta.
 */
@Component({
  selector: 'az-seletor-idioma',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="az-seletor">
      <label class="az-visualmente-oculto" for="az-seletor-idioma">{{ i18n.t('idioma.rotulo') }}</label>
      <select id="az-seletor-idioma" class="az-seletor__campo" (change)="aoEscolher($event)">
        @for (idioma of idiomas; track idioma) {
          <!--
            A seleção vai na opção, e não num [value] no <select>: o valor é aplicado antes de as
            opções existirem, e o campo ficaria mostrando a primeira da lista enquanto o resto da
            tela já está no outro idioma.
          -->
          <option [value]="idioma" [selected]="idioma === i18n.idioma()">{{ nomes[idioma] }}</option>
        }
      </select>
      <p class="az-visualmente-oculto" role="status" aria-live="polite">{{ anuncio() }}</p>
    </div>
  `,
  styles: `
    .az-seletor__campo {
      min-height: var(--az-alvo-toque);
      padding: 0 var(--az-espaco-3);
      border: 1px solid var(--az-cor-borda-forte);
      border-radius: var(--az-raio-md);
      background: var(--az-cor-superficie);
      color: var(--az-cor-texto);
      font: inherit;
      font-size: var(--az-tamanho-sm);
      cursor: pointer;
    }
  `,
})
export class SeletorIdioma {
  protected readonly i18n = inject(I18nService);
  protected readonly idiomas = IDIOMAS;
  protected readonly nomes = NOME_DO_IDIOMA;
  protected readonly anuncio = signal('');

  protected aoEscolher(evento: Event): void {
    const escolhido = (evento.target as HTMLSelectElement).value;
    if (!ehIdiomaConhecido(escolhido)) {
      return;
    }
    this.i18n.definirIdioma(escolhido as Idioma);
    this.anuncio.set(this.i18n.t('idioma.alterado', { idioma: NOME_DO_IDIOMA[escolhido] }));
  }
}
