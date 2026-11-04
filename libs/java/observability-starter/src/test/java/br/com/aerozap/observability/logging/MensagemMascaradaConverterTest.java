package br.com.aerozap.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Palavra de conversão %msgSeguro")
class MensagemMascaradaConverterTest {

    private final MensagemMascaradaConverter converter = new MensagemMascaradaConverter();

    @Test
    @DisplayName("devolve a mensagem já mascarada")
    void devolveAMensagemMascarada() {
        ILoggingEvent evento = mock(ILoggingEvent.class);
        when(evento.getFormattedMessage()).thenReturn("enviando para +55 (11) 99999-8888");

        assertThat(converter.convert(evento)).doesNotContain("99999").contains("8888");
    }

    @Test
    @DisplayName("mensagem nula não quebra o log")
    void mensagemNulaNaoQuebra() {
        ILoggingEvent evento = mock(ILoggingEvent.class);
        when(evento.getFormattedMessage()).thenReturn(null);

        assertThat(converter.convert(evento)).isNull();
    }
}
