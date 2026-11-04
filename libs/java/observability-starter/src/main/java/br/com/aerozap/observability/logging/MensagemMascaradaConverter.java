package br.com.aerozap.observability.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Palavra de conversão {@code %msgSeguro}: a mensagem do log já mascarada.
 *
 * <p>Use-a em lugar de {@code %msg} em qualquer padrão de saída (console, arquivo, Loki) para
 * garantir que nenhum dado sensível chegue ao destino.
 */
public class MensagemMascaradaConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent evento) {
        return MascaradorDeDadosSensiveis.mascarar(evento.getFormattedMessage());
    }
}
