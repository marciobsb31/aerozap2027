package br.com.aerozap.contracts.events;

import br.com.aerozap.contracts.search.Money;
import java.time.Instant;
import java.util.Objects;

/**
 * Um alerta de preço bateu o alvo do usuário.
 *
 * <p>Leva o preço encontrado e a oferta, não só o aviso: quem notifica precisa dizer quanto e de
 * onde, e a mensagem não pode depender de uma segunda consulta que talvez já mostre outro preço.
 *
 * @param alertId alerta que disparou
 * @param userId dono do alerta
 * @param searchId pesquisa que encontrou a oferta
 * @param offerId oferta que bateu o alvo
 * @param origin origem da rota vigiada, IATA
 * @param destination destino da rota vigiada, IATA
 * @param targetPrice alvo definido pelo usuário
 * @param foundPrice preço encontrado
 * @param detectedAt quando foi detectado
 */
public record PriceTargetReached(
        String alertId,
        String userId,
        String searchId,
        String offerId,
        String origin,
        String destination,
        Money targetPrice,
        Money foundPrice,
        Instant detectedAt) {

    public PriceTargetReached {
        Objects.requireNonNull(alertId, "alertId");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(searchId, "searchId");
        Objects.requireNonNull(offerId, "offerId");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(targetPrice, "targetPrice");
        Objects.requireNonNull(foundPrice, "foundPrice");
        Objects.requireNonNull(detectedAt, "detectedAt");

        if (!targetPrice.currency().equals(foundPrice.currency())) {
            throw new IllegalArgumentException("alvo e preço encontrado precisam estar na mesma moeda");
        }
        if (foundPrice.amount().compareTo(targetPrice.amount()) > 0) {
            throw new IllegalArgumentException("o preço encontrado não bateu o alvo: nada a notificar");
        }
    }
}
