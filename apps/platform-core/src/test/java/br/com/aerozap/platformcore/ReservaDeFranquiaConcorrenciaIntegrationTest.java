package br.com.aerozap.platformcore;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aerozap.testes.concorrencia.ArenaDeConcorrencia;
import br.com.aerozap.testes.dados.DadosSinteticos;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * O cenário obrigatório mais caro de errar: duas requisições simultâneas pela última consulta Free.
 *
 * <p><b>O que este teste é.</b> A prova de que o mecanismo escolhido para a franquia — reserva por
 * {@code UPDATE} condicional, decidida pelo banco e não pela aplicação — sustenta N requisições
 * disputando o mesmo saldo. O PostgreSQL é real (Testcontainers), com o mesmo nível de isolamento
 * de produção.
 *
 * <p><b>O que este teste ainda não é.</b> O serviço de franquia é do marco M2 (RF-009 a RF-013);
 * aqui a tabela é criada pelo próprio teste. A invariante e o harness ficam escritos agora, antes
 * da implementação, porque esta é a classe de defeito que não aparece em teste sequencial, não
 * aparece em revisão de código e cobra caro em produção: duas consultas debitadas de um saldo de
 * uma. Quando o serviço existir, este teste passa a chamá-lo em vez do SQL, e a invariante
 * permanece a mesma.
 *
 * <p>O contraexemplo está incluído de propósito: a mesma disputa resolvida com "ler, decidir na
 * aplicação, gravar" passa quando as requisições chegam em fila e falha quando chegam juntas.
 * Sem ele, não há prova de que o {@code UPDATE} condicional é o que está segurando a invariante.
 */
class ReservaDeFranquiaConcorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final int CONCORRENTES = 32;
    private static final int RODADAS = 5;

    @Autowired
    private JdbcClient jdbc;

    private UUID usuario;

    @BeforeEach
    void prepararTabela() {
        jdbc.sql("""
                create table if not exists teste_franquia_de_uso (
                    usuario_id  uuid primary key,
                    restantes   integer not null check (restantes >= 0)
                )
                """).update();
        usuario = DadosSinteticos.padrao().identificador();
    }

    @AfterEach
    void limpar() {
        jdbc.sql("drop table if exists teste_franquia_de_uso").update();
    }

    private void saldo(int restantes) {
        jdbc.sql("insert into teste_franquia_de_uso (usuario_id, restantes) values (:u, :r) "
                        + "on conflict (usuario_id) do update set restantes = :r")
                .param("u", usuario).param("r", restantes).update();
    }

    private int restantes() {
        return jdbc.sql("select restantes from teste_franquia_de_uso where usuario_id = :u")
                .param("u", usuario).query(Integer.class).single();
    }

    /** Reserva aceita pelo banco: o {@code where restantes > 0} é a decisão, e ela é atômica. */
    private boolean reservarComUpdateCondicional() {
        return jdbc.sql("""
                update teste_franquia_de_uso
                   set restantes = restantes - 1
                 where usuario_id = :u
                   and restantes > 0
                """).param("u", usuario).update() == 1;
    }

    /**
     * Como NÃO fazer: a decisão sai do banco e vira uma janela entre a leitura e a gravação.
     *
     * <p>A pausa de 100 ms escancara essa janela. Em produção ela dura microssegundos e a corrida
     * acontece de vez em quando; no teste, uma corrida "de vez em quando" seria um teste instável,
     * e teste instável é defeito. A pausa torna o defeito determinístico sem inventá-lo: a janela
     * existe com ou sem ela.
     */
    private boolean reservarLendoEDepoisGravando() {
        Integer atual = jdbc.sql("select restantes from teste_franquia_de_uso where usuario_id = :u")
                .param("u", usuario).query(Integer.class).single();
        if (atual <= 0) {
            return false;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        jdbc.sql("update teste_franquia_de_uso set restantes = :r where usuario_id = :u")
                .param("r", atual - 1).param("u", usuario).update();
        return true;
    }

    @Test
    @DisplayName("última consulta disputada por 32 requisições simultâneas: exatamente uma é autorizada")
    void apenasUmaAutorizadaNaUltimaConsulta() {
        ArenaDeConcorrencia.repetir(RODADAS, CONCORRENTES,
                () -> saldo(1),
                indice -> reservarComUpdateCondicional(),
                resultado -> {
                    assertThat(resultado.totalDeFalhas())
                            .as("nenhuma requisição pode terminar em erro: negar é resposta, não falha")
                            .isZero();
                    assertThat(resultado.sucessosQue(Boolean.TRUE::equals))
                            .as("exatamente uma autorização para a última consulta")
                            .isEqualTo(1);
                    assertThat(restantes())
                            .as("o saldo nunca pode ficar negativo nem sobrar consulta")
                            .isZero();
                });
    }

    @Test
    @DisplayName("saldo de 5 com 32 pedidos simultâneos: exatamente 5 autorizações, saldo zerado")
    void autorizaExatamenteOSaldoDisponivel() {
        saldo(5);

        var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES,
                indice -> reservarComUpdateCondicional());

        assertThat(resultado.totalDeFalhas()).isZero();
        assertThat(resultado.sucessosQue(Boolean.TRUE::equals)).isEqualTo(5);
        assertThat(resultado.sucessosQue(Boolean.FALSE::equals)).isEqualTo(CONCORRENTES - 5);
        assertThat(restantes()).isZero();
    }

    @Test
    @DisplayName("contraexemplo: decidir na aplicação autoriza mais do que o saldo tinha")
    void lerEDepoisGravarQuebraAInvariante() {
        saldo(1);

        var resultado = ArenaDeConcorrencia.disparar(CONCORRENTES,
                indice -> reservarLendoEDepoisGravando());

        assertThat(resultado.sucessosQue(Boolean.TRUE::equals))
                .as("com a decisão na aplicação, um saldo de 1 autoriza várias consultas")
                .isGreaterThan(1);
        assertThat(restantes())
                .as("e o saldo final deixa de corresponder ao que foi autorizado")
                .isZero();
    }
}
