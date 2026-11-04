package br.com.aerozap.apigateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.aerozap.apigateway.rota.PropriedadesDeRotas;
import br.com.aerozap.apigateway.rota.PropriedadesDeRotas.Rota;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tabela de rotas")
class PropriedadesDeRotasTest {

    private final PropriedadesDeRotas rotas = new PropriedadesDeRotas(List.of(
            new Rota("/api/plataforma", "http://localhost:8081"),
            new Rota("/api/plataforma/admin", "http://localhost:8085"),
            new Rota("/api/busca", "http://localhost:8082")));

    @Test
    @DisplayName("o prefixo mais específico ganha")
    void prefixoMaisEspecificoGanha() {
        assertThat(rotas.paraCaminho("/api/plataforma/admin/v1/usuarios").destino())
                .isEqualTo("http://localhost:8085");
        assertThat(rotas.paraCaminho("/api/plataforma/v1/usuarios").destino())
                .isEqualTo("http://localhost:8081");
    }

    @Test
    @DisplayName("o prefixo exato também casa")
    void prefixoExatoCasa() {
        assertThat(rotas.paraCaminho("/api/busca").destino()).isEqualTo("http://localhost:8082");
    }

    @Test
    @DisplayName("prefixo parcial não casa: /api/buscador não é /api/busca")
    void prefixoParcialNaoCasa() {
        assertThat(rotas.paraCaminho("/api/buscador/x")).isNull();
    }

    @Test
    @DisplayName("sem rota devolve nulo, para o gateway responder 404")
    void semRotaDevolveNulo() {
        assertThat(rotas.paraCaminho("/api/inexistente")).isNull();
    }

    @Test
    @DisplayName("rota mal configurada falha na subida, não em produção")
    void rotaMalConfiguradaFalhaAoCriar() {
        assertThatThrownBy(() -> new Rota("/plataforma", "http://localhost:8081"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("/api/");
        assertThatThrownBy(() -> new Rota("/api/x", "localhost:8081"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http(s)");
        assertThatThrownBy(() -> new Rota("/api/x", "http://localhost:8081/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("terminar com /");
    }
}
