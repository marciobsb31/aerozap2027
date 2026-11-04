package br.com.aerozap.apigateway.seguranca;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Validação do token de entrada.
 *
 * <p>Na Fase 1 o token é assinado com segredo compartilhado (HS256), porque o serviço de
 * identidade só chega na AZ-020. Quando ele existir, a troca é de configuração: em lugar do
 * segredo, o endereço do conjunto de chaves públicas (JWKS).
 *
 * @param segredo segredo de assinatura; mínimo de 32 caracteres, exigência do HS256
 * @param emissor valor esperado em {@code iss}
 * @param publico valor esperado em {@code aud}; vazio não verifica
 */
@Validated
@ConfigurationProperties("aerozap.seguranca.jwt")
public record PropriedadesDeJwt(@NotBlank String segredo, @NotBlank String emissor, String publico) {

    public PropriedadesDeJwt {
        if (segredo != null && segredo.length() < 32) {
            throw new IllegalArgumentException(
                    "aerozap.seguranca.jwt.segredo precisa de pelo menos 32 caracteres (256 bits) para HS256");
        }
    }
}
