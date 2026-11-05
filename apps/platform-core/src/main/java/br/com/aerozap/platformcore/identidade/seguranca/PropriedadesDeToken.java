package br.com.aerozap.platformcore.identidade.seguranca;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do token emitido. Os valores batem com o que o gateway valida.
 *
 * @param segredo    segredo compartilhado HS256; sem valor padrão de propósito — serviço sem
 *                   segredo configurado não deve subir fingindo que está seguro
 * @param emissor    claim {@code iss}
 * @param publico    claim {@code aud}
 * @param validadeDoAcesso validade do token de acesso: curta, porque ele não pode ser revogado
 * @param validadeDaSessao validade do refresh token
 */
@ConfigurationProperties(prefix = "aerozap.identidade.token")
public record PropriedadesDeToken(
        String segredo,
        String emissor,
        String publico,
        Duration validadeDoAcesso,
        Duration validadeDaSessao) {

    public PropriedadesDeToken {
        validadeDoAcesso = validadeDoAcesso == null ? Duration.ofMinutes(15) : validadeDoAcesso;
        validadeDaSessao = validadeDaSessao == null ? Duration.ofDays(30) : validadeDaSessao;
    }
}
