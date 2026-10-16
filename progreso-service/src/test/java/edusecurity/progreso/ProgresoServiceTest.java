package edusecurity.progreso;

import edusecurity.common.exception.ForbiddenException;
import edusecurity.common.exception.ResourceNotFoundException;
import edusecurity.common.security.TokenInfo;
import edusecurity.progreso.client.AuthClient;
import edusecurity.progreso.client.ContenidoClient;
import edusecurity.progreso.client.LeccionFeign;
import edusecurity.progreso.client.UsuarioFeign;
import edusecurity.progreso.dto.ProgresoRequest;
import edusecurity.progreso.dto.ProgresoResponse;
import edusecurity.progreso.dto.ResumenUsuarioResponse;
import edusecurity.progreso.entity.Progreso;
import edusecurity.progreso.repository.ProgresoRepository;
import edusecurity.progreso.repository.SimulacionCompletadaRepository;
import edusecurity.progreso.service.ProgresoService;
import edusecurity.progreso.service.ResumenService;
import edusecurity.common.web.PageResponse;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProgresoServiceTest {

    @Mock
    private ProgresoRepository repository;

    @Mock
    private AuthClient authClient;

    @Mock
    private ContenidoClient contenidoClient;

    @InjectMocks
    private ProgresoService progresoService;

    @Mock
    private SimulacionCompletadaRepository simulacionRepository;

    private ResumenService resumenService;

    private TokenInfo estudiante;
    private TokenInfo admin;

    private LeccionFeign leccion;

    @BeforeEach
    void setUp() {
        // Mockito inyecta progresoService antes de los @BeforeEach
        resumenService = new ResumenService(progresoService, repository, simulacionRepository);

        estudiante = new TokenInfo("laura@correo.com", 1L, "ESTUDIANTE", "access");
        admin = new TokenInfo("admin@correo.com", 99L, "ADMIN", "access");

        leccion = new LeccionFeign(10L, "Lección demo", true, 1, null,
                new LeccionFeign.ModuloFeign(1L, "Módulo Ciberseguridad", "Desc", true, 1,
                        null, null, 5L));

        when(authClient.obtenerUsuario(anyLong()))
                .thenReturn(new UsuarioFeign(1L, "Laura", "laura@correo.com", true));
        when(contenidoClient.obtenerLeccion(10L)).thenReturn(leccion);
        when(contenidoClient.listarLecciones(anyInt()))
                .thenReturn(new PageResponse<>(List.of(leccion, leccion), 0, 5000, 2, 1, true, true));
    }

    @Test
    void registraProgresoNuevoConDatosDenormalizados() {
        when(repository.findByUsuarioIdAndLeccionId(1L, 10L)).thenReturn(Optional.empty());
        when(repository.save(any(Progreso.class))).thenAnswer(inv -> {
            Progreso p = inv.getArgument(0);
            p.setId(55L);
            return p;
        });

        ProgresoResponse respuesta = progresoService.guardar(
                new ProgresoRequest(null, 10L, 40.0, false), estudiante);

        assertThat(respuesta.id()).isEqualTo(55L);
        assertThat(respuesta.usuarioId()).isEqualTo(1L);
        assertThat(respuesta.usuarioNombre()).isEqualTo("Laura");
        assertThat(respuesta.leccionTitulo()).isEqualTo("Lección demo");
        assertThat(respuesta.moduloTitulo()).isEqualTo("Módulo Ciberseguridad");
        assertThat(respuesta.porcentaje()).isEqualTo(40.0);
        assertThat(respuesta.completado()).isFalse();
    }

    @Test
    void completadoFuerzaPorcentaje100() {
        when(repository.findByUsuarioIdAndLeccionId(1L, 10L)).thenReturn(Optional.empty());
        when(repository.save(any(Progreso.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgresoResponse respuesta = progresoService.guardar(
                new ProgresoRequest(null, 10L, 12.0, true), estudiante);

        assertThat(respuesta.porcentaje()).isEqualTo(100.0);
        assertThat(respuesta.completado()).isTrue();
        assertThat(respuesta.moduloId()).isEqualTo(1L);
    }

    @Test
    void actualizaProgresoExistente() {
        Progreso existente = new Progreso();
        existente.setId(7L);
        existente.setUsuarioId(1L);
        existente.setLeccionId(10L);
        existente.setModuloId(1L);
        existente.setPorcentaje(10.0);
        existente.setCompletado(false);

        when(repository.findByUsuarioIdAndLeccionId(1L, 10L)).thenReturn(Optional.of(existente));
        when(repository.save(any(Progreso.class))).thenReturn(existente);

        ProgresoResponse respuesta = progresoService.guardar(
                new ProgresoRequest(null, 10L, 80.0, false), estudiante);

        assertThat(respuesta.id()).isEqualTo(7L);
        assertThat(respuesta.porcentaje()).isEqualTo(80.0);
    }

    @Test
    void estudianteNoPuedeRegistrarProgresoDeOtro() {
        assertThatThrownBy(() -> progresoService.guardar(
                new ProgresoRequest(2L, 10L, 50.0, false), estudiante))
                .isInstanceOf(ForbiddenException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void adminSiPuedeRegistrarProgresoDeOtroUsuario() {
        when(repository.findByUsuarioIdAndLeccionId(2L, 10L)).thenReturn(Optional.empty());
        when(repository.save(any(Progreso.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgresoResponse respuesta = progresoService.guardar(
                new ProgresoRequest(2L, 10L, 50.0, false), admin);

        assertThat(respuesta.usuarioId()).isEqualTo(2L);
    }

    @Test
    void leccionInexistenteDevuelve404() {
        when(contenidoClient.obtenerLeccion(777L)).thenReturn(null);

        assertThatThrownBy(() -> progresoService.guardar(
                new ProgresoRequest(null, 777L, 10.0, false), estudiante))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resumenDeUsuarioCalculaPromedioYCompletadas() {
        Progreso p1 = progresoGuardado(1L, 10L, 100.0, true);
        Progreso p2 = progresoGuardado(1L, 11L, 50.0, false);

        when(repository.findByUsuarioId(1L)).thenReturn(List.of(p1, p2));

        ResumenUsuarioResponse resumen = resumenService.resumenUsuario(1L);

        assertThat(resumen.usuarioNombre()).isEqualTo("Laura");
        assertThat(resumen.leccionesRegistradas()).isEqualTo(2);
        assertThat(resumen.leccionesCompletadas()).isEqualTo(1);
        assertThat(resumen.porcentajePromedio()).isEqualTo(75.0);
        assertThat(resumen.totalLecciones()).isEqualTo(2);
        assertThat(resumen.porModulo()).hasSize(1);
        assertThat(resumen.porModulo().get(0).moduloTitulo()).isEqualTo("Módulo Ciberseguridad");
    }

    @Test
    void listarEnriqueceConNombresDeUsuario() {
        Progreso p = progresoGuardado(1L, 10L, 30.0, false);
        when(repository.buscar(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));

        PageResponse<ProgresoResponse> page =
                progresoService.listar(null, null, null, PageRequest.of(0, 20));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).usuarioNombre()).isEqualTo("Laura");
        assertThat(page.content().get(0).leccionTitulo()).isEqualTo("Lección demo");
    }

    private Progreso progresoGuardado(Long usuarioId, Long leccionId, double porcentaje,
                                      boolean completado) {
        Progreso p = new Progreso();
        p.setId(1L);
        p.setUsuarioId(usuarioId);
        p.setLeccionId(leccionId);
        p.setModuloId(1L);
        p.setPorcentaje(porcentaje);
        p.setCompletado(completado);
        return p;
    }
}
