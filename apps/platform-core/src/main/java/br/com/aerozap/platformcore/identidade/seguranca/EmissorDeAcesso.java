package br.com.aerozap.platformcore.identidade.seguranca;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Emite o token de acesso que o gateway valida.
 *
 * <p>Fase 1: HS256 com segredo compartilhado, exatamente como a ADR-0003 previu. Quando o serviço
 * publicar JWKS, muda-se a assinatura para RS256 aqui e o decodificador no gateway; as claims
 * continuam as mesmas, então nada mais no caminho precisa mudar.
 *
 * <p>O token carrega o identificador da pessoa em {@code sub} e os papéis em {@code papeis}. Não
 * carrega e-mail, nome nem telefone: o token viaja em cabeçalho, aparece em log de proxy e é
 * legível por quem o tiver — dado pessoal dentro dele é dado pessoal espalhado.
 */
@Component
public class EmissorDeAcesso {

    private final JwtEncoder codificador;
    private final PropriedadesDeToken propriedades;

    public EmissorDeAcesso(PropriedadesDeToken propriedades) {
        this.propriedades = propriedades;
        if (propriedades.segredo() == null || propriedades.segredo().isBlank()) {
            throw new IllegalStateException(
                    "aerozap.identidade.token.segredo não configurado: o serviço não sobe sem ele.");
        }
        SecretKeySpec chave = new SecretKeySpec(
                propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.codificador = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
    }

    /** @return o token assinado e o instante em que expira */
    public Acesso emitir(UUID usuarioId, List<String> papeis) {
        Instant agora = Instant.now();
        Instant expiraEm = agora.plus(propriedades.validadeDoAcesso());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propriedades.emissor())
                .audience(List.of(propriedades.publico()))
                .subject(usuarioId.toString())
                .issuedAt(agora)
                .expiresAt(expiraEm)
                .id(UUID.randomUUID().toString())
                .claim("papeis", papeis)
                .build();

        try {
            String token = codificador.encode(
                    JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                    .getTokenValue();
            return new Acesso(token, expiraEm, propriedades.validadeDoAcesso().toSeconds());
        } catch (JwtEncodingException e) {
            throw new IllegalStateException("Não foi possível emitir o token de acesso.", e);
        }
    }

    public record Acesso(String token, Instant expiraEm, long validadeEmSegundos) {
    }
}
