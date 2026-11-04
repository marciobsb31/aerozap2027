package br.com.aerozap.contracts;

/**
 * Catálogo dos tipos de erro da plataforma (RFC 9457, campo {@code type}).
 *
 * <p>O tipo é o que o cliente usa para decidir o que fazer; a mensagem é para a pessoa ler.
 * Por isso o tipo é contrato: só se acrescenta, nunca se renomeia nem se reaproveita. Um tipo
 * novo exige entrada nesta classe e em {@code docs/api-contracts/erros.md}.
 */
public final class ProblemTypes {

    /** Prefixo de todos os tipos. URN porque não precisa resolver na web para ser estável. */
    public static final String PREFIXO = "urn:aerozap:problem:";

    /** Entrada inválida: campo faltando, formato errado, regra de domínio violada. */
    public static final String VALIDACAO = PREFIXO + "validation";
    /** Sem credencial válida. */
    public static final String NAO_AUTENTICADO = PREFIXO + "unauthorized";
    /** Credencial válida, sem permissão para esta operação. */
    public static final String ACESSO_NEGADO = PREFIXO + "forbidden";
    /** Recurso inexistente, ou que não pertence a quem pediu (não revelamos a diferença). */
    public static final String NAO_ENCONTRADO = PREFIXO + "not-found";
    /** Conflito de estado: a operação não cabe na situação atual do recurso. */
    public static final String CONFLITO = PREFIXO + "conflict";
    /** Limite de taxa da borda excedido. Proteção de infraestrutura, não franquia. */
    public static final String LIMITE_DE_TAXA = PREFIXO + "rate-limit";
    /** Franquia de consultas esgotada no período. Diferente de limite de taxa, de propósito. */
    public static final String FRANQUIA_ESGOTADA = PREFIXO + "usage-exhausted";
    /** Nenhum provedor de voos respondeu; a pesquisa não consome franquia. */
    public static final String PROVEDOR_INDISPONIVEL = PREFIXO + "provider-unavailable";
    /** O serviço de destino não respondeu a tempo. */
    public static final String TEMPO_ESGOTADO = PREFIXO + "gateway-timeout";
    /** Falha não prevista. Nunca expõe detalhe interno; o correlationId liga ao log. */
    public static final String ERRO_INTERNO = PREFIXO + "internal-error";

    private ProblemTypes() {
    }
}
