package br.com.aerozap.platformcore.identidade.seguranca;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hash de senha.
 *
 * <p>BCrypt com custo 12. O custo é o ponto: um hash rápido (SHA-256, MD5) transforma o vazamento
 * da tabela em lista de senhas em poucas horas de GPU. Doze é o que mantém a verificação na casa
 * das centenas de milissegundos nesta geração de hardware — caro o bastante para o atacante,
 * barato o bastante para o login.
 *
 * <p>O preferido seria Argon2id, que resiste melhor a ataque com hardware dedicado. Ele depende de
 * uma biblioteca que ainda não está no projeto; quando entrar, a troca é aqui e o formato do hash
 * já diz qual algoritmo gerou cada senha, então a migração é transparente no próximo login.
 */
@Component
public class Senhas {

    private static final int CUSTO = 12;

    /** Tamanho mínimo. Comprimento protege mais que exigência de símbolo, que leva a "Senha1!". */
    public static final int TAMANHO_MINIMO = 10;

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder(CUSTO);

    public String codificar(String senhaEmClaro) {
        if (senhaEmClaro == null || senhaEmClaro.length() < TAMANHO_MINIMO) {
            throw new IllegalArgumentException(
                    "A senha precisa de pelo menos " + TAMANHO_MINIMO + " caracteres.");
        }
        return codificador.encode(senhaEmClaro);
    }

    /**
     * Compara senha com hash. Nunca devolve informação sobre <em>onde</em> falhou, e o BCrypt já
     * compara em tempo constante.
     */
    public boolean confere(String senhaEmClaro, String hash) {
        if (senhaEmClaro == null || hash == null) {
            return false;
        }
        return codificador.matches(senhaEmClaro, hash);
    }
}
