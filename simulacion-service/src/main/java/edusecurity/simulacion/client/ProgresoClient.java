package edusecurity.simulacion.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente de progreso-service. Usa la ruta interna (rol INTERNAL con X-Service-Key), que el
 * gateway no expone.
 */
@FeignClient(name = "edusecurity-progreso", path = "/internal/progreso")
public interface ProgresoClient {

    /** Idempotente por sesionUuid: reenviar el mismo resultado no crea duplicados. */
    @PostMapping("/simulaciones")
    void registrarSimulacion(@RequestBody SimulacionCompletadaPayload payload);
}
