package br.com.aerozap.contracts.search;

/**
 * Porta de saída para um provedor de voos (DTC §2.1, item 6).
 *
 * <p>É o que permite trocar, somar ou derrubar um provedor sem tocar na pesquisa. Todo adaptador
 * — Travelpayouts, SerpApi, simulado — implementa exatamente isto.
 *
 * <p>Contrato de comportamento, tão obrigatório quanto a assinatura:
 * <ul>
 *   <li><strong>Não lança por falha do provedor.</strong> Erro, tempo esgotado e recusa voltam
 *       como {@link ProviderResult} com o {@link ProviderStatus} certo. Exceção fica para defeito
 *       de programação.</li>
 *   <li><strong>Respeita o prazo</strong> de {@link SearchContext#timeout()}. A pesquisa inteira
 *       tem orçamento de tempo; um provedor lento não pode gastá-lo sozinho.</li>
 *   <li><strong>Não inventa.</strong> Preço, horário, companhia e link vêm do provedor. Campo que
 *       o provedor não informa fica nulo (AGENTS #7).</li>
 *   <li><strong>Declara simulação.</strong> {@link #demo()} verdadeiro marca toda oferta com
 *       {@code demo = true} (AGENTS #17).</li>
 * </ul>
 */
public interface FlightProvider {

    /** Código estável, usado em log, métrica e no campo {@code provider} da oferta. */
    String code();

    /** Verdadeiro para adaptadores simulados. */
    boolean demo();

    /** Consulta o provedor. Não lança por falha dele: devolve o status no resultado. */
    ProviderResult search(SearchIntent intent, SearchContext context);
}
