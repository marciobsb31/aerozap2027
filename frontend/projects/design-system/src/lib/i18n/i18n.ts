import { DOCUMENT, Injectable, InjectionToken, Provider, computed, inject, signal } from '@angular/core';

import { DICIONARIO_ES } from './dicionario.es';
import { ChaveDeTraducao, DICIONARIO_PT_BR, Dicionario } from './dicionario.pt-br';
import { IDIOMA_PADRAO, Idioma, ehIdiomaConhecido, idiomaMaisProximo } from './idioma';

const DICIONARIOS: Readonly<Record<Idioma, Dicionario>> = {
  'pt-BR': DICIONARIO_PT_BR,
  es: DICIONARIO_ES,
};

const CHAVE_NO_ARMAZENAMENTO = 'aerozap.idioma';

/** Idioma inicial imposto pela aplicação (testes, SSR, ou um idioma vindo da conta da pessoa). */
export const IDIOMA_INICIAL = new InjectionToken<Idioma>('AZ_IDIOMA_INICIAL');

export type ParametrosDeTraducao = Readonly<Record<string, string | number>>;

/**
 * Tradução em tempo de execução.
 *
 * O i18n nativo do Angular compila um pacote por idioma e exige recarregar a página para trocar —
 * o que o critério da AZ-016 ("trocar pt-BR/es sem recarregar") proíbe. Então os dois dicionários
 * viajam juntos no pacote e o idioma ativo é um signal: trocá-lo redesenha só o que depende dele,
 * sem recarregar nem perder o que a pessoa já digitou. São dois idiomas de texto curto; quando
 * forem muitos, o caminho é carregar o dicionário sob demanda, mantendo esta mesma interface.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly documento = inject(DOCUMENT, { optional: true });
  private readonly idiomaImposto = inject(IDIOMA_INICIAL, { optional: true });

  private readonly idiomaAtivo = signal<Idioma>(IDIOMA_PADRAO);

  /** Idioma em uso. Leia no template para que a vista acompanhe a troca. */
  readonly idioma = this.idiomaAtivo.asReadonly();

  private readonly dicionario = computed<Dicionario>(() => DICIONARIOS[this.idiomaAtivo()]);

  constructor() {
    this.aplicar(this.idiomaImposto ?? this.descobrirIdioma(), { lembrar: false });
  }

  /**
   * Texto da chave no idioma ativo, com `{nome}` trocado pelos parâmetros.
   *
   * Chave desconhecida devolve a própria chave em vez de texto vazio: na tela isso aparece como
   * defeito evidente, e é melhor um defeito visível do que um espaço em branco silencioso.
   */
  t(chave: ChaveDeTraducao, parametros?: ParametrosDeTraducao): string {
    const texto = this.dicionario()[chave] ?? DICIONARIO_PT_BR[chave] ?? chave;
    return parametros ? interpolar(texto, parametros) : texto;
  }

  /** Troca o idioma e lembra a escolha. Idioma desconhecido é ignorado. */
  definirIdioma(idioma: Idioma): void {
    if (!ehIdiomaConhecido(idioma) || idioma === this.idiomaAtivo()) {
      return;
    }
    this.aplicar(idioma, { lembrar: true });
  }

  private aplicar(idioma: Idioma, opcoes: { lembrar: boolean }): void {
    this.idiomaAtivo.set(idioma);
    // `lang` no <html> não é enfeite: é o que faz o leitor de tela trocar de voz e o navegador
    // acertar a hifenização. Sem isso o espanhol sai lido com fonética portuguesa.
    this.documento?.documentElement?.setAttribute('lang', idioma);
    if (opcoes.lembrar) {
      try {
        this.documento?.defaultView?.localStorage?.setItem(CHAVE_NO_ARMAZENAMENTO, idioma);
      } catch {
        // Janela privada, cookies bloqueados ou cota cheia: a escolha vale para esta sessão.
      }
    }
  }

  private descobrirIdioma(): Idioma {
    const janela = this.documento?.defaultView;
    try {
      const lembrado = janela?.localStorage?.getItem(CHAVE_NO_ARMAZENAMENTO);
      if (ehIdiomaConhecido(lembrado)) {
        return lembrado;
      }
    } catch {
      // Sem armazenamento: cai para o idioma do navegador.
    }
    const doNavegador = janela?.navigator?.languages ?? [];
    return idiomaMaisProximo(doNavegador) ?? IDIOMA_PADRAO;
  }
}

function interpolar(texto: string, parametros: ParametrosDeTraducao): string {
  return texto.replace(/\{(\w+)\}/g, (original, nome: string) => {
    const valor = parametros[nome];
    return valor === undefined ? original : String(valor);
  });
}

/** Fixa o idioma inicial da aplicação (usado em testes e quando a conta já tem preferência). */
export function provideIdiomaInicial(idioma: Idioma): Provider {
  return { provide: IDIOMA_INICIAL, useValue: idioma };
}
