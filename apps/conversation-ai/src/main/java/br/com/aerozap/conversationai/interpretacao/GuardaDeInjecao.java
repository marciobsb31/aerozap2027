package br.com.aerozap.conversationai.interpretacao;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Reconhece mensagens que tentam mandar na plataforma em vez de pedir uma passagem.
 *
 * <p>Isto é a **segunda** linha de defesa, e é importante saber disso. A primeira é estrutural: o
 * interpretador só consegue devolver pesquisa, pergunta ou recusa
 * ({@link ResultadoDaInterpretacao}), então nenhuma frase — por mais bem construída que seja —
 * consegue mudar um plano, ler a conta de outra pessoa ou chamar uma ferramenta. Lista de padrões
 * é jogo de gato e rato; contrato de tipo não é.
 *
 * <p>O que esta classe acrescenta é o <em>reconhecimento</em>: responder com clareza, não gastar
 * consulta e registrar o evento de segurança (RF-046).
 */
public final class GuardaDeInjecao {

    private record Sinal(String nome, Pattern padrao) {
    }

    private static final List<Sinal> SINAIS = List.of(
            new Sinal("ignorar-regras", Pattern.compile(
                    "\\b(?:ignore|ignora|ignorar|esquec[ae]|olvida|desconsidere)\\b.{0,30}"
                            + "\\b(?:regras?|instruc[oa]es?|instrucciones?|prompt|sistema|anterior(?:es)?)\\b")),
            new Sinal("assumir-papel", Pattern.compile(
                    "\\b(?:voce agora e|voce e agora|aja como|finja ser|a partir de agora voce|"
                            + "ahora eres|actua como|haz de cuenta|you are now|act as)\\b")),
            new Sinal("mudar-plano", Pattern.compile(
                    "\\b(?:me torne|me transforme|vire|mude meu plano|libere|desbloqueie|"
                            + "me d[eêao] acesso|quero acesso|"
                            + "hazme|convierteme|cambia mi plan|dame acceso)\\b.{0,20}"
                            + "\\b(?:premium|admin|administrador|vip|ilimitado|gratis)\\b")),
            new Sinal("dados-de-terceiros", Pattern.compile(
                    "\\b(?:pesquisas?|buscas?|dados?|conta|historico|mensagens?|busquedas?|datos?)\\b"
                            + ".{0,25}\\b(?:do usuario|da usuaria|de outro|de outra|do cliente|"
                            + "del usuario|de otro|de otra|de todos)\\b")),
            new Sinal("vazar-configuracao", Pattern.compile(
                    "\\b(?:system prompt|prompt do sistema|suas instrucoes|tus instrucciones|"
                            + "chave de api|api key|token de acesso|variavel de ambiente|senha do banco)\\b")),
            new Sinal("comando-tecnico", Pattern.compile(
                    "\\b(?:drop table|delete from|truncate|select \\*|update .{0,20} set|"
                            + "rm -rf|curl |wget |os\\.system)\\b")));

    private GuardaDeInjecao() {
    }

    /** Nome do sinal detectado, para o log de segurança. Vazio quando a frase é um pedido normal. */
    public static Optional<String> detectar(String textoNormalizado) {
        return SINAIS.stream()
                .filter(s -> s.padrao().matcher(textoNormalizado).find())
                .map(Sinal::nome)
                .findFirst();
    }
}
