package br.com.aerozap.platformcore.franquia;

import br.com.aerozap.contracts.EventEnvelope;
import br.com.aerozap.contracts.ProblemTypes;
import br.com.aerozap.contracts.events.EventTypes;
import br.com.aerozap.contracts.events.UsageConfirmed;
import br.com.aerozap.contracts.events.UsageReleased;
import br.com.aerozap.contracts.events.UsageReserved;
import br.com.aerozap.contracts.usage.ReleaseReason;
import br.com.aerozap.contracts.usage.ReservationRequest;
import br.com.aerozap.contracts.usage.ReservationResponse;
import br.com.aerozap.contracts.usage.ReservationStatus;
import br.com.aerozap.foundation.outbox.OutboxService;
import br.com.aerozap.foundation.web.CorrelationIds;
import br.com.aerozap.foundation.web.ProblemaDeNegocio;
import br.com.aerozap.platformcore.franquia.RepositorioDeFranquia.Periodo;
import br.com.aerozap.platformcore.franquia.RepositorioDeFranquia.Plano;
import br.com.aerozap.platformcore.franquia.RepositorioDeFranquia.Reserva;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Reserva, confirmação e liberação de consultas da franquia (ADR-0004).
 *
 * <p>A regra de negócio em uma frase: nada é debitado quando a pesquisa termina — é debitado
 * quando ela <em>começa</em>, e devolvido se ela não der certo. O contrário (debitar no fim)
 * permite que N pesquisas simultâneas comecem com saldo 1.
 *
 * <p>Três invariantes, e nenhuma delas mora neste arquivo: todas estão em condição de
 * {@code UPDATE} no banco, porque é o único lugar onde "verificar e gravar" acontece de uma vez só.
 * <ul>
 *   <li>o saldo nunca fica negativo nem passa da franquia;</li>
 *   <li>uma reserva só é fechada uma vez, por quem chegar primeiro;</li>
 *   <li>a mesma chave de idempotência nunca debita duas vezes.</li>
 * </ul>
 */
@Service
public class ServicoDeFranquia {

    private final RepositorioDeFranquia repositorio;
    private final PropriedadesDeFranquia propriedades;
    private final OutboxService outbox;
    private final ObjectMapper json;

    public ServicoDeFranquia(RepositorioDeFranquia repositorio, PropriedadesDeFranquia propriedades,
                             OutboxService outbox, ObjectMapper json) {
        this.repositorio = repositorio;
        this.propriedades = propriedades;
        this.outbox = outbox;
        this.json = json;
    }

    /**
     * Reserva uma consulta.
     *
     * @throws ProblemaDeNegocio 409 {@code usage-exhausted} quando não há saldo. Saldo esgotado é
     *                           erro de propósito: assim quem chama não precisa inspecionar o corpo
     *                           de uma resposta de sucesso para descobrir que não pode pesquisar.
     */
    @Transactional
    public ReservationResponse reservar(ReservationRequest pedido, Instant inicioDaConta) {
        UUID usuario = UUID.fromString(pedido.userId());

        // Repetição da mesma chamada: devolve a reserva que já existe, sem novo débito. Acontece
        // o tempo todo — reentrega de mensagem, retentativa de rede, worker reiniciado.
        var existente = repositorio.reservaPorChave(usuario, pedido.idempotencyKey());
        if (existente.isPresent()) {
            return respostaDe(existente.get());
        }

        Periodo periodo = periodoVigente(usuario, inicioDaConta);

        if (!repositorio.debitar(periodo.id())) {
            throw new ProblemaDeNegocio(ProblemTypes.FRANQUIA_ESGOTADA,
                    "Franquia esgotada", HttpStatus.CONFLICT,
                    "Suas consultas do período acabaram. O saldo volta na próxima renovação.");
        }

        Instant agora = Instant.now();
        Reserva reserva = new Reserva(UUID.randomUUID(), periodo.id(), usuario, pedido.searchId(),
                pedido.idempotencyKey(), ReservationStatus.RESERVED, agora,
                agora.plus(propriedades.expiracaoDaReserva()), null);
        try {
            repositorio.inserirReserva(reserva);
        } catch (DuplicateKeyException e) {
            // Outra requisição criou a reserva para a mesma pesquisa ou a mesma chave no intervalo.
            // O débito desta transação não vale: desfaz e devolve a que ficou de pé.
            throw new ReservaJaExiste(usuario, pedido);
        }

        int restantes = periodo.franquia() - repositorio.consumidas(periodo.id());
        publicar(EventTypes.USAGE_RESERVED, reserva.id().toString(),
                new UsageReserved(reserva.id().toString(), pedido.userId(), pedido.searchId(),
                        periodo.id().toString(), Math.max(0, restantes), agora, reserva.expiraEm()));

        return new ReservationResponse(reserva.id().toString(), ReservationStatus.RESERVED,
                periodo.id().toString(), Math.max(0, restantes), reserva.expiraEm());
    }

    /** Confirma: a consulta foi mesmo usada. O saldo já tinha sido debitado na reserva. */
    @Transactional
    public ReservationResponse confirmar(UUID reservaId) {
        Reserva reserva = exigirReserva(reservaId);
        if (!repositorio.fechar(reservaId, ReservationStatus.CONFIRMED, null)) {
            // Já fechada: pode ser repetição da chamada ou o expirador tendo chegado antes.
            return respostaDe(exigirReserva(reservaId));
        }
        publicar(EventTypes.USAGE_CONFIRMED, reservaId.toString(),
                new UsageConfirmed(reservaId.toString(), reserva.usuarioId().toString(),
                        reserva.pesquisaId(), restantesDe(reserva), Instant.now()));
        return respostaDe(exigirReserva(reservaId));
    }

    /**
     * Libera: a pesquisa falhou, foi cancelada ou não trouxe nada, conforme a política.
     * A consulta volta para o saldo.
     */
    @Transactional
    public ReservationResponse liberar(UUID reservaId, ReleaseReason motivo) {
        Reserva reserva = exigirReserva(reservaId);
        ReservationStatus destino = motivo == ReleaseReason.EXPIRED
                ? ReservationStatus.EXPIRED : ReservationStatus.RELEASED;

        if (!repositorio.fechar(reservaId, destino, motivo)) {
            return respostaDe(exigirReserva(reservaId));
        }
        // O crédito só acontece depois de a reserva ter sido fechada por esta transação. Creditar
        // antes permitiria duas liberações concorrentes devolverem duas consultas de uma reserva.
        repositorio.creditar(reserva.periodoId());

        publicar(EventTypes.USAGE_RELEASED, reservaId.toString(),
                new UsageReleased(reservaId.toString(), reserva.usuarioId().toString(),
                        reserva.pesquisaId(), motivo, restantesDe(reserva), Instant.now()));
        return respostaDe(exigirReserva(reservaId));
    }

    /**
     * Devolve a reserva que já existe para esta chave ou para esta pesquisa.
     *
     * <p>Serve ao caso em que duas requisições idênticas chegam juntas: a que perde a corrida tem
     * a transação desfeita pelo índice único — inclusive o débito — e lê aqui o que a outra
     * gravou. A leitura é de fora da transação que falhou, porque depois de uma violação de
     * restrição o PostgreSQL não aceita mais nada na mesma transação.
     */
    @Transactional(readOnly = true)
    public ReservationResponse reservaJaCriada(UUID usuarioId, String chave, String pesquisaId) {
        return repositorio.reservaPorChave(usuarioId, chave)
                .or(() -> repositorio.reservaPorPesquisa(usuarioId, pesquisaId))
                .map(this::respostaDe)
                .orElseThrow(() -> new ProblemaDeNegocio(ProblemTypes.CONFLITO,
                        "Reserva em conflito", HttpStatus.CONFLICT,
                        "Já existe uma reserva para esta pesquisa."));
    }

    /** Saldo do período vigente, criando o período se for a primeira consulta da janela. */
    @Transactional
    public Saldo saldo(UUID usuarioId, Instant inicioDaConta) {
        Periodo periodo = periodoVigente(usuarioId, inicioDaConta);
        int consumidas = repositorio.consumidas(periodo.id());
        return new Saldo(periodo.id().toString(), periodo.franquia(),
                Math.max(0, periodo.franquia() - consumidas), periodo.inicio(), periodo.fim());
    }

    private Periodo periodoVigente(UUID usuarioId, Instant inicioDaConta) {
        Instant agora = Instant.now();
        var existente = repositorio.periodoVigente(usuarioId, agora);
        if (existente.isPresent()) {
            return existente.get();
        }

        Plano plano = repositorio.plano(propriedades.planoPadrao())
                .orElseThrow(() -> new IllegalStateException(
                        "Plano padrão não cadastrado: " + propriedades.planoPadrao()));
        TipoDePeriodo tipo = propriedades.tipoDePeriodo() != null
                ? propriedades.tipoDePeriodo() : plano.tipoDePeriodo();
        var janela = tipo.janelaDe(agora, inicioDaConta, propriedades.fuso());

        // Duas primeiras consultas simultâneas na virada: quem perde relê o período que a outra
        // criou. Criar um segundo dobraria a franquia do trimestre. O conflito é resolvido no
        // INSERT porque uma violação de restrição abortaria a transação e impediria essa releitura.
        return repositorio.criarPeriodo(usuarioId, plano.codigo(), janela, plano.consultas())
                .or(() -> repositorio.periodoVigente(usuarioId, agora))
                .orElseThrow(() -> new IllegalStateException(
                        "Período em conflito não encontrado para a janela vigente"));
    }

    private Reserva exigirReserva(UUID reservaId) {
        return repositorio.reservaPorId(reservaId)
                .orElseThrow(() -> new ProblemaDeNegocio(ProblemTypes.NAO_ENCONTRADO,
                        "Reserva não encontrada", HttpStatus.NOT_FOUND,
                        "Não encontramos esta reserva."));
    }

    /** Saldo do período DA RESERVA. Usar o período de hoje erraria depois de uma virada. */
    private int restantesDe(Reserva reserva) {
        return repositorio.periodoPorId(reserva.periodoId()).map(Periodo::restantes).orElse(0);
    }

    private ReservationResponse respostaDe(Reserva reserva) {
        int restantes = restantesDe(reserva);
        return new ReservationResponse(reserva.id().toString(), reserva.estado(),
                reserva.periodoId().toString(), restantes,
                reserva.estado() == ReservationStatus.RESERVED ? reserva.expiraEm() : null);
    }

    /** O evento nasce na mesma transação da mudança: ou os dois existem, ou nenhum (Outbox). */
    private void publicar(String tipo, String agregado, Object conteudo) {
        outbox.append(new EventEnvelope(UUID.randomUUID(), tipo, 1, Instant.now(),
                CorrelationIds.current(), agregado, json.writeValueAsString(conteudo)));
    }

    public record Saldo(String periodoId, int franquia, int restantes, Instant inicio, Instant fim) {
    }

    /** Sinaliza que outra requisição ganhou a corrida pela mesma reserva. */
    public static class ReservaJaExiste extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public final transient UUID usuario;
        public final transient ReservationRequest pedido;

        ReservaJaExiste(UUID usuario, ReservationRequest pedido) {
            super("reserva já criada por outra requisição");
            this.usuario = usuario;
            this.pedido = pedido;
        }
    }
}
