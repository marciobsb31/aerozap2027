package br.com.aerozap.platformcore.franquia;

import br.com.aerozap.contracts.usage.ReleaseReason;
import br.com.aerozap.contracts.usage.ReservationStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Acesso ao schema da franquia.
 *
 * <p>Toda decisão que dependa de concorrência está escrita como {@code UPDATE} condicional, e os
 * métodos devolvem quantas linhas mudaram. Quem chama decide o que fazer com zero — mas nunca
 * precisa ler antes para decidir, que é o padrão que produz débito duplicado.
 */
@Repository
public class RepositorioDeFranquia {

    private final JdbcClient jdbc;

    public RepositorioDeFranquia(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- plano

    public Optional<Plano> plano(String codigo) {
        return jdbc.sql("""
                select codigo, nome, consultas, tipo_periodo, ativo
                  from franquia.plano where codigo = :codigo and ativo
                """)
                .param("codigo", codigo)
                .query((rs, linha) -> new Plano(rs.getString("codigo"), rs.getString("nome"),
                        rs.getInt("consultas"), TipoDePeriodo.valueOf(rs.getString("tipo_periodo"))))
                .optional();
    }

    // ---------------------------------------------------------------- período

    public Optional<Periodo> periodoVigente(UUID usuarioId, Instant momento) {
        return jdbc.sql("""
                select id, usuario_id, plano_codigo, inicio, fim, franquia, consumidas
                  from franquia.periodo_de_uso
                 where usuario_id = :usuario and inicio <= :momento and fim > :momento
                """)
                .param("usuario", usuarioId)
                .param("momento", OffsetDateTime.ofInstant(momento, ZoneOffset.UTC))
                .query(RepositorioDeFranquia::mapearPeriodo)
                .optional();
    }

    public Optional<Periodo> periodoPorId(UUID id) {
        return jdbc.sql("""
                select id, usuario_id, plano_codigo, inicio, fim, franquia, consumidas
                  from franquia.periodo_de_uso where id = :id
                """)
                .param("id", id)
                .query(RepositorioDeFranquia::mapearPeriodo)
                .optional();
    }

    /**
     * Cria o período da janela. Pode falhar por chave duplicada quando duas requisições do mesmo
     * usuário chegam juntas na virada — e é assim que tem de ser: quem perde relê o período que a
     * outra criou, em vez de criar um segundo e dobrar a franquia.
     */
    public Periodo criarPeriodo(UUID usuarioId, String plano, TipoDePeriodo.Janela janela, int franquia) {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                insert into franquia.periodo_de_uso (id, usuario_id, plano_codigo, inicio, fim, franquia)
                values (:id, :usuario, :plano, :inicio, :fim, :franquia)
                """)
                .param("id", id)
                .param("usuario", usuarioId)
                .param("plano", plano)
                .param("inicio", OffsetDateTime.ofInstant(janela.inicio(), ZoneOffset.UTC))
                .param("fim", OffsetDateTime.ofInstant(janela.fim(), ZoneOffset.UTC))
                .param("franquia", franquia)
                .update();
        return new Periodo(id, usuarioId, plano, janela.inicio(), janela.fim(), franquia, 0);
    }

    /**
     * Debita uma consulta, se houver.
     *
     * <p>Esta linha é a invariante inteira do módulo. A condição {@code consumidas < franquia} é
     * avaliada pelo banco dentro da mesma instrução que grava, então duas requisições pela última
     * consulta não podem passar as duas.
     *
     * @return verdadeiro se o débito aconteceu
     */
    public boolean debitar(UUID periodoId) {
        return jdbc.sql("""
                update franquia.periodo_de_uso
                   set consumidas = consumidas + 1
                 where id = :id and consumidas < franquia
                """).param("id", periodoId).update() == 1;
    }

    /** Devolve uma consulta ao saldo. Nunca abaixo de zero, pela condição e pela constraint. */
    public boolean creditar(UUID periodoId) {
        return jdbc.sql("""
                update franquia.periodo_de_uso
                   set consumidas = consumidas - 1
                 where id = :id and consumidas > 0
                """).param("id", periodoId).update() == 1;
    }

    public int consumidas(UUID periodoId) {
        return jdbc.sql("select consumidas from franquia.periodo_de_uso where id = :id")
                .param("id", periodoId).query(Integer.class).single();
    }

    // ---------------------------------------------------------------- reserva

    public void inserirReserva(Reserva reserva) {
        jdbc.sql("""
                insert into franquia.reserva_de_uso
                  (id, periodo_id, usuario_id, pesquisa_id, chave_idempotencia, estado, expira_em)
                values (:id, :periodo, :usuario, :pesquisa, :chave, :estado, :expira)
                """)
                .param("id", reserva.id())
                .param("periodo", reserva.periodoId())
                .param("usuario", reserva.usuarioId())
                .param("pesquisa", reserva.pesquisaId())
                .param("chave", reserva.chaveDeIdempotencia())
                .param("estado", reserva.estado().name())
                .param("expira", OffsetDateTime.ofInstant(reserva.expiraEm(), ZoneOffset.UTC))
                .update();
    }

    public Optional<Reserva> reservaPorChave(UUID usuarioId, String chave) {
        return buscarReserva("usuario_id = :usuario and chave_idempotencia = :valor", usuarioId, chave);
    }

    public Optional<Reserva> reservaPorPesquisa(UUID usuarioId, String pesquisaId) {
        return buscarReserva("usuario_id = :usuario and pesquisa_id = :valor", usuarioId, pesquisaId);
    }

    public Optional<Reserva> reservaPorId(UUID id) {
        return jdbc.sql(SELECT_RESERVA + " where id = :id")
                .param("id", id)
                .query(RepositorioDeFranquia::mapearReserva)
                .optional();
    }

    /**
     * Fecha a reserva, se ela ainda estiver aberta.
     *
     * <p>A condição {@code estado = 'RESERVED'} é o que impede confirmar duas vezes, liberar duas
     * vezes, ou o expirador liberar uma reserva que acabou de ser confirmada.
     *
     * @return verdadeiro se esta chamada foi a que fechou
     */
    public boolean fechar(UUID reservaId, ReservationStatus novoEstado, ReleaseReason motivo) {
        return jdbc.sql("""
                update franquia.reserva_de_uso
                   set estado = :estado, resolvida_em = now(), motivo_liberacao = :motivo
                 where id = :id and estado = 'RESERVED'
                """)
                .param("estado", novoEstado.name())
                .param("motivo", motivo == null ? null : motivo.name())
                .param("id", reservaId)
                .update() == 1;
    }

    /** Reservas em aberto cujo prazo passou. O índice parcial mantém esta varredura barata. */
    public List<Reserva> reservasVencidas(Instant momento, int limite) {
        return jdbc.sql(SELECT_RESERVA
                + " where estado = 'RESERVED' and expira_em < :momento order by expira_em limit :limite")
                .param("momento", OffsetDateTime.ofInstant(momento, ZoneOffset.UTC))
                .param("limite", limite)
                .query(RepositorioDeFranquia::mapearReserva)
                .list();
    }

    private Optional<Reserva> buscarReserva(String condicao, UUID usuarioId, String valor) {
        return jdbc.sql(SELECT_RESERVA + " where " + condicao)
                .param("usuario", usuarioId)
                .param("valor", valor)
                .query(RepositorioDeFranquia::mapearReserva)
                .optional();
    }

    private static final String SELECT_RESERVA = """
            select id, periodo_id, usuario_id, pesquisa_id, chave_idempotencia, estado,
                   criada_em, expira_em, resolvida_em
              from franquia.reserva_de_uso
            """;

    private static Periodo mapearPeriodo(ResultSet rs, int linha) throws SQLException {
        return new Periodo(
                rs.getObject("id", UUID.class),
                rs.getObject("usuario_id", UUID.class),
                rs.getString("plano_codigo"),
                rs.getTimestamp("inicio").toInstant(),
                rs.getTimestamp("fim").toInstant(),
                rs.getInt("franquia"),
                rs.getInt("consumidas"));
    }

    private static Reserva mapearReserva(ResultSet rs, int linha) throws SQLException {
        var resolvida = rs.getTimestamp("resolvida_em");
        return new Reserva(
                rs.getObject("id", UUID.class),
                rs.getObject("periodo_id", UUID.class),
                rs.getObject("usuario_id", UUID.class),
                rs.getString("pesquisa_id"),
                rs.getString("chave_idempotencia"),
                ReservationStatus.valueOf(rs.getString("estado")),
                rs.getTimestamp("criada_em").toInstant(),
                rs.getTimestamp("expira_em").toInstant(),
                resolvida == null ? null : resolvida.toInstant());
    }

    public record Plano(String codigo, String nome, int consultas, TipoDePeriodo tipoDePeriodo) {
    }

    public record Periodo(UUID id, UUID usuarioId, String plano, Instant inicio, Instant fim,
                          int franquia, int consumidas) {

        public int restantes() {
            return Math.max(0, franquia - consumidas);
        }
    }

    public record Reserva(UUID id, UUID periodoId, UUID usuarioId, String pesquisaId,
                          String chaveDeIdempotencia, ReservationStatus estado, Instant criadaEm,
                          Instant expiraEm, Instant resolvidaEm) {
    }
}
