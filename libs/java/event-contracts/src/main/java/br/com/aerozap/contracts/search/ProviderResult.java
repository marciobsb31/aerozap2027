package br.com.aerozap.contracts.search;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Resposta de um provedor a uma pesquisa.
 *
 * <p>Erro de provedor não é exceção: é resultado. Modelar assim obriga quem chama a decidir o que
 * fazer com a falha — mostrar o que os outros devolveram e dizer qual fonte falhou (RF-027) — em
 * vez de deixar a pesquisa inteira cair.
 *
 * @param provider código do provedor
 * @param status como terminou
 * @param offers ofertas devolvidas; vazio quando não houve
 * @param message detalhe para diagnóstico; nunca mostrado ao usuário final
 * @param checkedAt quando a resposta chegou
 */
public record ProviderResult(
        String provider,
        ProviderStatus status,
        List<FlightOffer> offers,
        String message,
        Instant checkedAt) {

    public ProviderResult {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(checkedAt, "checkedAt");
        offers = offers == null ? List.of() : List.copyOf(offers);

        if (status == ProviderStatus.OK && offers.isEmpty()) {
            throw new IllegalArgumentException("status OK exige pelo menos uma oferta; sem ofertas use EMPTY");
        }
        if ((status == ProviderStatus.EMPTY || status == ProviderStatus.ERROR
                || status == ProviderStatus.TIMEOUT) && !offers.isEmpty()) {
            throw new IllegalArgumentException("status " + status + " não pode vir com ofertas");
        }
        for (FlightOffer oferta : offers) {
            if (!provider.equals(oferta.provider())) {
                throw new IllegalArgumentException("oferta de outro provedor no resultado de " + provider);
            }
        }
    }

    public static ProviderResult vazio(String provider, Instant checkedAt) {
        return new ProviderResult(provider, ProviderStatus.EMPTY, List.of(), null, checkedAt);
    }

    public static ProviderResult erro(String provider, ProviderStatus status, String message, Instant checkedAt) {
        return new ProviderResult(provider, status, List.of(), message, checkedAt);
    }

    public boolean temOfertas() {
        return !offers.isEmpty();
    }
}
