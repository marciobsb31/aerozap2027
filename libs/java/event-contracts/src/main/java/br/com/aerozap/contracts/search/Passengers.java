package br.com.aerozap.contracts.search;

/**
 * Passageiros da pesquisa. Objeto, não número: o DTC e o manual divergiam nisso e a divergência
 * foi resolvida em favor do objeto (diagnóstico técnico, item 4).
 *
 * @param adults crianças a partir de 12 anos ocupam assento de adulto na maioria dos provedores
 * @param children de 2 a 11 anos, com assento
 * @param infants até 2 anos, no colo: por isso nunca mais que um por adulto
 */
public record Passengers(int adults, int children, int infants) {

    /** Limite das interfaces de pesquisa da maioria dos provedores. */
    public static final int MAXIMO_POR_PESQUISA = 9;

    public Passengers {
        if (adults < 1) {
            throw new IllegalArgumentException("passengers.adults precisa ser pelo menos 1");
        }
        if (children < 0 || infants < 0) {
            throw new IllegalArgumentException("passengers não aceita quantidade negativa");
        }
        if (infants > adults) {
            throw new IllegalArgumentException("passengers.infants não pode passar de adults (bebê no colo)");
        }
        if (adults + children + infants > MAXIMO_POR_PESQUISA) {
            throw new IllegalArgumentException(
                    "passengers soma mais que " + MAXIMO_POR_PESQUISA + " pessoas por pesquisa");
        }
    }

    /** Atalho para a pesquisa mais comum. */
    public static Passengers umAdulto() {
        return new Passengers(1, 0, 0);
    }

    public int total() {
        return adults + children + infants;
    }
}
