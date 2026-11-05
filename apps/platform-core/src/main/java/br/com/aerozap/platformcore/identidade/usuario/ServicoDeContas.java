package br.com.aerozap.platformcore.identidade.usuario;

import br.com.aerozap.contracts.ProblemTypes;
import br.com.aerozap.foundation.web.ProblemaDeNegocio;
import br.com.aerozap.platformcore.identidade.seguranca.Senhas;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cadastro e verificação de credenciais. */
@Service
public class ServicoDeContas {

    /** Papel de toda conta nova. Papel administrativo é concedido, nunca escolhido no cadastro. */
    public static final String PAPEL_PADRAO = "USUARIO";

    private final RepositorioDeUsuarios repositorio;
    private final Senhas senhas;
    private final ControleDeTentativas tentativas;

    public ServicoDeContas(RepositorioDeUsuarios repositorio, Senhas senhas,
                           ControleDeTentativas tentativas) {
        this.repositorio = repositorio;
        this.senhas = senhas;
        this.tentativas = tentativas;
    }

    @Transactional
    public Usuario cadastrar(String email, String senhaEmClaro, String nome, String idioma,
                             String versaoDosTermos, String origemIp) {
        String hash;
        try {
            hash = senhas.codificar(senhaEmClaro);
        } catch (IllegalArgumentException e) {
            throw new ProblemaDeNegocio(ProblemTypes.VALIDACAO, "Senha fraca",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "A senha precisa de pelo menos " + Senhas.TAMANHO_MINIMO + " caracteres.");
        }

        Usuario novo = new Usuario(UUID.randomUUID(), email.trim(), hash,
                nome == null ? null : nome.trim(),
                idioma == null ? "pt-BR" : idioma,
                Usuario.Estado.ATIVO, List.of(PAPEL_PADRAO), null);

        try {
            repositorio.inserir(novo);
        } catch (DuplicateKeyException e) {
            // Quem decide é o índice único, não uma consulta anterior: entre consultar e gravar
            // cabe outro cadastro com o mesmo e-mail.
            throw new ProblemaDeNegocio(ProblemTypes.CONFLITO, "E-mail já cadastrado",
                    HttpStatus.CONFLICT,
                    "Já existe uma conta com este e-mail. Tente entrar ou recuperar o acesso.");
        }
        repositorio.concederPapel(novo.id(), PAPEL_PADRAO);
        repositorio.registrarAceite(novo.id(), "TERMOS_DE_USO", versaoDosTermos, origemIp);
        repositorio.registrarAceite(novo.id(), "POLITICA_DE_PRIVACIDADE", versaoDosTermos, origemIp);
        return novo;
    }

    /**
     * Verifica credenciais.
     *
     * <p>E-mail inexistente, senha errada e conta bloqueada devolvem **a mesma** resposta. Dizer
     * "este e-mail não existe" entrega ao atacante a lista de quem tem conta — e a diferença de
     * tempo entre os dois caminhos entrega o mesmo, por isso a senha é verificada contra um hash
     * descartável quando a conta não existe.
     */
    public Usuario autenticar(String email, String senhaEmClaro) {
        String chave = "email:" + email.trim().toLowerCase(Locale.ROOT);
        tentativas.exigirQueNaoEstejaBloqueado(chave);

        Usuario usuario = repositorio.porEmail(email).orElse(null);
        boolean confere = usuario != null
                ? senhas.confere(senhaEmClaro, usuario.senhaHash())
                : senhas.confere(senhaEmClaro, HASH_DESCARTAVEL);

        if (usuario == null || !confere || !usuario.podeEntrar()) {
            tentativas.registrarFalha(chave);
            throw new ProblemaDeNegocio(ProblemTypes.NAO_AUTENTICADO, "Credenciais inválidas",
                    HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }
        tentativas.registrarSucesso(chave);
        return usuario;
    }

    /** Hash de uma senha qualquer, só para gastar o mesmo tempo quando a conta não existe. */
    private static final String HASH_DESCARTAVEL =
            "$2a$12$C6UzMDM.H6dfI/f/IKcEe.e6g0ZDkA3UdY1mPeV5m5Qc0OdqK3Yqu";
}
