package br.com.aerozap.apigateway.rota;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Cliente HTTP do encaminhamento, com tempos limite explícitos. */
@Configuration
public class ConfiguracaoDeEncaminhamento {

    @Bean
    RestClient clienteDeEncaminhamento(TemposLimite tempos) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(tempos.conexao())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(http);
        fabrica.setReadTimeout(tempos.leitura());
        return RestClient.builder().requestFactory(fabrica).build();
    }
}
