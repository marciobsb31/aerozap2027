package br.com.aerozap.apigateway.rota;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tempos limite do encaminhamento. Sem limite explícito, uma dependência lenta prende as threads
 * da borda e derruba tudo junto.
 *
 * @param conexao tempo para abrir a conexão com o serviço
 * @param leitura tempo para o serviço responder
 */
@ConfigurationProperties("aerozap.gateway.tempos-limite")
public record TemposLimite(@DefaultValue("2s") Duration conexao, @DefaultValue("15s") Duration leitura) {
}
