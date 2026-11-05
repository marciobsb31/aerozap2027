package br.com.aerozap.contracts.search;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Valor com moeda. Nunca um número solto: preço sem moeda é erro esperando acontecer numa
 * plataforma que pesquisa voo internacional.
 *
 * @param amount valor; {@link BigDecimal} porque dinheiro em ponto flutuante binário erra
 * @param currency código ISO 4217 de três letras
 */
public record Money(BigDecimal amount, String currency) {

    private static final Pattern MOEDA = Pattern.compile("^[A-Z]{3}$");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount não pode ser negativo");
        }
        if (!MOEDA.matcher(currency).matches()) {
            throw new IllegalArgumentException("currency precisa ser um código ISO 4217 de 3 letras: " + currency);
        }
    }

    public static Money de(String amount, String currency) {
        return new Money(new BigDecimal(amount), currency);
    }
}
