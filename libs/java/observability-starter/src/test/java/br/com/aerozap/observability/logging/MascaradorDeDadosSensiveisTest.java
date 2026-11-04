package br.com.aerozap.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Mascaramento de dados sensíveis no log")
class MascaradorDeDadosSensiveisTest {

    @ParameterizedTest(name = "{0} → não deve conter {1}")
    @CsvSource(delimiter = '|', value = {
        "contato do cliente joao.silva@exemplo.com.br                 | joao.silva",
        "whatsapp +55 (11) 99999-8888                                 | 99999",
        "whatsapp 5511999998888                                       | 5511999",
        "cpf 123.456.789-00                                           | 123.456.789",
        "cnpj 12.345.678/0001-99                                      | 12.345.678",
        "cartao 4111 1111 1111 1111                                   | 4111 1111",
        "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.abc               | eyJhbGciOiJIUzI1NiJ9",
        "senha=umaSenhaForte123                                       | umaSenhaForte123",
        "api_key: sk-test-0123456789abcdef                            | sk-test-0123456789abcdef",
        "documento 12345678901234567                                  | 1234567890123",
    })
    void removeODadoSensivel(String original, String trechoProibido) {
        assertThat(MascaradorDeDadosSensiveis.mascarar(original)).doesNotContain(trechoProibido.trim());
    }

    @Test
    @DisplayName("mantém os quatro últimos dígitos do telefone, para reconhecer o atendimento")
    void mantemOsQuatroUltimosDigitos() {
        assertThat(MascaradorDeDadosSensiveis.mascarar("whatsapp +55 (11) 99999-8888")).contains("8888");
    }

    @Test
    @DisplayName("mantém o domínio do e-mail, útil no diagnóstico e pouco identificável")
    void mantemODominioDoEmail() {
        assertThat(MascaradorDeDadosSensiveis.mascarar("erro ao enviar para ana@empresa.com"))
                .contains("@empresa.com")
                .doesNotContain("ana@");
    }

    @ParameterizedTest(name = "preserva: {0}")
    @CsvSource(delimiter = '|', value = {
        "pedido processado em 1234567 ms                              | 1234567",
        "porta 15432 e porta 5672                                     | 15432",
        "busca concluida: 37 ofertas                                   | 37 ofertas",
        "data 2026-11-04T09:33:00                                     | 2026-11-04T09:33:00",
        "busca 550e8400-e29b-41d4-a716-446655440000 concluida         | 550e8400-e29b-41d4-a716-446655440000",
    })
    void naoMascaraOQueNaoESensivel(String original, String trechoEsperado) {
        assertThat(MascaradorDeDadosSensiveis.mascarar(original)).contains(trechoEsperado.trim());
    }

    @Test
    @DisplayName("texto nulo ou vazio passa sem erro")
    void toleraNuloEVazio() {
        assertThat(MascaradorDeDadosSensiveis.mascarar(null)).isNull();
        assertThat(MascaradorDeDadosSensiveis.mascarar("")).isEmpty();
    }

    @Test
    @DisplayName("mascararValor só altera textos")
    void mascararValorSoAlteraTextos() {
        assertThat(MascaradorDeDadosSensiveis.mascararValor(42)).isEqualTo(42);
        assertThat(MascaradorDeDadosSensiveis.mascararValor("cpf 123.456.789-00"))
                .isEqualTo("cpf ***.***.***-**");
    }
}
