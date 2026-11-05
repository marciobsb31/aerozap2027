package br.com.aerozap.apigateway.seguranca;

import br.com.aerozap.contracts.ProblemTypes;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

/**
 * Autenticação de entrada: só passa quem apresenta um token válido.
 *
 * <p>O gateway **valida** o token; não emite, não renova e não consulta banco. Emissão fica no
 * serviço de identidade (AZ-020). Erros saem em Problem Details, iguais ao resto da plataforma.
 */
@Configuration
public class ConfiguracaoDeSeguranca {

    /** Caminhos abertos: só o que precisa responder antes de haver token. */
    static final String[] CAMINHOS_PUBLICOS = {
        "/actuator/health", "/actuator/health/**", "/actuator/info",
        "/v3/api-docs", "/v3/api-docs/**",
    };

    /** Quem ainda não tem token, ou está com ele vencido, precisa destes para conseguir um. */
    static final String[] ENTRADAS_SEM_TOKEN = {
        "/api/plataforma/v1/auth/registrar", "/api/plataforma/v1/auth/entrar",
        "/api/plataforma/v1/auth/renovar", "/api/plataforma/v1/auth/sair",
    };

    @Bean
    SecurityFilterChain cadeiaDeFiltros(HttpSecurity http, ObjectMapper json) throws Exception {
        return http
                // API sem sessão e sem cookie de autenticação: o token vai no cabeçalho.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .authorizeHttpRequests(a -> a
                        .requestMatchers(CAMINHOS_PUBLICOS).permitAll()
                        .requestMatchers(HttpMethod.POST, ENTRADAS_SEM_TOKEN).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((req, resp, ex) -> ProblemaHttp.escrever(resp, json,
                                HttpStatus.UNAUTHORIZED, ProblemTypes.NAO_AUTENTICADO, "Não autenticado",
                                "Apresente um token de acesso válido no cabeçalho Authorization."))
                        .accessDeniedHandler((req, resp, ex) -> ProblemaHttp.escrever(resp, json,
                                HttpStatus.FORBIDDEN, ProblemTypes.ACESSO_NEGADO, "Acesso negado",
                                "O token é válido, mas não permite esta operação.")))
                .build();
    }

    /**
     * Decodificador do token. Fase 1: segredo compartilhado (HS256). Quando o serviço de
     * identidade existir, troca-se por JWKS sem mexer no resto do gateway.
     */
    @Bean
    JwtDecoder decodificadorDeToken(PropriedadesDeJwt propriedades) {
        SecretKeySpec chave = new SecretKeySpec(
                propriedades.segredo().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(chave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        List<OAuth2TokenValidator<Jwt>> validadores = new ArrayList<>();
        validadores.add(new JwtTimestampValidator());
        validadores.add(new JwtIssuerValidator(propriedades.emissor()));
        if (StringUtils.hasText(propriedades.publico())) {
            validadores.add(new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                    audiencia -> audiencia != null && audiencia.contains(propriedades.publico())));
        }
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validadores));
        return decodificador;
    }
}
