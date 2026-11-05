import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { I18nService, PularParaConteudo, SeletorIdioma } from 'design-system';

/**
 * Casca da aplicação: link de pular, cabeçalho, navegação, conteúdo e rodapé.
 *
 * Nenhum texto é escrito aqui — tudo sai do dicionário pelo `i18n.t()`. É o que permite trocar o
 * idioma sem recarregar: o texto é lido de um signal, então a troca redesenha só o que mudou.
 */
@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, PularParaConteudo, SeletorIdioma],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly i18n = inject(I18nService);
  protected readonly ano = new Date().getFullYear();
}
