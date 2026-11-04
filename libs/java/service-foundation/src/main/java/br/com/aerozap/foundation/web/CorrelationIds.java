package br.com.aerozap.foundation.web;

import java.util.UUID;
import org.slf4j.MDC;

/** Acesso ao identificador de correlação da requisição corrente. */
public final class CorrelationIds {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private CorrelationIds() {
    }

    /** Correlação corrente; gera uma nova quando não há (ex.: fora de uma requisição). */
    public static String current() {
        String id = MDC.get(MDC_KEY);
        return id != null ? id : UUID.randomUUID().toString();
    }

    /** Aceita só valores curtos e seguros, para impedir injeção em logs; caso contrário gera um novo. */
    static String sanitizeOrGenerate(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > 100
                || !candidate.matches("[A-Za-z0-9._-]+")) {
            return UUID.randomUUID().toString();
        }
        return candidate;
    }
}
