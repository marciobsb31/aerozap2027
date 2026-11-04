import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';

let sequencia = 0;

/**
 * Campo de texto com rótulo, dica e erro já ligados por `id`.
 *
 * A ligação é o ponto do componente: `for`/`id` entre rótulo e campo, e `aria-describedby`
 * apontando para dica e erro. Feito à mão em cada tela, isso é o primeiro detalhe a se perder — e
 * sem ele o leitor de tela anuncia "campo de edição" sem dizer de quê.
 */
@Component({
  selector: 'az-campo-texto',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="az-campo">
      <label class="az-campo__rotulo" [attr.for]="idDoCampo()">
        {{ rotulo() }}
        @if (obrigatorio()) {
          <span class="az-campo__obrigatorio" aria-hidden="true">*</span>
          <span class="az-visualmente-oculto">({{ textoObrigatorio() }})</span>
        }
      </label>

      <input
        class="az-campo__entrada"
        [class.az-campo__entrada--invalida]="!!erro()"
        [attr.id]="idDoCampo()"
        [attr.type]="tipo()"
        [attr.placeholder]="exemplo()"
        [attr.autocomplete]="preenchimentoAutomatico()"
        [attr.aria-describedby]="descricoes()"
        [attr.aria-invalid]="!!erro()"
        [attr.aria-required]="obrigatorio()"
        [disabled]="desabilitado()"
        [value]="valor()"
        (input)="aoDigitar($event)"
      />

      @if (dica() && !erro()) {
        <p class="az-campo__dica" [attr.id]="idDaDica()">{{ dica() }}</p>
      }
      @if (erro()) {
        <p class="az-campo__erro" [attr.id]="idDoErro()" role="alert">{{ erro() }}</p>
      }
    </div>
  `,
  styles: `
    .az-campo {
      display: flex;
      flex-direction: column;
      gap: var(--az-espaco-2);
    }

    .az-campo__rotulo {
      font-size: var(--az-tamanho-sm);
      font-weight: var(--az-peso-medio);
      color: var(--az-cor-texto);
    }

    .az-campo__obrigatorio {
      color: var(--az-cor-erro);
    }

    .az-campo__entrada {
      min-height: var(--az-alvo-toque);
      padding: 0 var(--az-espaco-4);
      border: 1px solid var(--az-cor-borda-forte);
      border-radius: var(--az-raio-md);
      background: var(--az-cor-superficie);
      color: var(--az-cor-texto);
      font: inherit;
      transition: border-color var(--az-transicao);
    }

    .az-campo__entrada::placeholder {
      color: var(--az-cor-texto-sutil);
    }

    .az-campo__entrada:hover:not(:disabled) {
      border-color: var(--az-cor-marca);
    }

    .az-campo__entrada:disabled {
      background: var(--az-cor-superficie-sutil);
      cursor: not-allowed;
    }

    .az-campo__entrada--invalida {
      border-color: var(--az-cor-erro);
    }

    .az-campo__dica,
    .az-campo__erro {
      margin: 0;
      font-size: var(--az-tamanho-sm);
    }

    .az-campo__dica {
      color: var(--az-cor-texto-sutil);
    }

    .az-campo__erro {
      color: var(--az-cor-erro);
      font-weight: var(--az-peso-medio);
    }
  `,
})
export class CampoTexto {
  readonly rotulo = input.required<string>();
  readonly valor = model('');
  readonly dica = input<string | undefined>(undefined);
  readonly erro = input<string | undefined>(undefined);
  readonly exemplo = input<string | undefined>(undefined);
  readonly tipo = input<'text' | 'email' | 'search' | 'tel'>('text');
  readonly obrigatorio = input(false);
  readonly desabilitado = input(false);
  readonly preenchimentoAutomatico = input<string | undefined>(undefined);

  /** Traduzido pela aplicação: o design system não decide o idioma de ninguém. */
  readonly textoObrigatorio = input('obrigatório');

  private readonly sufixo = `az-campo-${++sequencia}`;

  protected readonly idDoCampo = computed(() => this.sufixo);
  protected readonly idDaDica = computed(() => `${this.sufixo}-dica`);
  protected readonly idDoErro = computed(() => `${this.sufixo}-erro`);

  protected readonly descricoes = computed(() => {
    const partes: string[] = [];
    if (this.dica() && !this.erro()) {
      partes.push(this.idDaDica());
    }
    if (this.erro()) {
      partes.push(this.idDoErro());
    }
    return partes.length > 0 ? partes.join(' ') : null;
  });

  protected aoDigitar(evento: Event): void {
    this.valor.set((evento.target as HTMLInputElement).value);
  }
}
