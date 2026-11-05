import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type TomDoEmblema = 'neutro' | 'sucesso' | 'aviso' | 'erro';

/**
 * Etiqueta curta de estado.
 *
 * A cor nunca carrega o significado sozinha: o texto do emblema já diz o estado, e quando há um
 * ponto colorido ele é decorativo (`aria-hidden`). Isso é o que faz o estado continuar legível para
 * quem não distingue as cores — cerca de uma em doze pessoas, na prática.
 */
@Component({
  selector: 'az-emblema',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="az-emblema"
      [class.az-emblema--sucesso]="tom() === 'sucesso'"
      [class.az-emblema--aviso]="tom() === 'aviso'"
      [class.az-emblema--erro]="tom() === 'erro'"
    >
      @if (comPonto()) {
        <span class="az-emblema__ponto" aria-hidden="true"></span>
      }
      <ng-content />
    </span>
  `,
  styles: `
    :host {
      display: inline-flex;
    }

    .az-emblema {
      display: inline-flex;
      align-items: center;
      gap: var(--az-espaco-2);
      padding: var(--az-espaco-1) var(--az-espaco-3);
      border-radius: var(--az-raio-cheio);
      background: var(--az-cor-superficie-sutil);
      color: var(--az-cor-texto);
      font-size: var(--az-tamanho-xs);
      font-weight: var(--az-peso-medio);
      white-space: nowrap;
    }

    .az-emblema__ponto {
      width: 0.5em;
      height: 0.5em;
      border-radius: 50%;
      background: currentcolor;
    }

    .az-emblema--sucesso {
      background: var(--az-cor-sucesso-fundo);
      color: var(--az-cor-sucesso);
    }

    .az-emblema--aviso {
      background: var(--az-cor-aviso-fundo);
      color: var(--az-cor-aviso);
    }

    .az-emblema--erro {
      background: var(--az-cor-erro-fundo);
      color: var(--az-cor-erro);
    }
  `,
})
export class Emblema {
  readonly tom = input<TomDoEmblema>('neutro');
  readonly comPonto = input(true);
}
