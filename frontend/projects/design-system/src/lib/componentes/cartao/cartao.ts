import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Superfície elevada com título opcional.
 *
 * O título sai como `<h2>` ou `<h3>` conforme o nível informado, porque a hierarquia de títulos é
 * o índice que o leitor de tela usa para navegar a página — e um cartão que force sempre `h2`
 * quebra essa hierarquia quando mora dentro de uma seção.
 */
@Component({
  selector: 'az-cartao',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="az-cartao" [class.az-cartao--sutil]="sutil()">
      @if (titulo(); as texto) {
        @switch (nivelDoTitulo()) {
          @case (2) {
            <h2 class="az-cartao__titulo">{{ texto }}</h2>
          }
          @case (3) {
            <h3 class="az-cartao__titulo">{{ texto }}</h3>
          }
          @default {
            <h4 class="az-cartao__titulo">{{ texto }}</h4>
          }
        }
      }
      <div class="az-cartao__corpo">
        <ng-content />
      </div>
    </section>
  `,
  styles: `
    :host {
      display: block;
    }

    .az-cartao {
      padding: var(--az-espaco-5);
      border: 1px solid var(--az-cor-borda);
      border-radius: var(--az-raio-lg);
      background: var(--az-cor-superficie);
      box-shadow: var(--az-sombra-1);
    }

    .az-cartao--sutil {
      background: var(--az-cor-superficie-sutil);
      box-shadow: none;
    }

    .az-cartao__titulo {
      margin-bottom: var(--az-espaco-4);
      font-size: var(--az-tamanho-lg);
    }

    .az-cartao__corpo > :last-child {
      margin-bottom: 0;
    }
  `,
})
export class Cartao {
  readonly titulo = input<string | undefined>(undefined);
  readonly nivelDoTitulo = input<2 | 3 | 4>(2);
  readonly sutil = input(false);
}
