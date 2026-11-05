package br.com.aerozap.identity.usuario;

import br.com.aerozap.contracts.ProblemTypes;
import br.com.aerozap.foundation.web.ProblemaDeNegocio;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Limita tentativas de entrada por identificador e por origem (RF-002).
 *
 * <p>O limite de taxa do gateway protege a infraestrutura e conta por endereço de origem. Este
 * aqui protege **a conta**: alguém testando mil senhas de um e-mail a partir de mil endereços passa
 * reto pelo primeiro e esbarra neste.
 *
 * <p>A contagem é feita por {@code UPDATE} condicional, como a franquia: a decisão é do banco. Ler
 * o contador, decidir na aplicação e gravar deixaria uma janela por onde passam tentativas
 * simultâneas — e força bruta é, por definição, simultânea.
 */
@Component
public class ControleDeTentativas {

    static final int LIMITE = 10;
    static final Duration JANELA = Duration.ofMinutes(15);
    static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private final JdbcClient jdbc;

    public ControleDeTentativas(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Recusa de imediato quem já está bloqueado, sem chegar a comparar senha. */
    public void exigirQueNaoEstejaBloqueado(String chave) {
        Boolean bloqueado = jdbc.sql("""
                select bloqueado_ate is not null and bloqueado_ate > now()
                  from tentativa_de_entrada where chave = :chave
                """).param("chave", chave).query(Boolean.class).optional().orElse(false);

        if (Boolean.TRUE.equals(bloqueado)) {
            throw new ProblemaDeNegocio(ProblemTypes.LIMITE_DE_TAXA,
                    "Muitas tentativas", HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde alguns minutos antes de tentar novamente.");
        }
    }

    /** Conta a falha e bloqueia ao atingir o limite dentro da janela. */
    public void registrarFalha(String chave) {
        jdbc.sql("""
                insert into tentativa_de_entrada (chave, tentativas, janela_abre)
                values (:chave, 1, now())
                on conflict (chave) do update set
                    tentativas = case
                        when tentativa_de_entrada.janela_abre < now() - :janela then 1
                        else tentativa_de_entrada.tentativas + 1 end,
                    janela_abre = case
                        when tentativa_de_entrada.janela_abre < now() - :janela then now()
                        else tentativa_de_entrada.janela_abre end,
                    bloqueado_ate = case
                        when tentativa_de_entrada.tentativas + 1 >= :limite then now() + :bloqueio
                        else tentativa_de_entrada.bloqueado_ate end
                """)
                .param("chave", chave)
                .param("janela", JANELA)
                .param("limite", LIMITE)
                .param("bloqueio", BLOQUEIO)
                .update();
    }

    /** Entrada bem-sucedida zera o contador: o limite pune tentativa errada, não uso normal. */
    public void registrarSucesso(String chave) {
        jdbc.sql("delete from tentativa_de_entrada where chave = :chave")
                .param("chave", chave).update();
    }
}
