package br.com.aerozap.conversationai.interpretacao;

import br.com.aerozap.contracts.search.SearchIntent;
import java.util.Objects;

/**
 * O que o interpretador pode devolver — e só isto.
 *
 * <p>Esta é a defesa estrutural contra injeção de prompt (RF-046). Não é uma lista de frases
 * proibidas: é o tipo de retorno. O interpretador não tem como mandar mudar um plano, consultar
 * outra conta ou executar uma ferramenta, porque não existe forma de expressar isso aqui. Uma
 * mensagem que peça essas coisas só pode virar pesquisa, pergunta ou recusa.
 *
 * <p>É também por isso que a IA nunca decide sozinha: ela produz uma intenção, e quem valida e
 * executa é o backend (DTC §2.1, regra 5).
 */
public sealed interface ResultadoDaInterpretacao {

    /**
     * Entendeu o suficiente para pesquisar.
     *
     * @param intencao a pesquisa, já no contrato compartilhado
     * @param confianca de 0 a 1; abaixo do limiar o canal confirma com a pessoa antes (RF-044)
     */
    record Pesquisa(SearchIntent intencao, double confianca) implements ResultadoDaInterpretacao {
        public Pesquisa {
            Objects.requireNonNull(intencao, "intencao");
            if (confianca < 0 || confianca > 1) {
                throw new IllegalArgumentException("confianca fica entre 0 e 1: " + confianca);
            }
        }
    }

    /**
     * Falta um dado essencial. Uma pergunta por vez, nunca um formulário (RF-041).
     *
     * @param campo o que falta
     * @param pergunta texto pronto para o canal, no idioma da pessoa
     * @param parcial o que já foi entendido, para não perguntar de novo o que ela já disse
     */
    record Pergunta(CampoFaltante campo, String pergunta, IntencaoParcial parcial)
            implements ResultadoDaInterpretacao {
        public Pergunta {
            Objects.requireNonNull(campo, "campo");
            Objects.requireNonNull(pergunta, "pergunta");
            Objects.requireNonNull(parcial, "parcial");
            if (pergunta.isBlank()) {
                throw new IllegalArgumentException("pergunta vazia");
            }
        }
    }

    /** Não vai adiante. Nenhuma pesquisa é criada e nada é consumido da franquia. */
    record Recusa(MotivoDaRecusa motivo, String resposta) implements ResultadoDaInterpretacao {
        public Recusa {
            Objects.requireNonNull(motivo, "motivo");
            Objects.requireNonNull(resposta, "resposta");
        }
    }
}
