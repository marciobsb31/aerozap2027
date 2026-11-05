package br.com.aerozap.foundation.web;

import org.springframework.http.HttpStatus;

/**
 * Erro previsto pelo domínio, pronto para virar Problem Details.
 *
 * <p>Existe para separar duas coisas que costumam se misturar: a falha que o serviço espera e sabe
 * explicar (e-mail já cadastrado, credencial inválida, limite atingido) e a falha que ninguém
 * previu. A primeira tem tipo, título e mensagem escritos para quem vai ler; a segunda vira 500 com
 * o código de correlação e nada mais, porque qualquer detalhe a mais ali é vazamento.
 *
 * <p>A mensagem desta exceção **vai para o cliente**. Nunca ponha nela nome de tabela, SQL, caminho
 * de arquivo ou o valor que a pessoa digitou.
 */
public class ProblemaDeNegocio extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String tipo;
    private final String titulo;
    private final HttpStatus status;

    public ProblemaDeNegocio(String tipo, String titulo, HttpStatus status, String detalheParaOCliente) {
        super(detalheParaOCliente);
        this.tipo = tipo;
        this.titulo = titulo;
        this.status = status;
    }

    public String tipo() {
        return tipo;
    }

    public String titulo() {
        return titulo;
    }

    public HttpStatus status() {
        return status;
    }
}
