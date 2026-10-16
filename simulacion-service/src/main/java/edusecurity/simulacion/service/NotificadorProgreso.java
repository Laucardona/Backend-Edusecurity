package edusecurity.simulacion.service;

import edusecurity.simulacion.client.ProgresoClient;
import edusecurity.simulacion.client.SimulacionCompletadaPayload;
import edusecurity.simulacion.entity.Escenario;
import edusecurity.simulacion.entity.SesionSimulacion;
import edusecurity.simulacion.repository.SesionSimulacionRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envía a progreso-service el resultado de cada sesión cerrada (FINALIZADA o VENCIDA, phishing o
 * laboratorio guiado).
 *
 * <p>Outbox ligero hasta el outbox completo de I2.2: la marca {@code progreso_notificado} de la
 * sesión hace de bandeja de salida. El envío ocurre después del commit, nunca dentro de la
 * transacción del estudiante; si progreso-service falla, se registra una advertencia, la sesión
 * queda sin marcar y {@link #reintentarPendientes()} la reenvía más tarde. progreso-service es
 * idempotente por sesionUuid, así que un reenvío nunca duplica el registro.
 */
@Component
public class NotificadorProgreso {

    private static final Logger log = LoggerFactory.getLogger(NotificadorProgreso.class);

    private final SesionSimulacionRepository sesionRepository;
    private final ProgresoClient progresoClient;
    private final TransactionTemplate transaccion;
    private final int tamanoLote;

    public NotificadorProgreso(SesionSimulacionRepository sesionRepository, ProgresoClient progresoClient,
                               PlatformTransactionManager transactionManager,
                               @Value("${app.progreso.reintento.lote:20}") int tamanoLote) {
        this.sesionRepository = sesionRepository;
        this.progresoClient = progresoClient;
        // REQUIRES_NEW: en AFTER_COMMIT los recursos de la transacción original siguen ligados al
        // hilo; sin una transacción nueva, la marca de notificado nunca se confirmaría.
        this.transaccion = new TransactionTemplate(transactionManager);
        this.transaccion.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.tamanoLote = tamanoLote;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCerrarSesion(SesionCerradaEvent evento) {
        notificar(evento.sesionId());
    }

    /** Reenvía en lotes pequeños las sesiones cerradas que progreso-service aún no recibió. */
    @Scheduled(fixedDelayString = "${app.progreso.reintento.intervalo-ms:60000}",
            initialDelayString = "${app.progreso.reintento.retraso-inicial-ms:60000}")
    public void reintentarPendientes() {
        List<Long> pendientes = sesionRepository.pendientesDeNotificar(PageRequest.of(0, tamanoLote));
        if (pendientes.isEmpty()) {
            return;
        }
        long enviadas = pendientes.stream().filter(this::notificar).count();
        log.info("Reintento de envío a progreso: {} de {} sesiones enviadas", enviadas, pendientes.size());
    }

    /**
     * Envía el resultado y marca la sesión. Nunca lanza excepciones: la petición del estudiante
     * no debe fallar porque progreso-service no esté disponible.
     *
     * @return true si progreso-service recibió el resultado
     */
    boolean notificar(Long sesionId) {
        try {
            Optional<SimulacionCompletadaPayload> payload = transaccion.execute(tx -> construirPayload(sesionId));
            if (payload == null || payload.isEmpty()) {
                return false;
            }
            // Llamada remota fuera de cualquier transacción.
            progresoClient.registrarSimulacion(payload.get());
            transaccion.executeWithoutResult(tx -> sesionRepository.findById(sesionId)
                    .ifPresent(s -> {
                        s.marcarProgresoNotificado();
                        sesionRepository.save(s);
                    }));
            return true;
        } catch (Exception ex) {
            log.warn("No se pudo enviar a progreso-service el resultado de la sesión {}: {}. Se reintentará.",
                    sesionId, ex.getMessage());
            return false;
        }
    }

    private Optional<SimulacionCompletadaPayload> construirPayload(Long sesionId) {
        return sesionRepository.findById(sesionId)
                .filter(s -> s.getPuntaje() != null && s.getFinalizadaEn() != null)
                .filter(s -> !Boolean.TRUE.equals(s.getProgresoNotificado()))
                .map(NotificadorProgreso::aPayload);
    }

    static SimulacionCompletadaPayload aPayload(SesionSimulacion s) {
        Escenario e = s.getEscenario();
        int maximo = s.getPuntajeMaximo() == null ? 0 : s.getPuntajeMaximo();
        // Mismo redondeo y umbral que la calificación que ve el estudiante.
        int porcentaje = maximo == 0 ? 0 : Math.round(s.getPuntaje() * 100f / maximo);
        int umbral = e.esLaboratorioGuiado()
                ? MaquinaLaboratorio.PORCENTAJE_APROBACION
                : CalificadorSimulacion.PORCENTAJE_APROBACION;
        return new SimulacionCompletadaPayload(s.getUsuarioId(), s.getUuid(), e.getUuid(), e.getTipo().name(),
                e.getTitulo(), s.getPuntaje(), maximo, porcentaje, porcentaje >= umbral, s.getEstado().name(),
                s.getFinalizadaEn());
    }
}
