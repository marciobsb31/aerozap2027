package br.com.aerozap.testes.dados;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * As garantias que fazem o dado ser sintético de verdade.
 *
 * <p>Sem estes testes, "dados sintéticos" é intenção. Com eles, é propriedade verificada: nenhum
 * e-mail entregável, nenhum telefone roteável, nenhum CPF que possa ser de alguém.
 */
class DadosSinteticosTest {

    @Test
    @DisplayName("e-mails ficam em domínio reservado, que não resolve")
    void emailsNaoEntregaveis() {
        var dados = DadosSinteticos.padrao();
        IntStream.range(0, 200).forEach(i -> assertThat(dados.email()).endsWith(".invalid"));
    }

    @Test
    @DisplayName("telefones usam código de país reservado e não roteável")
    void telefonesNaoRoteaveis() {
        var dados = DadosSinteticos.padrao();
        IntStream.range(0, 200).forEach(i -> assertThat(dados.telefone()).startsWith("+999"));
    }

    @Test
    @DisplayName("o CPF tem o formato certo e o dígito verificador errado, sempre")
    void cpfNuncaValido() {
        var dados = DadosSinteticos.padrao();
        for (int i = 0; i < 500; i++) {
            String cpf = dados.cpfSempreInvalido();
            assertThat(cpf).hasSize(11).containsOnlyDigits();
            assertThat(cpfValido(cpf)).as(cpf).isFalse();
        }
    }

    @Test
    @DisplayName("a mesma semente produz exatamente os mesmos dados")
    void determinismo() {
        var primeira = DadosSinteticos.comSemente(42);
        var segunda = DadosSinteticos.comSemente(42);

        for (int i = 0; i < 50; i++) {
            assertThat(segunda.email()).isEqualTo(primeira.email());
            assertThat(segunda.telefone()).isEqualTo(primeira.telefone());
            assertThat(segunda.identificador()).isEqualTo(primeira.identificador());
        }
    }

    @Test
    @DisplayName("sementes diferentes produzem dados diferentes")
    void sementesDiferentes() {
        assertThat(DadosSinteticos.comSemente(1).email())
                .isNotEqualTo(DadosSinteticos.comSemente(2).email());
    }

    @Test
    @DisplayName("a rota nunca tem origem igual ao destino")
    void rotaSempreValida() {
        var dados = DadosSinteticos.padrao();
        for (int i = 0; i < 300; i++) {
            String[] rota = dados.rota();
            assertThat(rota[0]).isNotEqualTo(rota[1]);
            assertThat(rota[0]).hasSize(3);
        }
    }

    @Test
    @DisplayName("a data de ida é sempre futura")
    void dataDeIdaFutura() {
        var dados = DadosSinteticos.padrao();
        var hoje = LocalDate.of(2026, 11, 4);
        for (int i = 0; i < 200; i++) {
            assertThat(dados.dataDeIda(hoje)).isAfter(hoje);
        }
    }

    /** Validação de CPF de verdade, só para provar que o gerador nunca produz um válido. */
    private static boolean cpfValido(String cpf) {
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        int[] digitos = cpf.chars().map(c -> c - '0').toArray();
        return digitos[9] == verificador(digitos, 9, 10) && digitos[10] == verificador(digitos, 10, 11);
    }

    private static int verificador(int[] digitos, int ate, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int i = 0; i < ate; i++) {
            soma += digitos[i] * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
