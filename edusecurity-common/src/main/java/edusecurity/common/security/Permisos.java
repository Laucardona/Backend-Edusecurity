package edusecurity.common.security;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Catálogo de permisos (diseño §8.1, ADR-012). Los controladores protegen sus endpoints con
 * {@code @PreAuthorize("hasAuthority('<permiso>')")}; el access token lleva los permisos del rol
 * del usuario en el claim {@code permisos}.
 *
 * <p>La fuente de verdad de qué permisos tiene cada rol es la base de auth-service (tablas
 * {@code permisos} y {@code rol_permisos}, editable con {@code PUT /api/roles/{id}/permisos}).
 * {@link #PREDETERMINADOS} replica el seed inicial (migración V3 de auth) y una prueba de
 * auth-service verifica que coincidan.</p>
 */
public final class Permisos {

    public static final String PERFIL_GESTIONAR = "perfil:gestionar";
    public static final String CONTENIDO_VER = "contenido:ver";
    public static final String CONTENIDO_EDITAR = "contenido:editar";
    public static final String CONTENIDO_PUBLICAR = "contenido:publicar";
    public static final String ANUNCIOS_GESTIONAR = "anuncios:gestionar";
    public static final String FOROS_PARTICIPAR = "foros:participar";
    public static final String FOROS_CONFIGURAR = "foros:configurar";
    public static final String FOROS_MODERAR = "foros:moderar";
    public static final String EVALUACIONES_RESPONDER = "evaluaciones:responder";
    public static final String EVALUACIONES_EDITAR = "evaluaciones:editar";
    public static final String RESULTADOS_VER_TODOS = "resultados:ver-todos";
    public static final String NIVELES_GESTIONAR = "niveles:gestionar";
    public static final String SIMULACIONES_PARTICIPAR = "simulaciones:participar";
    public static final String SIMULACIONES_EDITAR = "simulaciones:editar";
    public static final String IA_GENERAR_EJERCICIOS = "ia:generar-ejercicios";
    public static final String ANALITICA_CONTENIDO = "analitica:contenido";
    public static final String ANALITICA_PANEL = "analitica:panel";
    public static final String USUARIOS_DESBLOQUEAR = "usuarios:desbloquear";
    public static final String USUARIOS_VER_SESIONES = "usuarios:ver-sesiones";
    public static final String USUARIOS_ADMINISTRAR = "usuarios:administrar";
    public static final String ROLES_ADMINISTRAR = "roles:administrar";
    public static final String ORGANIZACIONES_ADMINISTRAR = "organizaciones:administrar";
    public static final String AUDITORIA_VER = "auditoria:ver";
    public static final String ALERTAS_GESTIONAR = "alertas:gestionar";
    public static final String IA_VER_USO = "ia:ver-uso";

    public static final String ADMIN = "ADMIN";
    public static final String ESTUDIANTE = "ESTUDIANTE";
    public static final String GESTOR_CONTENIDO = "GESTOR_CONTENIDO";
    public static final String SOPORTE = "SOPORTE";

    /** Permisos iniciales de cada rol (igual al seed V3 de auth-service). */
    public static final Map<String, Set<String>> PREDETERMINADOS = Map.of(
            ESTUDIANTE, ordenados(PERFIL_GESTIONAR, CONTENIDO_VER, FOROS_PARTICIPAR, EVALUACIONES_RESPONDER,
                    SIMULACIONES_PARTICIPAR),
            GESTOR_CONTENIDO, ordenados(PERFIL_GESTIONAR, CONTENIDO_VER, CONTENIDO_EDITAR, CONTENIDO_PUBLICAR,
                    ANUNCIOS_GESTIONAR, FOROS_PARTICIPAR, FOROS_CONFIGURAR, FOROS_MODERAR, EVALUACIONES_EDITAR,
                    RESULTADOS_VER_TODOS, NIVELES_GESTIONAR, SIMULACIONES_PARTICIPAR, SIMULACIONES_EDITAR,
                    IA_GENERAR_EJERCICIOS, ANALITICA_CONTENIDO),
            SOPORTE, ordenados(PERFIL_GESTIONAR, USUARIOS_DESBLOQUEAR, USUARIOS_VER_SESIONES),
            ADMIN, ordenados(PERFIL_GESTIONAR, CONTENIDO_VER, CONTENIDO_EDITAR, CONTENIDO_PUBLICAR,
                    ANUNCIOS_GESTIONAR, FOROS_PARTICIPAR, FOROS_CONFIGURAR, FOROS_MODERAR, EVALUACIONES_EDITAR,
                    RESULTADOS_VER_TODOS, NIVELES_GESTIONAR, SIMULACIONES_PARTICIPAR, SIMULACIONES_EDITAR,
                    IA_GENERAR_EJERCICIOS, ANALITICA_CONTENIDO, ANALITICA_PANEL, USUARIOS_DESBLOQUEAR,
                    USUARIOS_VER_SESIONES, USUARIOS_ADMINISTRAR, ROLES_ADMINISTRAR, ORGANIZACIONES_ADMINISTRAR,
                    AUDITORIA_VER, ALERTAS_GESTIONAR, IA_VER_USO));

    private Permisos() {
    }

    /** Permisos iniciales del rol, ordenados; vacío si el rol no es uno de los predeterminados. */
    public static List<String> predeterminadosDe(String rol) {
        return List.copyOf(PREDETERMINADOS.getOrDefault(rol, Set.of()));
    }

    private static Set<String> ordenados(String... codigos) {
        return java.util.Collections.unmodifiableSet(new TreeSet<>(List.of(codigos)));
    }
}
