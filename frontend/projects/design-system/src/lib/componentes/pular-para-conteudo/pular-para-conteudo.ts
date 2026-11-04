import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Primeiro link da página: pula o cabeçalho e a navegação e vai direto ao conteúdo.
 *
 * Fica invisível até receber foco. Para quem navega por teclado ou leitor de tela, é a diferença
 * entre atravessar o menu inteiro em cada página e chegar ao conteúdo com um Tab (WCAG 2.4.1).
 */
@Component({
  selector: 'az-pular-para-conteudo',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="az-pular" [attr.href]="'#' + alvo()">
      <ng-content />
    </a>
  `,
  styles: `
    .az-pular {
      position: absolute;
      left: var(--az-espaco-4);
      top: var(--az-espaco-4);
      z-index: 100;
      padding: var(--az-espaco-3) var(--az-espaco-4);
      border-radius: var(--az-raio-md);
      background: var(--az-cor-acao);
      color: var(--az-cor-acao-texto);
      font-weight: var(--az-peso-forte);
      text-decoration: none;
      box-shadow: var(--az-sombra-2);
      transform: translateY(-200%);
      transition: transform var(--az-transicao);
    }

    .az-pular:focus-visible {
      transform: translateY(0);
    }
  `,
})
export class PularParaConteudo {
  readonly alvo = input('conteudo-principal');
}
