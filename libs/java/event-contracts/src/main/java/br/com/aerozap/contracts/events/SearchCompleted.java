package br.com.aerozap.contracts.events;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A pesquisa terminou. Concluir não quer dizer achar: {@code offerCount} pode ser zero.
 *
 * <p>Quem consome este evento decide o destino da reserva de franquia pela política configurável
 * da ADR-0004 — e é por isso que o evento carrega o que aconteceu com cada provedor, em vez de
 * um "sucesso" binário que esconderia a diferença entre "ninguém respondeu" e "não há voo".
 *
 * @param searchId pesquisa
 * @param userId dono
 * @param offerCount quantas ofertas entraram no resultado final
 * @param providersSucceeded provedores que responderam
 * @param providersFailed provedores que falharam; aparecem na interface como fonte indisponível
 * @param completedAt quando terminou
 */
public record SearchCompleted(
        String searchId,
        String userId,
        int offerCount,
        List<String> providersSucceeded,
        List<String> providersFailed,
        Instant completedAt) {

    public SearchCompleted {
        Objects.requireNonNull(searchId, "searchId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(completedAt, "completedAt");
        providersSucceeded = providersSucceeded == null ? List.of() : List.copyOf(providersSucceeded);
        providersFailed = providersFailed == null ? List.of() : List.copyOf(providersFailed);

        if (offerCount < 0) {
            throw new IllegalArgumentException("offerCount não pode ser negativo");
        }
        if (providersSucceeded.isEmpty() && providersFailed.isEmpty()) {
            throw new IllegalArgumentException("uma pesquisa concluída consultou pelo menos um provedor");
        }
        if (offerCount > 0 && providersSucceeded.isEmpty()) {
            throw new IllegalArgumentException("há ofertas mas nenhum provedor bem-sucedido");
        }
    }

    /** Resultado parcial: houve oferta, mas alguma fonte ficou de fora. */
    public boolean parcial() {
        return !providersFailed.isEmpty() && !providersSucceeded.isEmpty();
    }
}
