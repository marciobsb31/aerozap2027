package br.com.aerozap.testes.dados;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Gera dados de teste que não podem pertencer a ninguém.
 *
 * <p>A regra do projeto é "dados de teste sintéticos, nunca dados reais de usuários". Gerar nomes e
 * telefones ao acaso não cumpre essa regra: um telefone plausível gerado ao acaso é o telefone de
 * alguém, e um dia um teste liga para ele, manda mensagem para ele ou vaza num log. Por isso cada
 * tipo aqui usa uma faixa que o mundo reserva para exemplo e teste:
 *
 * <ul>
 *   <li>e-mail em {@code .invalid} e domínios em {@code .example} — reservados pela RFC 6761, não
 *       resolvem e não podem ser registrados;</li>
 *   <li>telefone com código de país {@code +999} — reservado pela ITU-T E.164 e não roteável;</li>
 *   <li>CPF com dígitos verificadores propositalmente errados — nunca corresponde a um documento
 *       válido, e ainda assim exercita máscara, tamanho e validação;</li>
 *   <li>nomes de uma lista fictícia fixa, sem sobrenome de pessoa real.</li>
 * </ul>
 *
 * <p>Tudo é determinístico a partir da semente: o mesmo teste produz os mesmos dados em toda
 * execução, porque teste que muda de entrada a cada rodada falha de forma irreproduzível, e falha
 * irreproduzível é tratada como ruído até o dia em que não era.
 */
public final class DadosSinteticos {

    private static final List<String> NOMES = List.of(
            "Ana Teste", "Bruno Exemplo", "Carla Fictícia", "Davi Amostra", "Elisa Simulada",
            "Fábio Protótipo", "Gabi Rascunho", "Heitor Modelo", "Iara Hipótese", "João Suposto");

    /** Aeroportos reais: código público de infraestrutura, não dado de pessoa. */
    private static final List<String> AEROPORTOS = List.of(
            "GRU", "CGH", "GIG", "BSB", "CWB", "POA", "REC", "SSA", "FOR", "MAO",
            "LIS", "MAD", "EZE", "SCL", "BOG", "MIA", "JFK", "CDG", "FCO", "LHR");

    private final Random sorteio;

    private DadosSinteticos(long semente) {
        this.sorteio = new Random(semente);
    }

    /** Mesma semente, mesmos dados: é o que torna a falha reproduzível. */
    public static DadosSinteticos comSemente(long semente) {
        return new DadosSinteticos(semente);
    }

    /** Semente padrão, para quem só precisa de dados estáveis e não se importa com quais. */
    public static DadosSinteticos padrao() {
        return new DadosSinteticos(20_260_418L);
    }

    public String nome() {
        return NOMES.get(sorteio.nextInt(NOMES.size()));
    }

    /** `.invalid` nunca resolve: um e-mail daqui não chega a lugar nenhum nem por acidente. */
    public String email() {
        return "usuario-" + sorteio.nextInt(100_000) + "@aerozap-teste.invalid";
    }

    /** Código de país +999: reservado pela ITU, nenhuma operadora o encaminha. */
    public String telefone() {
        return "+999" + (1_000_000_000L + (long) sorteio.nextInt(900_000_000));
    }

    /**
     * CPF com formato certo e dígito verificador errado de propósito.
     *
     * <p>Serve para exercitar máscara, tamanho e a rejeição pela validação. Nenhum teste deve
     * esperar que ele seja aceito — se algum dia for, a validação de CPF está quebrada, e esse é
     * justamente um defeito que vale descobrir.
     */
    public String cpfSempreInvalido() {
        StringBuilder base = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            base.append(sorteio.nextInt(10));
        }
        int[] digitos = base.chars().map(c -> c - '0').toArray();
        int primeiro = digitoVerificador(digitos, 10);
        int[] comPrimeiro = new int[10];
        System.arraycopy(digitos, 0, comPrimeiro, 0, 9);
        comPrimeiro[9] = primeiro;
        int segundo = digitoVerificador(comPrimeiro, 11);
        // Estraga o segundo dígito: continua com 11 caracteres e nunca passa na validação.
        return base.append(primeiro).append((segundo + 1) % 10).toString();
    }

    public String aeroporto() {
        return AEROPORTOS.get(sorteio.nextInt(AEROPORTOS.size()));
    }

    /** Par origem/destino sempre diferente entre si, como o contrato SearchIntent exige. */
    public String[] rota() {
        String origem = aeroporto();
        String destino = aeroporto();
        while (destino.equals(origem)) {
            destino = aeroporto();
        }
        return new String[] {origem, destino};
    }

    /** Data de ida futura em relação à data informada, entre 7 e 180 dias. */
    public LocalDate dataDeIda(LocalDate hoje) {
        return hoje.plusDays(7L + sorteio.nextInt(174));
    }

    public UUID identificador() {
        return new UUID(sorteio.nextLong(), sorteio.nextLong());
    }

    /** Chave de idempotência: o mesmo pedido repetido carrega a mesma chave. */
    public String chaveDeIdempotencia() {
        return "idem-" + identificador();
    }

    private static int digitoVerificador(int[] digitos, int pesoInicial) {
        int soma = 0;
        int peso = pesoInicial;
        for (int digito : digitos) {
            soma += digito * peso--;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
