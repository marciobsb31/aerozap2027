package br.com.aerozap.platformcore.franquia;

import br.com.aerozap.contracts.usage.ReleaseReason;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Devolve ao saldo as reservas que ficaram em aberto além do prazo.
 *
 * <p>É a rede que pega o caso do worker derrubado no meio da pesquisa: a consulta foi debitada, o
 * processo morreu antes de confirmar ou liberar, e sem isto a consulta ficaria presa até a virada
 * do período. A pessoa não tem como saber disso nem como reclamar com precisão — por isso o
 * conserto é automático, e não um procedimento de suporte.
 *
 * <p>Cada reserva é liberada na sua própria transação: uma linha problemática não impede as
 * outras de voltarem.
 */
@Component
public class ExpiradorDeReservas {

    private static final Logger log = LoggerFactory.getLogger(ExpiradorDeReservas.class);
    private static final int LOTE = 200;

    private final RepositorioDeFranquia repositorio;
    private final ServicoDeFranquia servico;
    private final Clock relogio;

    public ExpiradorDeReservas(RepositorioDeFranquia repositorio, ServicoDeFranquia servico, Clock relogio) {
        this.repositorio = repositorio;
        this.servico = servico;
        this.relogio = relogio;
    }

    @Scheduled(fixedDelayString = "${aerozap.franquia.intervalo-de-expiracao-ms:60000}")
    public void liberarVencidas() {
        var vencidas = repositorio.reservasVencidas(relogio.instant(), LOTE);
        if (vencidas.isEmpty()) {
            return;
        }
        int liberadas = 0;
        for (var reserva : vencidas) {
            try {
                servico.liberar(reserva.id(), ReleaseReason.EXPIRED);
                liberadas++;
            } catch (RuntimeException e) {
                // Uma reserva que não volta não pode travar as outras; o log é o que permite
                // descobrir que existe um caso recorrente.
                log.warn("Não foi possível liberar a reserva {}: {}", reserva.id(), e.getMessage());
            }
        }
        log.info("Reservas expiradas devolvidas ao saldo: {} de {}", liberadas, vencidas.size());
    }
}
