package br.com.aerozap.platformcore.identidade.sessao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeSessoes {

    private final JdbcClient jdbc;

    public RepositorioDeSessoes(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void inserir(Sessao sessao) {
        jdbc.sql("""
                insert into identidade.sessao (id, familia_id, usuario_id, token_hash, expira_em, user_agent, origem_ip)
                values (:id, :familia, :usuario, :hash, :expira, :agente, :ip)
                """)
                .param("id", sessao.id())
                .param("familia", sessao.familiaId())
                .param("usuario", sessao.usuarioId())
                .param("hash", sessao.tokenHash())
                .param("expira", OffsetDateTime.ofInstant(sessao.expiraEm(), ZoneOffset.UTC))
                .param("agente", sessao.userAgent())
                .param("ip", sessao.origemIp())
                .update();
    }

    public Optional<Sessao> porTokenHash(String hash) {
        return jdbc.sql("""
                select id, familia_id, usuario_id, token_hash, expira_em, usado_em, revogado_em
                  from identidade.sessao where token_hash = :hash
                """)
                .param("hash", hash)
                .query(RepositorioDeSessoes::mapear)
                .optional();
    }

    /**
     * Marca o refresh token como usado — mas só se ainda não tiver sido.
     *
     * <p>A condição {@code usado_em is null} é a rotação inteira: duas requisições com o mesmo
     * token chegando juntas, só uma consegue a linha. A outra recebe zero e é tratada como reuso,
     * que é exatamente o que é.
     *
     * @return verdadeiro se esta chamada foi a que consumiu o token
     */
    public boolean marcarComoUsado(UUID sessaoId) {
        return jdbc.sql("""
                update identidade.sessao set usado_em = now()
                 where id = :id and usado_em is null and revogado_em is null
                """).param("id", sessaoId).update() == 1;
    }

    /** Derruba a família inteira. Usado quando um token já rotacionado reaparece. */
    public int revogarFamilia(UUID familiaId, String motivo) {
        return jdbc.sql("""
                update identidade.sessao set revogado_em = now(), motivo_revogacao = :motivo
                 where familia_id = :familia and revogado_em is null
                """).param("familia", familiaId).param("motivo", motivo).update();
    }

    public int revogarPorUsuario(UUID usuarioId, String motivo) {
        return jdbc.sql("""
                update identidade.sessao set revogado_em = now(), motivo_revogacao = :motivo
                 where usuario_id = :usuario and revogado_em is null
                """).param("usuario", usuarioId).param("motivo", motivo).update();
    }

    private static Sessao mapear(ResultSet rs, int linha) throws SQLException {
        return new Sessao(
                rs.getObject("id", UUID.class),
                rs.getObject("familia_id", UUID.class),
                rs.getObject("usuario_id", UUID.class),
                rs.getString("token_hash"),
                instante(rs, "expira_em"),
                instante(rs, "usado_em"),
                instante(rs, "revogado_em"),
                null,
                null);
    }

    private static Instant instante(ResultSet rs, String coluna) throws SQLException {
        var valor = rs.getTimestamp(coluna);
        return valor == null ? null : valor.toInstant();
    }
}
