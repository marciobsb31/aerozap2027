package br.com.aerozap.platformcore.identidade;

import br.com.aerozap.platformcore.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.testes.concorrencia.ArenaDeConcorrencia;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * O mesmo refresh token apresentado por várias requisições ao mesmo tempo.
 *
 * <p>Acontece sem má intenção: o aplicativo tem três abas abertas, o token de acesso expira e as
 * três disparam a renovação juntas. Se a rotação não for atômica, as três ganham tokens diferentes
 * da mesma linha e a sessão se multiplica — e aí a detecção de reuso, que é a defesa contra token
 * roubado, deixa de funcionar porque o estado já está inconsistente.
 *
 * <p>A invariante é a mesma da franquia, e pelo mesmo motivo: quem decide é o banco, no
 * {@code UPDATE} condicional sobre {@code usado_em is null}.
 */
class RotacaoConcorrenteIntegrationTest extends AbstractIntegrationTest {

    private static final int CONCORRENTES = 16;

    @Test
    @DisplayName("16 renovações simultâneas com o mesmo token: exatamente uma vence")
    void apenasUmaRenovacaoVence() {
        String refresh = postar("/api/v1/auth/registrar", Map.of(
                "email", "rotacao-" + System.nanoTime() + "@aerozap-teste.invalid",
                "senha", "uma senha longa o bastante",
                "nome", "Pessoa de Teste",
                "idioma", "pt-BR",
                "versaoDosTermos", "2026-11-01")).texto("refreshToken");

        var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES, indice ->
                postar("/api/v1/auth/renovar", Map.of("refreshToken", refresh)).status());

        assertThat(resultado.totalDeFalhas())
                .as("perder a corrida é 401, não exceção. " + resultado.resumo())
                .isZero();
        assertThat(resultado.sucessosQue(status -> status == 200))
                .as("exatamente uma renovação pode vencer. " + resultado.resumo())
                .isEqualTo(1);
        assertThat(resultado.sucessosQue(status -> status == 401))
                .isEqualTo(CONCORRENTES - 1);
    }
}
