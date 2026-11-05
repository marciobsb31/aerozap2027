package br.com.aerozap.platformcore.franquia;

import br.com.aerozap.foundation.web.ApiResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * O saldo da própria pessoa, alcançável pelo gateway.
 *
 * <p>Reservar, confirmar e liberar continuam em {@code /internal/usage}: são chamados pelos
 * serviços, e o gateway só encaminha o que está sob {@code /api}. O usuário vem do cabeçalho que a
 * borda preenche a partir do token, nunca do corpo nem da query (RF-007).
 */
@RestController
@RequestMapping("/api/v1/usage")
public class SaldoController {

    private final ServicoDeFranquia servico;

    public SaldoController(ServicoDeFranquia servico) {
        this.servico = servico;
    }

    @GetMapping("/saldo")
    public ApiResponse<ServicoDeFranquia.Saldo> saldo(@RequestHeader("X-Usuario-Id") UUID usuarioId) {
        return ApiResponse.of(servico.saldo(usuarioId, FranquiaController.inicioDaConta()));
    }
}
