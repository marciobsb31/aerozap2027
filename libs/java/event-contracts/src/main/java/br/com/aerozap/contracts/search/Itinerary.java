package br.com.aerozap.contracts.search;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Um sentido da viagem: ida ou volta. É a sequência de trechos entre a origem e o destino
 * daquele sentido.
 *
 * @param segments trechos em ordem de embarque; pelo menos um
 */
public record Itinerary(List<Segment> segments) {

    public Itinerary {
        Objects.requireNonNull(segments, "segments");
        segments = List.copyOf(segments);
        if (segments.isEmpty()) {
            throw new IllegalArgumentException("itinerary precisa de pelo menos um segmento");
        }
        for (int i = 1; i < segments.size(); i++) {
            Segment anterior = segments.get(i - 1);
            Segment atual = segments.get(i);
            if (!anterior.destination().equals(atual.origin())) {
                throw new IllegalArgumentException("os segmentos não se encadeiam: "
                        + anterior.destination() + " não é a origem de " + atual.origin());
            }
            if (atual.departure().isBefore(anterior.arrival())) {
                throw new IllegalArgumentException("um segmento parte antes de o anterior chegar");
            }
        }
    }

    public String origin() {
        return segments.getFirst().origin();
    }

    public String destination() {
        return segments.getLast().destination();
    }

    /** Escalas: trocas de avião, não quantidade de trechos. */
    public int stops() {
        return segments.size() - 1;
    }

    /** Da partida do primeiro trecho à chegada do último, incluindo as conexões. */
    public Duration totalDuration() {
        return Duration.between(segments.getFirst().departure(), segments.getLast().arrival());
    }
}
