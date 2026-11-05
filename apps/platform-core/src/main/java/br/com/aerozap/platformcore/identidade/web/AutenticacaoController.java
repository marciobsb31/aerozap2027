package br.com.aerozap.platformcore.identidade.web;

import br.com.aerozap.contracts.ProblemTypes;
import br.com.aerozap.foundation.web.ApiResponse;
import br.com.aerozap.foundation.web.ProblemaDeNegocio;
import br.com.aerozap.platformcore.identidade.seguranca.EmissorDeAcesso;
import br.com.aerozap.platformcore.identidade.sessao.ServicoDeSessao;
import br.com.aerozap.platformcore.identidade.usuario.RepositorioDeUsuarios;
import br.com.aerozap.platformcore.identidade.usuario.ServicoDeContas;
import br.com.aerozap.platformcore.identidade.usuario.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cadastro, entrada, renovação e saída.
 *
 * <p>O identificador de quem está pedindo vem do token, repassado pelo gateway no cabeçalho
 * {@code X-Usuario-Id} — nunca do corpo nem da query (RF-007). Aceitar o {@code user_id} que o
 * cliente mandar é a forma mais curta de deixar uma pessoa ler os dados de outra.
 */
@RestController
@RequestMapping("/v1/auth")
public class AutenticacaoController {

    private final ServicoDeContas contas;
    private final ServicoDeSessao sessoes;
    private final EmissorDeAcesso emissor;
    private final RepositorioDeUsuarios usuarios;

    public AutenticacaoController(ServicoDeContas contas, ServicoDeSessao sessoes,
                                  EmissorDeAcesso emissor, RepositorioDeUsuarios usuarios) {
        this.contas = contas;
        this.sessoes = sessoes;
        this.emissor = emissor;
        this.usuarios = usuarios;
    }

    @PostMapping("/registrar")
    public ResponseEntity<ApiResponse<Autenticacao.Sessao>> registrar(
            @Valid @RequestBody Autenticacao.PedidoDeCadastro pedido,
            HttpServletRequest requisicao) {

        Usuario usuario = contas.cadastrar(pedido.email(), pedido.senha(), pedido.nome(),
                pedido.idioma(), pedido.versaoDosTermos(), origem(requisicao));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(abrirSessao(usuario, requisicao)));
    }

    @PostMapping("/entrar")
    public ApiResponse<Autenticacao.Sessao> entrar(
            @Valid @RequestBody Autenticacao.PedidoDeEntrada pedido,
            HttpServletRequest requisicao) {

        Usuario usuario = contas.autenticar(pedido.email(), pedido.senha());
        return ApiResponse.of(abrirSessao(usuario, requisicao));
    }

    @PostMapping("/renovar")
    public ApiResponse<Autenticacao.Sessao> renovar(
            @Valid @RequestBody Autenticacao.PedidoDeRenovacao pedido,
            HttpServletRequest requisicao) {

        var renovacao = sessoes.renovar(pedido.refreshToken(),
                requisicao.getHeader("User-Agent"), origem(requisicao));

        Usuario usuario = usuarios.porId(renovacao.usuarioId())
                .orElseThrow(() -> new ProblemaDeNegocio(ProblemTypes.NAO_AUTENTICADO,
                        "Sessão inválida", HttpStatus.UNAUTHORIZED,
                        "Entre novamente para continuar."));

        var acesso = emissor.emitir(usuario.id(), usuario.papeis());
        return ApiResponse.of(new Autenticacao.Sessao(acesso.token(),
                renovacao.sessao().token(), acesso.validadeEmSegundos(),
                renovacao.sessao().expiraEm()));
    }

    @PostMapping("/sair")
    public ResponseEntity<Void> sair(@Valid @RequestBody Autenticacao.PedidoDeRenovacao pedido) {
        sessoes.encerrar(pedido.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/eu")
    public ApiResponse<Autenticacao.Perfil> eu(@RequestHeader("X-Usuario-Id") UUID usuarioId) {
        Usuario usuario = usuarios.porId(usuarioId)
                .orElseThrow(() -> new ProblemaDeNegocio(ProblemTypes.NAO_ENCONTRADO,
                        "Conta não encontrada", HttpStatus.NOT_FOUND,
                        "Não encontramos esta conta."));
        return ApiResponse.of(new Autenticacao.Perfil(usuario.id().toString(), usuario.email(),
                usuario.nome(), usuario.idioma(), usuario.papeis()));
    }

    private Autenticacao.Sessao abrirSessao(Usuario usuario, HttpServletRequest requisicao) {
        var acesso = emissor.emitir(usuario.id(), usuario.papeis());
        var sessao = sessoes.abrir(usuario.id(), requisicao.getHeader("User-Agent"),
                origem(requisicao));
        return new Autenticacao.Sessao(acesso.token(), sessao.token(),
                acesso.validadeEmSegundos(), sessao.expiraEm());
    }

    /** Endereço de origem só para registro de sessão; nunca entra em resposta nem em log. */
    private static String origem(HttpServletRequest requisicao) {
        String encaminhado = requisicao.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return requisicao.getRemoteAddr();
    }
}
