import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Cartao, I18nService } from 'design-system';

/**
 * Recebe a frase da pessoa e guarda o pedido.
 *
 * A tela diz com todas as letras que a busca nas companhias ainda não acontece. Mostrar resultado
 * inventado aqui seria mais bonito e menos honesto — e o dado falso vazaria para o teste, para a
 * demonstração e para a expectativa de quem vê.
 */
@Component({
  selector: 'app-busca',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Cartao, RouterLink],
  templateUrl: './busca.html',
})
export class Busca {
  /** Vem de `?q=` pela ligação de parâmetros de rota do roteador. */
  readonly q = input('');

  protected readonly i18n = inject(I18nService);
  protected readonly consulta = computed(() => this.q().trim());
}
