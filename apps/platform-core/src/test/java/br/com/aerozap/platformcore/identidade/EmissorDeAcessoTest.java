package br.com.aerozap.platformcore.identidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.aerozap.platformcore.identidade.seguranca.EmissorDeAcesso;
import br.com.aerozap.platformcore.identidade.seguranca.PropriedadesDeToken;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * O token emitido aqui precisa ser aceito lá no gateway. Por isso o teste **decodifica** com o
 * mesmo tipo de decodificador que o gateway usa, em vez de conferir o texto do token: é a
 * interoperabilidade entre os dois serviços que está sendo provada, não o formato.
 */
class EmissorDeAcessoTest {

    private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-32-bytes!!";

    private final PropriedadesDeToken propriedades = new PropriedadesDeToken(
            SEGREDO, "https://aerozap.local/identidade", "aerozap-api",
            Duration.ofMinutes(15), Duration.ofDays(30));

    @Test
    @DisplayName("o token sai assinado e com as claims que o gateway valida")
    void tokenValidoNoGateway() {
        UUID usuario = UUID.randomUUID();
        var acesso = new EmissorDeAcesso(propriedades).emitir(usuario, List.of("USUARIO"));

        Jwt token = decodificador().decode(acesso.token());

        assertThat(token.getSubject()).isEqualTo(usuario.toString());
        assertThat(token.getIssuer().toString()).isEqualTo("https://aerozap.local/identidade");
        assertThat(token.getAudience()).containsExactly("aerozap-api");
        assertThat(token.getClaimAsStringList("papeis")).containsExactly("USUARIO");
        assertThat(token.getExpiresAt()).isAfter(token.getIssuedAt());
        assertThat(acesso.validadeEmSegundos()).isEqualTo(900);
    }

    @Test
    @DisplayName("o token não carrega dado pessoal")
    void semDadoPessoal() {
        var acesso = new EmissorDeAcesso(propriedades).emitir(UUID.randomUUID(), List.of("USUARIO"));
        Jwt token = decodificador().decode(acesso.token());

        // Token viaja em cabeçalho, aparece em log de proxy e é legível por quem o tiver.
        assertThat(token.getClaims()).doesNotContainKeys("email", "nome", "telefone", "cpf");
    }

    @Test
    @DisplayName("cada token tem identificador próprio, para poder ser rastreado e revogado")
    void jtiUnico() {
        var emissor = new EmissorDeAcesso(propriedades);
        UUID usuario = UUID.randomUUID();
        String primeiro = decodificador().decode(emissor.emitir(usuario, List.of()).token()).getId();
        String segundo = decodificador().decode(emissor.emitir(usuario, List.of()).token()).getId();
        assertThat(primeiro).isNotEqualTo(segundo);
    }

    @Test
    @DisplayName("sem segredo configurado, o serviço não sobe")
    void semSegredoNaoSobe() {
        var semSegredo = new PropriedadesDeToken("", "iss", "aud", null, null);
        assertThatThrownBy(() -> new EmissorDeAcesso(semSegredo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("segredo");
    }

    private NimbusJwtDecoder decodificador() {
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(SEGREDO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
