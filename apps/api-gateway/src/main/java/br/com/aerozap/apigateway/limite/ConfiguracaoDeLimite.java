package br.com.aerozap.apigateway.limite;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/** Monta o limitador e o coloca na frente da cadeia de segurança. */
@Configuration
public class ConfiguracaoDeLimite {

    /**
     * Ordem anterior à da cadeia de segurança (que fica em -100), para o limite valer também
     * para requisições sem token válido.
     */
    static final int ORDEM_DO_FILTRO = -110;

    @Bean
    RedisScript<String> scriptDeLimiteDeTaxa() {
        return RedisScript.of(new ClassPathResource("limite-de-taxa.lua"), String.class);
    }

    @Bean
    LimitadorDeTaxa limitadorDeTaxa(StringRedisTemplate redis, RedisScript<String> scriptDeLimiteDeTaxa,
                                    PropriedadesDeLimite propriedades) {
        return new LimitadorDeTaxa(redis, scriptDeLimiteDeTaxa, propriedades);
    }

    @Bean
    FilterRegistrationBean<FiltroDeLimiteDeTaxa> registroDoFiltroDeLimite(
            LimitadorDeTaxa limitador, PropriedadesDeLimite propriedades, ObjectMapper json) {
        FilterRegistrationBean<FiltroDeLimiteDeTaxa> registro =
                new FilterRegistrationBean<>(new FiltroDeLimiteDeTaxa(limitador, propriedades, json));
        registro.setOrder(ORDEM_DO_FILTRO);
        return registro;
    }
}
