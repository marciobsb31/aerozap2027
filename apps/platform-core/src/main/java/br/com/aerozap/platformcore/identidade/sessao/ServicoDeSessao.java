package br.com.aerozap.platformcore.identidade.sessao;

import br.com.aerozap.contracts.ProblemTypes;
import br.com.aerozap.foundation.web.ProblemaDeNegocio;
import br.com.aerozap.platformcore.identidade.seguranca.PropriedadesDeToken;
import br.com.aerozap.platformcore.identidade.seguranca.Segredos;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre, rotaciona e encerra sessões.
 *
 * <p>A regra que importa é a rotação com detecção de reuso. Cada refresh token vale uma vez: ao
 * ser usado, é marcado e um novo é emitido na mesma família. Se um token já usado reaparecer, há
 * duas explicações — ele vazou e alguém está tentando entrar, ou o cliente legítimo repetiu uma
 * requisição. Não dá para distinguir as duas, e por isso a resposta é a mesma e é a conservadora:
 * **derruba a família inteira** e obriga a entrar de novo. Perder uma sessão é incômodo; manter
 * viva uma sessão possivelmente roubada é o problema.
 */
@Service
public class ServicoDeSessao {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeSessao.class);

    private final RepositorioDeSessoes repositorio;
    private final PropriedadesDeToken propriedades;

    public ServicoDeSessao(RepositorioDeSessoes repositorio, PropriedadesDeToken propriedades) {
        this.repositorio = repositorio;
        this.propriedades = propriedades;
    }

    /** Primeiro refresh token de uma família: é o que um login cria. */
    @Transactional
    public TokenDeSessao abrir(UUID usuarioId, String userAgent, String origemIp) {
        return gravar(UUID.randomUUID(), usuarioId, userAgent, origemIp);
    }

    /**
     * Troca um refresh token por outro.
     *
     * @throws ProblemaDeNegocio quando o token não existe, expirou, foi revogado ou já tinha sido
     *                           usado — sempre com a mesma mensagem, para não dizer a quem tenta
     *                           qual das hipóteses aconteceu
     */
    @Transactional
    public Renovacao renovar(String tokenApresentado, String userAgent, String origemIp) {
        Sessao sessao = repositorio.porTokenHash(Segredos.hash(tokenApresentado))
                .orElseThrow(ServicoDeSessao::sessaoInvalida);

        if (sessao.usadoEm() != null || sessao.revogadoEm() != null) {
            // Token já rotacionado reaparecendo: ou vazou, ou é repetição. Nos dois casos, fora.
            int derrubadas = repositorio.revogarFamilia(sessao.familiaId(), "REUSO_DE_TOKEN");
            log.warn("Refresh token reutilizado; família {} revogada ({} sessões).",
                    sessao.familiaId(), derrubadas);
            throw sessaoInvalida();
        }
        if (!sessao.expiraEm().isAfter(Instant.now())) {
            throw sessaoInvalida();
        }
        if (!repositorio.marcarComoUsado(sessao.id())) {
            // Perdeu a corrida para outra requisição com o mesmo token. Mesmo tratamento.
            repositorio.revogarFamilia(sessao.familiaId(), "REUSO_DE_TOKEN");
            throw sessaoInvalida();
        }

        TokenDeSessao novo = gravar(sessao.familiaId(), sessao.usuarioId(), userAgent, origemIp);
        return new Renovacao(sessao.usuarioId(), novo);
    }

    @Transactional
    public void encerrar(String tokenApresentado) {
        repositorio.porTokenHash(Segredos.hash(tokenApresentado))
                .ifPresent(sessao -> repositorio.revogarFamilia(sessao.familiaId(), "SAIDA"));
    }

    @Transactional
    public int encerrarTodasDoUsuario(UUID usuarioId, String motivo) {
        return repositorio.revogarPorUsuario(usuarioId, motivo);
    }

    private TokenDeSessao gravar(UUID familiaId, UUID usuarioId, String userAgent, String origemIp) {
        String token = Segredos.novoToken();
        Instant expiraEm = Instant.now().plus(propriedades.validadeDaSessao());
        repositorio.inserir(new Sessao(UUID.randomUUID(), familiaId, usuarioId,
                Segredos.hash(token), expiraEm, null, null, userAgent, origemIp));
        return new TokenDeSessao(token, expiraEm);
    }

    private static ProblemaDeNegocio sessaoInvalida() {
        return new ProblemaDeNegocio(ProblemTypes.NAO_AUTENTICADO, "Sessão inválida",
                HttpStatus.UNAUTHORIZED, "Entre novamente para continuar.");
    }

    public record TokenDeSessao(String token, Instant expiraEm) {
    }

    public record Renovacao(UUID usuarioId, TokenDeSessao sessao) {
    }
}
