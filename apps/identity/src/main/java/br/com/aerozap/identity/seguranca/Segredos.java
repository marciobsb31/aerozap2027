package br.com.aerozap.identity.seguranca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Geração e guarda de segredos opacos — refresh token e token de recuperação.
 *
 * <p>Dois cuidados que parecem detalhe e não são. O primeiro: o valor sorteado tem 256 bits de
 * entropia de uma fonte criptográfica, porque um token adivinhável é uma conta aberta. O segundo:
 * no banco fica o SHA-256 do token, nunca o token. Vazamento da tabela não dá sessão a ninguém, e
 * nem a equipe consegue se passar por um usuário olhando o banco.
 *
 * <p>Aqui o hash é rápido de propósito, ao contrário da senha: o segredo já é aleatório e longo, e
 * não há ataque de dicionário a conter — o que se quer é só não guardar o original.
 */
public final class Segredos {

    private static final SecureRandom SORTEIO = new SecureRandom();
    private static final Base64.Encoder BASE64 = Base64.getUrlEncoder().withoutPadding();

    private Segredos() {
    }

    /** Valor opaco para entregar ao cliente. Só ele vê; o banco guarda o hash. */
    public static String novoToken() {
        byte[] bytes = new byte[32];
        SORTEIO.nextBytes(bytes);
        return BASE64.encodeToString(bytes);
    }

    /** Código curto para verificação por canal (SMS, WhatsApp): seis dígitos, sorteio seguro. */
    public static String novoCodigoNumerico() {
        return String.format("%06d", SORTEIO.nextInt(1_000_000));
    }

    public static String hash(String valor) {
        try {
            byte[] digerido = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digerido) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16))
                   .append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", e);
        }
    }
}
