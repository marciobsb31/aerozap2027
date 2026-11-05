import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AerozapApi, ProblemaDaApi } from 'api-client';
import { Botao, CampoTexto, Cartao, Emblema, I18nService } from 'design-system';

type EstadoDaPlataforma =
  | { situacao: 'verificando' }
  | { situacao: 'disponivel'; correlacao: string }
  | { situacao: 'indisponivel'; correlacao: string | null };

/**
 * Tela inicial: a frase da pessoa e o estado da plataforma.
 *
 * O cartão de estado chama o `ping` de verdade pelo cliente gerado do contrato. É deliberado: é a
 * única forma de a tela provar, ao abrir, que o caminho pessoa → gateway → serviço está de pé.
 */
@Component({
  selector: 'app-inicio',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Botao, CampoTexto, Cartao, Emblema],
  templateUrl: './inicio.html',
  styleUrl: './inicio.scss',
})
export class Inicio {
  protected readonly i18n = inject(I18nService);
  private readonly api = inject(AerozapApi);
  private readonly roteador = inject(Router);

  protected readonly consulta = signal('');
  protected readonly erro = signal<string | undefined>(undefined);
  protected readonly enviando = signal(false);
  protected readonly estado = signal<EstadoDaPlataforma>({ situacao: 'verificando' });

  constructor() {
    this.verificarPlataforma();
  }

  protected verificarPlataforma(): void {
    this.estado.set({ situacao: 'verificando' });
    this.api.ping().subscribe({
      next: (resposta) =>
        this.estado.set({ situacao: 'disponivel', correlacao: resposta.correlacao }),
      error: (problema: ProblemaDaApi) =>
        this.estado.set({ situacao: 'indisponivel', correlacao: problema.correlacao }),
    });
  }

  protected enviar(evento: Event): void {
    evento.preventDefault();
    const texto = this.consulta().trim();
    if (texto.length === 0) {
      // A mensagem é lida pelo leitor de tela (role="alert" no campo) e o foco fica onde está:
      // mover o foco depois de um erro de validação faz a pessoa perder o lugar.
      this.erro.set(this.i18n.t('inicio.busca.vazio'));
      return;
    }
    this.erro.set(undefined);
    this.enviando.set(true);
    void this.roteador.navigate(['/busca'], { queryParams: { q: texto } });
  }
}
