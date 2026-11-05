package br.com.aerozap.contracts.search;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * O que a pessoa quer pesquisar, já estruturado e validado.
 *
 * <p>É o contrato central da plataforma: o formulário da Web, o interpretador de linguagem
 * natural (AZ-019) e o WhatsApp produzem a mesma coisa, e a pesquisa só conhece esta forma.
 * É isso que permite trocar a maneira de pedir sem mexer em quem executa.
 *
 * <p><strong>O que esta classe valida</strong>: forma e coerência interna — código de aeroporto,
 * ordem das datas, quantidade de passageiros, moeda. <strong>O que ela não valida</strong>: se o
 * aeroporto existe, se a data é futura ou se a rota é atendida. Isso depende de dados e de
 * relógio, vive no serviço de pesquisa (RF-026), e deixar fora daqui é o que permite reconstruir
 * uma pesquisa antiga a partir do histórico sem ela estourar.
 *
 * @param origin aeroporto ou cidade de origem, código IATA de três letras
 * @param destination destino, código IATA de três letras
 * @param departureDate data de ida
 * @param returnDate data de volta; nulo em ida simples
 * @param passengers quem viaja
 * @param cabin classe desejada; nulo significa {@link CabinClass#ECONOMY}
 * @param currency moeda dos preços, ISO 4217; nulo significa BRL
 * @param maxStops máximo de escalas aceito; nulo significa sem limite
 * @param preferredAirlines códigos IATA de companhias preferidas; vazio significa todas
 * @param maxPrice teto de preço; nulo significa sem teto
 * @param locale idioma da resposta: {@code pt-BR} ou {@code es}
 */
public record SearchIntent(
        String origin,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate,
        Passengers passengers,
        CabinClass cabin,
        String currency,
        Integer maxStops,
        List<String> preferredAirlines,
        BigDecimal maxPrice,
        String locale) {

    private static final Pattern IATA = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern MOEDA = Pattern.compile("^[A-Z]{3}$");
    private static final List<String> IDIOMAS = List.of("pt-BR", "es");

    public SearchIntent {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(departureDate, "departureDate");
        Objects.requireNonNull(passengers, "passengers");

        if (!IATA.matcher(origin).matches()) {
            throw new IllegalArgumentException("origin precisa ser um código IATA de 3 letras maiúsculas: " + origin);
        }
        if (!IATA.matcher(destination).matches()) {
            throw new IllegalArgumentException(
                    "destination precisa ser um código IATA de 3 letras maiúsculas: " + destination);
        }
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("origin e destination não podem ser iguais");
        }
        if (returnDate != null && returnDate.isBefore(departureDate)) {
            throw new IllegalArgumentException("returnDate não pode ser anterior a departureDate");
        }
        if (maxStops != null && maxStops < 0) {
            throw new IllegalArgumentException("maxStops não pode ser negativo");
        }
        if (maxPrice != null && maxPrice.signum() <= 0) {
            throw new IllegalArgumentException("maxPrice precisa ser maior que zero");
        }

        cabin = cabin != null ? cabin : CabinClass.ECONOMY;
        currency = currency != null ? currency : "BRL";
        if (!MOEDA.matcher(currency).matches()) {
            throw new IllegalArgumentException("currency precisa ser um código ISO 4217 de 3 letras: " + currency);
        }
        locale = locale != null ? locale : "pt-BR";
        if (!IDIOMAS.contains(locale)) {
            throw new IllegalArgumentException("locale precisa ser pt-BR ou es: " + locale);
        }

        preferredAirlines = preferredAirlines == null ? List.of() : List.copyOf(preferredAirlines);
        for (String companhia : preferredAirlines) {
            if (companhia == null || !companhia.matches("^[A-Z0-9]{2}$")) {
                throw new IllegalArgumentException(
                        "preferredAirlines aceita códigos IATA de companhia com 2 caracteres: " + companhia);
            }
        }
    }

    /** Pesquisa mínima: ida simples, um adulto, econômica, em reais. */
    public static SearchIntent ida(String origin, String destination, LocalDate departureDate) {
        return new SearchIntent(origin, destination, departureDate, null, Passengers.umAdulto(),
                null, null, null, null, null, null);
    }

    public TripType tripType() {
        return returnDate == null ? TripType.ONE_WAY : TripType.ROUND_TRIP;
    }
}
