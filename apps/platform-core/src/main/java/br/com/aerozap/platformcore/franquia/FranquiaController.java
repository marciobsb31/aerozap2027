package br.com.aerozap.platformcore.franquia;

import br.com.aerozap.contracts.usage.ReleaseRequest;
import br.com.aerozap.contracts.usage.ReservationRequest;
import br.com.aerozap.contracts.usage.ReservationResponse;
import br.com.aerozap.foundation.web.ApiResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API interna da franquia (ADR-0004).
 *
 * <p>Fica em {@code /internal} porque quem chama é outro serviço do AeroZap, não o navegador: a
 * pesquisa reserva antes de começar e confirma ou libera no fim. O saldo que a pessoa vê na tela
 * é o {@code GET /saldo}, e nele o usuário vem do cabeçalho posto pelo gateway — nunca do corpo,
 * senão qualquer um consulta (e gasta) a franquia de outro.
 */
@RestController
@RequestMapping("/internal/usage")
public class FranquiaController {

    private final ServicoDeFranquia servico;

    public FranquiaController(ServicoDeFranquia servico) {
        this.servico = servico;
    }

    @PostMapping("/reservations")
    public ApiResponse<ReservationResponse> reservar(@Valid @RequestBody ReservationRequest pedido) {
        try {
            return ApiResponse.of(servico.reservar(pedido, inicioDaConta()));
        } catch (ServicoDeFranquia.ReservaJaExiste corrida) {
            // Outra requisição idêntica ganhou a corrida. A transação desta foi desfeita pelo
            // índice único, inclusive o débito; aqui se lê, fora dela, a reserva que ficou de pé.
            return ApiResponse.of(servico.reservaJaCriada(corrida.usuario,
                    corrida.pedido.idempotencyKey(), corrida.pedido.searchId()));
        }
    }

    @PostMapping("/reservations/{reservaId}/confirm")
    public ApiResponse<ReservationResponse> confirmar(@PathVariable UUID reservaId) {
        return ApiResponse.of(servico.confirmar(reservaId));
    }

    @PostMapping("/reservations/{reservaId}/release")
    public ApiResponse<ReservationResponse> liberar(@PathVariable UUID reservaId,
                                                    @Valid @RequestBody ReleaseRequest pedido) {
        return ApiResponse.of(servico.liberar(reservaId, pedido.reason()));
    }

    @GetMapping("/saldo")
    public ApiResponse<ServicoDeFranquia.Saldo> saldo(@RequestHeader("X-Usuario-Id") UUID usuarioId) {
        return ApiResponse.of(servico.saldo(usuarioId, inicioDaConta()));
    }

    /**
     * Data de início da conta, usada só pela janela móvel.
     *
     * <p>Hoje o módulo da franquia não lê a tabela da identidade — a ADR-0002 proíbe um módulo
     * tocar o schema de outro. Enquanto a API interna da identidade não existir, o trimestre-
     * calendário (que ignora este valor) é o padrão configurado, e a janela móvel fica disponível
     * mas não selecionada. Trocar a configuração sem essa API daria períodos errados, e é melhor
     * que isso esteja escrito aqui do que descoberto depois.
     */
    private Instant inicioDaConta() {
        return Instant.EPOCH;
    }
}
