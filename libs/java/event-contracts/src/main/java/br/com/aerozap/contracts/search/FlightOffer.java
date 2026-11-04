package br.com.aerozap.contracts.search;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Uma oferta de voo já normalizada, venha de que provedor vier.
 *
 * <p>Regras inegociáveis que este contrato carrega:
 * <ul>
 *   <li><strong>Nada sem fonte.</strong> {@code provider} e {@code checkedAt} são obrigatórios:
 *       preço de voo envelhece em minutos, e exibir sem dizer de onde veio e de quando é
 *       enganar o usuário (RF-024, AGENTS #7).</li>
 *   <li><strong>Demonstração se declara.</strong> {@code demo} sobe até a interface e o texto do
 *       WhatsApp; dado simulado nunca se passa por real (RF-029, AGENTS #17).</li>
 *   <li><strong>Isto não é reserva.</strong> A plataforma pesquisa e encaminha; não emite bilhete,
 *       não garante preço e não usa a palavra "reservado" (HU-15).</li>
 * </ul>
 *
 * @param offerId identificador estável dentro da pesquisa, para comparar e abrir detalhes
 * @param provider código do provedor que devolveu esta oferta
 * @param demo verdadeiro quando veio do adaptador simulado
 * @param totalPrice preço total para todos os passageiros da pesquisa
 * @param itineraries um itinerário em ida simples, dois em ida e volta
 * @param deepLink endereço do provedor para concluir a compra; pode ser nulo
 * @param fareConditions condições e bagagem, no texto do provedor; pode ser nulo
 * @param checkedAt quando o provedor devolveu este preço
 */
public record FlightOffer(
        String offerId,
        String provider,
        boolean demo,
        Money totalPrice,
        List<Itinerary> itineraries,
        String deepLink,
        String fareConditions,
        Instant checkedAt) {

    public FlightOffer {
        Objects.requireNonNull(offerId, "offerId");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(totalPrice, "totalPrice");
        Objects.requireNonNull(checkedAt, "checkedAt");
        Objects.requireNonNull(itineraries, "itineraries");

        if (offerId.isBlank()) {
            throw new IllegalArgumentException("offerId não pode ser vazio");
        }
        if (provider.isBlank()) {
            throw new IllegalArgumentException("provider não pode ser vazio: oferta sem fonte não é exibível");
        }
        itineraries = List.copyOf(itineraries);
        if (itineraries.isEmpty() || itineraries.size() > 2) {
            throw new IllegalArgumentException("uma oferta tem 1 itinerário (ida) ou 2 (ida e volta)");
        }
        if (totalPrice.amount().signum() <= 0) {
            throw new IllegalArgumentException("totalPrice precisa ser maior que zero");
        }
    }

    public TripType tripType() {
        return itineraries.size() == 1 ? TripType.ONE_WAY : TripType.ROUND_TRIP;
    }

    /** Soma das escalas de todos os sentidos. */
    public int totalStops() {
        return itineraries.stream().mapToInt(Itinerary::stops).sum();
    }
}
