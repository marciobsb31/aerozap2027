package br.com.aerozap.identity.usuario;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeUsuarios {

    private final JdbcClient jdbc;

    public RepositorioDeUsuarios(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void inserir(Usuario usuario) {
        jdbc.sql("""
                insert into usuario (id, email, email_normalizado, senha_hash, nome, idioma, estado)
                values (:id, :email, :normalizado, :hash, :nome, :idioma, :estado)
                """)
                .param("id", usuario.id())
                .param("email", usuario.email())
                .param("normalizado", Usuario.normalizar(usuario.email()))
                .param("hash", usuario.senhaHash())
                .param("nome", usuario.nome())
                .param("idioma", usuario.idioma())
                .param("estado", usuario.estado().name())
                .update();
    }

    public Optional<Usuario> porEmail(String email) {
        return jdbc.sql(consulta("u.email_normalizado = :email"))
                .param("email", Usuario.normalizar(email))
                .query(RepositorioDeUsuarios::mapear)
                .optional();
    }

    public Optional<Usuario> porId(UUID id) {
        return jdbc.sql(consulta("u.id = :id"))
                .param("id", id)
                .query(RepositorioDeUsuarios::mapear)
                .optional();
    }

    public void concederPapel(UUID usuarioId, String papel) {
        jdbc.sql("insert into papel_do_usuario (usuario_id, papel) values (:u, :p) "
                        + "on conflict do nothing")
                .param("u", usuarioId).param("p", papel).update();
    }

    public void trocarSenha(UUID usuarioId, String novoHash) {
        jdbc.sql("update usuario set senha_hash = :h, atualizado_em = now() where id = :id")
                .param("h", novoHash).param("id", usuarioId).update();
    }

    public void registrarAceite(UUID usuarioId, String documento, String versao, String origemIp) {
        jdbc.sql("""
                insert into aceite_de_termos (id, usuario_id, documento, versao, origem_ip)
                values (:id, :u, :d, :v, :ip)
                """)
                .param("id", UUID.randomUUID())
                .param("u", usuarioId)
                .param("d", documento)
                .param("v", versao)
                .param("ip", origemIp)
                .update();
    }

    /**
     * Os papéis vêm na mesma consulta, agregados. A alternativa — buscar o usuário e depois os
     * papéis — são duas idas ao banco em todo login, no caminho mais quente do serviço.
     */
    private static String consulta(String condicao) {
        return """
            select u.id, u.email, u.senha_hash, u.nome, u.idioma, u.estado, u.criado_em,
                   coalesce(string_agg(p.papel, ',' order by p.papel), '') as papeis
              from usuario u
              left join papel_do_usuario p on p.usuario_id = u.id
             where %s
             group by u.id
            """.formatted(condicao);
    }

    private static Usuario mapear(ResultSet rs, int linha) throws SQLException {
        String papeis = rs.getString("papeis");
        List<String> lista = new ArrayList<>();
        if (papeis != null && !papeis.isBlank()) {
            lista.addAll(List.of(papeis.split(",")));
        }
        return new Usuario(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("senha_hash"),
                rs.getString("nome"),
                rs.getString("idioma"),
                Usuario.Estado.valueOf(rs.getString("estado")),
                List.copyOf(lista),
                rs.getTimestamp("criado_em").toInstant());
    }
}
