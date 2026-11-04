package br.com.aerozap.contracts.search;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Um trecho voado sem troca de avião: a menor unidade de um itinerário.
 *
 * <p>Horários em {@link Instant} de propósito: horário local sem fuso é a origem clássica de erro
 * em voo internacional. Quem exibe converte para o fuso do aeroporto.
 *
 * @param origin aeroporto de partida, IATA
 * @param destination aeroporto de chegada, IATA
 * @param departure partida, instante absoluto
 * @param arrival chegada, instante absoluto
 * @param marketingCarrier companhia que vende o voo, IATA de 2 caracteres
 * @param flightNumber número do voo, sem o código da companhia
 * @param cabin cabine efetiva deste trecho; pode diferir da pedida
 * @param aircraft modelo do equipamento, quando o provedor informa
 */
public record Segment(
        String origin,
        String destination,
        Instant departure,
        Instant arrival,
        String marketingCarrier,
        String flightNumber,
        CabinClass cabin,
        String aircraft) {

    private static final Pattern IATA = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern COMPANHIA = Pattern.compile("^[A-Z0-9]{2}$");

    public Segment {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(departure, "departure");
        Objects.requireNonNull(arrival, "arrival");
        Objects.requireNonNull(marketingCarrier, "marketingCarrier");
        Objects.requireNonNull(flightNumber, "flightNumber");

        if (!IATA.matcher(origin).matches() || !IATA.matcher(destination).matches()) {
            throw new IllegalArgumentException("segment usa códigos IATA de 3 letras em origin e destination");
        }
        if (origin.equals(destination)) {
            throw new IllegalArgumentException("segment não pode partir e chegar no mesmo aeroporto");
        }
        if (!COMPANHIA.matcher(marketingCarrier).matches()) {
            throw new IllegalArgumentException("marketingCarrier precisa ter 2 caracteres IATA: " + marketingCarrier);
        }
        if (flightNumber.isBlank()) {
            throw new IllegalArgumentException("flightNumber não pode ser vazio");
        }
        if (arrival.isBefore(departure)) {
            throw new IllegalArgumentException("arrival não pode ser anterior a departure");
        }
    }
}
