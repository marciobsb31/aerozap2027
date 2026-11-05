package br.com.aerozap.platformcore.identidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.aerozap.platformcore.identidade.seguranca.Senhas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SenhasTest {

    private final Senhas senhas = new Senhas();

    @Test
    @DisplayName("o hash nunca é igual à senha e nunca se repete entre duas senhas iguais")
    void hashComSal() {
        String senha = "uma senha longa o bastante";
        String primeiro = senhas.codificar(senha);
        String segundo = senhas.codificar(senha);

        assertThat(primeiro).isNotEqualTo(senha).startsWith("$2");
        // Sal diferente a cada hash: duas pessoas com a mesma senha não têm o mesmo registro, e
        // tabela arco-íris deixa de servir.
        assertThat(primeiro).isNotEqualTo(segundo);
        assertThat(senhas.confere(senha, primeiro)).isTrue();
        assertThat(senhas.confere(senha, segundo)).isTrue();
    }

    @Test
    @DisplayName("senha errada não confere, e nulo não explode")
    void senhaErrada() {
        String hash = senhas.codificar("uma senha longa o bastante");
        assertThat(senhas.confere("outra senha qualquer", hash)).isFalse();
        assertThat(senhas.confere(null, hash)).isFalse();
        assertThat(senhas.confere("uma senha longa o bastante", null)).isFalse();
    }

    @Test
    @DisplayName("senha curta é recusada na hora de codificar")
    void senhaCurta() {
        assertThatThrownBy(() -> senhas.codificar("curta"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
