package br.com.aerozap.contracts.events;

import java.time.Instant;
import java.util.Objects;

/**
 * Chegou uma mensagem em um canal de entrada.
 *
 * <p><strong>Sem dado de pessoa.</strong> Nem telefone, nem texto, nem áudio: o evento carrega
 * referências. O conteúdo fica no serviço de mensageria, com o controle de acesso dele, e
 * {@code contactRef} identifica o contato sem expor o número — evento circula por filas, log e
 * DLQ, e o que não entra ali não vaza (AGENTS, privacidade; LGPD).
 *
 * @param messageId identificador externo da mensagem; é a chave de deduplicação (RF-051)
 * @param channel canal de origem
 * @param contactRef referência interna do contato, nunca o telefone
 * @param userId conta vinculada; nulo quando o contato ainda não tem conta
 * @param kind texto ou áudio
 * @param receivedAt quando a plataforma recebeu
 */
public record MessageReceived(
        String messageId,
        Channel channel,
        String contactRef,
        String userId,
        MessageKind kind,
        Instant receivedAt) {

    public MessageReceived {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(contactRef, "contactRef");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(receivedAt, "receivedAt");
        if (messageId.isBlank()) {
            throw new IllegalArgumentException("messageId não pode ser vazio: é a chave de deduplicação");
        }
        if (contactRef.matches(".*\\d{8,}.*")) {
            throw new IllegalArgumentException(
                    "contactRef parece conter um número de telefone; use a referência interna do contato");
        }
    }

    /** Forma da mensagem recebida. */
    public enum MessageKind {
        TEXT,
        AUDIO
    }
}
