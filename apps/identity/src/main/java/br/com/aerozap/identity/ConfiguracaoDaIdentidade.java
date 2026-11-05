package br.com.aerozap.identity;

import br.com.aerozap.identity.seguranca.PropriedadesDeToken;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PropriedadesDeToken.class)
public class ConfiguracaoDaIdentidade {
}
