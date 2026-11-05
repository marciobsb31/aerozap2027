import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { I18nService } from 'design-system';

@Component({
  selector: 'app-nao-encontrada',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  templateUrl: './nao-encontrada.html',
})
export class NaoEncontrada {
  protected readonly i18n = inject(I18nService);
}
