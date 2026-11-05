import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

export type VarianteDeBotao = 'primario' | 'secundario' | 'texto';

/**
 * Botão do produto.
 *
 * É um `<button>` de verdade por dentro (e não uma `div` clicável) porque só o elemento nativo
 * entra na ordem de tabulação, responde a Espaço e Enter e é anunciado como botão pelo leitor de
 * tela. Enquanto carrega, continua focável mas não aciona: perder o foco no meio de um clique
 * desorienta quem navega por teclado.
 */
@Component({
  selector: 'az-botao',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      class="az-botao"
      [class.az-botao--primario]="variante() === 'primario'"
      [class.az-botao--secundario]="variante() === 'secundario'"
      [class.az-botao--texto]="variante() === 'texto'"
      [class.az-botao--carregando]="carregando()"
      [attr.type]="tipo()"
      [attr.aria-label]="rotuloAcessivel()"
      [attr.aria-busy]="carregando()"
      [disabled]="bloqueado()"
      (click)="aoClicar()"
    >
      @if (carregando()) {
        <span class="az-botao__girador" aria-hidden="true"></span>
      }
      <span class="az-botao__texto"><ng-content /></span>
    </button>
  `,
  styles: `
    :host {
      display: inline-flex;
    }

    .az-botao {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: var(--az-espaco-2);
      min-height: var(--az-alvo-toque);
      padding: 0 var(--az-espaco-5);
      border: 1px solid transparent;
      border-radius: var(--az-raio-cheio);
      font: inherit;
      font-weight: var(--az-peso-forte);
      line-height: 1;
      cursor: pointer;
      transition:
        background var(--az-transicao),
        border-color var(--az-transicao),
        transform var(--az-transicao);
    }

    .az-botao:active:not(:disabled) {
      transform: translateY(1px);
    }

    .az-botao:disabled {
      cursor: not-allowed;
      opacity: 0.55;
    }

    .az-botao--primario {
      background: var(--az-cor-acao);
      color: var(--az-cor-acao-texto);
    }

    .az-botao--primario:hover:not(:disabled) {
      background: var(--az-cor-acao-forte);
      color: var(--az-cor-texto-invertido);
    }

    .az-botao--secundario {
      background: transparent;
      border-color: var(--az-cor-borda-forte);
      color: var(--az-cor-texto);
    }

    .az-botao--secundario:hover:not(:disabled) {
      background: var(--az-cor-superficie-sutil);
    }

    .az-botao--texto {
      min-height: auto;
      padding: var(--az-espaco-2);
      background: none;
      color: var(--az-cor-marca);
      text-decoration: underline;
      text-underline-offset: 0.2em;
    }

    .az-botao--carregando {
      cursor: progress;
    }

    .az-botao__girador {
      width: 1em;
      height: 1em;
      border: 2px solid currentcolor;
      border-right-color: transparent;
      border-radius: 50%;
      animation: az-girar 700ms linear infinite;
    }

    @keyframes az-girar {
      to {
        transform: rotate(360deg);
      }
    }
  `,
})
export class Botao {
  readonly variante = input<VarianteDeBotao>('primario');
  readonly tipo = input<'button' | 'submit' | 'reset'>('button');
  readonly carregando = input(false);
  readonly desabilitado = input(false);

  /** Use quando o texto visível não basta para quem não vê a tela (ex.: botão só com ícone). */
  readonly rotuloAcessivel = input<string | undefined>(undefined);

  readonly acionar = output<void>();

  /** Um botão que carrega não aciona, mesmo que a regra da tela o considere habilitado. */
  readonly bloqueado = computed(() => this.desabilitado() || this.carregando());

  protected aoClicar(): void {
    if (!this.bloqueado()) {
      this.acionar.emit();
    }
  }
}
