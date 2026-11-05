package br.com.aerozap.platformcore;

import br.com.aerozap.platformcore.franquia.PropriedadesDeFranquia;
import br.com.aerozap.platformcore.identidade.seguranca.PropriedadesDeToken;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({PropriedadesDeToken.class, PropriedadesDeFranquia.class})
public class ConfiguracaoDosModulos {
    // Propriedades dos módulos do platform-core, registradas num ponto só.
}
