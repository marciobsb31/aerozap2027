package br.com.aerozap.platformcore.system;

import br.com.aerozap.foundation.web.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint mínimo que mostra o envelope padrão de resposta. */
@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping() {
        return ApiResponse.of(Map.of("service", "platform-core", "status", "ok"));
    }
}
