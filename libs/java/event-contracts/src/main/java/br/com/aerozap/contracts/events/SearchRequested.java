package br.com.aerozap.contracts.events;

import br.com.aerozap.contracts.search.SearchIntent;
import java.time.Instant;
import java.util.Objects;

/**
 * Uma pesquisa foi criada e já tem a consulta reservada na franquia. Dispara os workers.
 *
 * @param searchId identificador da pesquisa
 * @param userId dono da pesquisa
 * @param channel canal de origem, para onde a resposta volta
 * @param intent o que pesquisar
 * @param requestedAt quando foi pedida
 */
public record SearchRequested(
        String searchId,
        String userId,
        Channel channel,
        SearchIntent intent,
        Instant requestedAt) {

    public SearchRequested {
        Objects.requireNonNull(searchId, "searchId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(requestedAt, "requestedAt");
    }
}
