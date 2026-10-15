package edusecurity.progreso.analitica;

import edusecurity.common.exception.BadRequestException;
import edusecurity.progreso.analitica.dto.PanelAdministrativoResponse;
import edusecurity.progreso.analitica.dto.PanelAdministrativoResponse.EscenarioTop;
import edusecurity.progreso.analitica.dto.PanelAdministrativoResponse.Periodo;
import edusecurity.progreso.analitica.dto.PanelAdministrativoResponse.Progreso;
import edusecurity.progreso.analitica.dto.PanelAdministrativoResponse.ProgresoModulo;
import edusecurity.progreso.analitica.dto.PanelAdministrativoResponse.Simulaciones;
import edusecurity.progreso.analitica.dto.PanelAdministrativoResponse.SimulacionesTipo;
import edusecurity.progreso.analitica.dto.PanelAdministrativoResponse.Usuarios;
import edusecurity.progreso.client.AuthClient;
import edusecurity.progreso.client.MetricasUsuariosFeign;
import edusecurity.progreso.repository.ProgresoRepository;
import edusecurity.progreso.repository.SimulacionCompletadaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Panel administrativo (HU33). Analítica vive por ahora como módulo de progreso-service (ADR-001):
 * usa los datos locales de progreso y simulaciones y pide a auth-service las métricas de usuarios.
 * Si auth-service no responde, el panel se entrega igual sin ese bloque.
 */
@Service
public class PanelAdministrativoService {

    static final int DIAS_POR_DEFECTO = 30;
    static final int MAX_DIAS = 366;
    static final int TOP_ESCENARIOS = 5;
    static final String FUENTE_AUTH = "auth";

    private static final Logger log = LoggerFactory.getLogger(PanelAdministrativoService.class);

    private final ProgresoRepository progresoRepository;
    private final SimulacionCompletadaRepository simulacionRepository;
    private final AuthClient authClient;
    private final Clock clock;

    public PanelAdministrativoService(ProgresoRepository progresoRepository,
                                      SimulacionCompletadaRepository simulacionRepository,
                                      AuthClient authClient, Clock clock) {
        this.progresoRepository = progresoRepository;
        this.simulacionRepository = simulacionRepository;
        this.authClient = authClient;
        this.clock = clock;
    }

    /** Sin fechas: los últimos {@value #DIAS_POR_DEFECTO} días hasta hoy (UTC), ambos incluidos. */
    @Transactional(readOnly = true)
    public PanelAdministrativoResponse panel(LocalDate desde, LocalDate hasta) {
        Periodo periodo = periodo(desde, hasta);
        List<String> noDisponibles = new ArrayList<>();

        Usuarios usuarios = usuarios(periodo, noDisponibles);
        return new PanelAdministrativoResponse(periodo, usuarios, progreso(), simulaciones(periodo), null,
                List.copyOf(noDisponibles));
    }

    Periodo periodo(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta != null ? hasta
                : desde != null ? desde.plusDays(DIAS_POR_DEFECTO - 1L) : LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate inicio = desde != null ? desde : fin.minusDays(DIAS_POR_DEFECTO - 1L);
        if (inicio.isAfter(fin)) {
            throw new BadRequestException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > MAX_DIAS) {
            throw new BadRequestException("El periodo no puede superar " + MAX_DIAS + " días");
        }
        return new Periodo(inicio, fin);
    }

    private Usuarios usuarios(Periodo periodo, List<String> noDisponibles) {
        try {
            MetricasUsuariosFeign m = authClient.obtenerMetricas(periodo.desde(), periodo.hasta());
            return new Usuarios(m.totalUsuarios(), m.usuariosActivos(), m.porRol(), m.bloqueadosAhora(),
                    m.usuariosConSesionEnPeriodo(), m.iniciosSesionEnPeriodo());
        } catch (Exception ex) {
            log.warn("Panel administrativo sin métricas de usuarios: auth-service no respondió ({})", ex.getMessage());
            noDisponibles.add(FUENTE_AUTH);
            return null;
        }
    }

    private Progreso progreso() {
        Object[] general = progresoRepository.resumenGeneral().stream().findFirst().orElse(new Object[3]);
        List<ProgresoModulo> porModulo = progresoRepository.resumenPorModulo().stream()
                .map(f -> new ProgresoModulo((Long) f[0], redondear(doble(f[1])), largo(f[2])))
                .toList();
        return new Progreso(redondear(doble(general[0])), largo(general[1]), largo(general[2]), porModulo);
    }

    private Simulaciones simulaciones(Periodo periodo) {
        LocalDateTime inicio = periodo.desde().atStartOfDay();
        LocalDateTime fin = periodo.hasta().plusDays(1).atStartOfDay();

        // Los tipos de phishing (PHISHING_DIRIGIDO, PHISHING_CORREO, SMISHING) se agrupan en PHISHING.
        long[] phishing = new long[2];
        long[] laboratorio = new long[2];
        double[] sumaPorcentaje = new double[2];
        for (Object[] f : simulacionRepository.resumenPorTipo(inicio, fin)) {
            int i = "LABORATORIO".equals(f[0]) ? 1 : 0;
            long[] destino = i == 1 ? laboratorio : phishing;
            destino[0] += largo(f[1]);
            destino[1] += largo(f[2]);
            sumaPorcentaje[i] += doble(f[3]);
        }

        Map<String, SimulacionesTipo> porTipo = new LinkedHashMap<>();
        porTipo.put("PHISHING", new SimulacionesTipo(phishing[0], phishing[1], promedio(sumaPorcentaje[0], phishing[0])));
        porTipo.put("LABORATORIO",
                new SimulacionesTipo(laboratorio[0], laboratorio[1], promedio(sumaPorcentaje[1], laboratorio[0])));

        List<EscenarioTop> top = simulacionRepository
                .escenariosMasCompletados(inicio, fin, PageRequest.of(0, TOP_ESCENARIOS)).stream()
                .map(f -> new EscenarioTop((String) f[0], (String) f[1], largo(f[2]), tasa(largo(f[3]), largo(f[2]))))
                .toList();

        long completadas = phishing[0] + laboratorio[0];
        long aprobadas = phishing[1] + laboratorio[1];
        return new Simulaciones(completadas, aprobadas, tasa(aprobadas, completadas), porTipo, top);
    }

    /** Porcentaje con un decimal; 0 si no hay datos. */
    static double tasa(long parte, long total) {
        return total == 0 ? 0 : Math.round(parte * 1000.0 / total) / 10.0;
    }

    private static double promedio(double suma, long cantidad) {
        return cantidad == 0 ? 0 : redondear(suma / cantidad);
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100) / 100.0;
    }

    private static double doble(Object valor) {
        return valor == null ? 0 : ((Number) valor).doubleValue();
    }

    private static long largo(Object valor) {
        return valor == null ? 0 : ((Number) valor).longValue();
    }
}
