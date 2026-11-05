package br.com.aerozap.platformcore.franquia;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração da franquia.
 *
 * <p>Duas decisões do PRD ainda em aberto moram aqui como configuração, e não como valor fixo no
 * código: o tipo de período (RN-02) e o que conta como pesquisa concluída (RN-12). É o que permite
 * o produto começar sem a resposta e mudar depois sem migração.
 *
 * @param planoPadrao      plano de quem ainda não assinou
 * @param tipoDePeriodo    sobrepõe o tipo do plano quando preenchido; serve a experimento
 * @param fuso             fuso em que a virada acontece
 * @param expiracaoDaReserva prazo para confirmar ou liberar antes de a consulta voltar sozinha
 * @param conclusao        o que conta como pesquisa concluída com sucesso (RN-12)
 */
@ConfigurationProperties(prefix = "aerozap.franquia")
public record PropriedadesDeFranquia(
        String planoPadrao,
        TipoDePeriodo tipoDePeriodo,
        ZoneId fuso,
        Duration expiracaoDaReserva,
        Conclusao conclusao) {

    public PropriedadesDeFranquia {
        planoPadrao = planoPadrao == null ? "FREE" : planoPadrao;
        fuso = fuso == null ? ZoneId.of("America/Sao_Paulo") : fuso;
        // Quinze minutos: tempo de sobra para a pesquisa mais lenta terminar, e curto o bastante
        // para que um worker derrubado não deixe a consulta presa até o fim do trimestre.
        expiracaoDaReserva = expiracaoDaReserva == null ? Duration.ofMinutes(15) : expiracaoDaReserva;
        conclusao = conclusao == null ? Conclusao.APENAS_COM_OFERTAS : conclusao;
    }

    /** RN-12: quando a pesquisa conta como consumida. */
    public enum Conclusao {
        /** Zero ofertas devolve a consulta ao saldo. É o padrão: a pessoa não recebeu nada. */
        APENAS_COM_OFERTAS,
        /** Qualquer pesquisa que termine sem erro consome, inclusive sem resultado. */
        QUALQUER_TERMINO
    }
}
