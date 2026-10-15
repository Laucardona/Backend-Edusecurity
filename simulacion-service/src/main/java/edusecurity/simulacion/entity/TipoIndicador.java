package edusecurity.simulacion.entity;

/**
 * Catálogo de señales de alerta que el estudiante puede marcar en un mensaje.
 * El frontend lo obtiene de {@code GET /api/simulaciones/indicadores}.
 */
public enum TipoIndicador {
    DOMINIO_SIMILAR("Dominio parecido al real", "El remitente o el enlace imitan un dominio conocido cambiando letras o agregando palabras."),
    REMITENTE_SOSPECHOSO("Remitente sospechoso", "El remitente no corresponde a quien dice ser: correo gratuito, número desconocido o dirección externa."),
    SUPLANTACION_AUTORIDAD("Suplantación de autoridad", "Se hace pasar por un jefe, directivo o entidad para que no cuestiones la orden."),
    URGENCIA("Urgencia", "Plazos muy cortos para que actúes sin verificar."),
    AMENAZA("Amenaza o consecuencia", "Advierte de bloqueos, multas o pérdidas si no actúas."),
    SECRETISMO("Petición de secreto", "Pide no contarle a nadie o saltarse los controles habituales."),
    SOLICITUD_INUSUAL("Solicitud inusual", "Pide pagos, cambios de cuenta o datos fuera del proceso normal."),
    PIDE_CREDENCIALES("Pide credenciales", "Solicita usuario, contraseña, token o códigos de verificación."),
    ENLACE_ENGANOSO("Enlace engañoso", "El texto del enlace no coincide con su destino real."),
    ADJUNTO_PELIGROSO("Adjunto peligroso", "Archivo ejecutable, con macros o con doble extensión."),
    CAMBIO_DE_CANAL("Cambio de canal", "Intenta llevar la conversación a WhatsApp, teléfono personal u otro canal fuera de la empresa."),
    DATOS_PERSONALES("Uso de datos personales", "Menciona datos tuyos o internos de la empresa para ganar confianza.");

    private final String etiqueta;
    private final String descripcion;

    TipoIndicador(String etiqueta, String descripcion) {
        this.etiqueta = etiqueta;
        this.descripcion = descripcion;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
