package br.com.aerozap.conversationai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Serviço de conversa: interpretação de linguagem natural, contexto e transcrição (ADR-0002).
 *
 * <p>Na AZ-019 o serviço existe para hospedar o interpretador e suas métricas; os endpoints e o
 * contexto de conversa chegam no marco M4.
 */
@SpringBootApplication
public class ConversationAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConversationAiApplication.class, args);
    }
}
