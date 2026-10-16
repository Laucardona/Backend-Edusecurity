package edusecurity.simulacion.entity;

/** Tipos de escenario: laboratorios de phishing con bandeja y laboratorios guiados por pasos. */
public enum TipoEscenario {
    /** Ataques preparados con información de la víctima o de su empresa (spear phishing, whaling). */
    PHISHING_DIRIGIDO,
    /** Phishing masivo por correo electrónico. */
    PHISHING_CORREO,
    /** Phishing por mensajes de texto. */
    SMISHING,
    /**
     * Laboratorio guiado (HU44): una máquina de estados de pasos con recursos simulados
     * (alertas, registros, pantallas) en texto. No se ejecuta código real (ADR-013).
     */
    LABORATORIO;

    public boolean esLaboratorioGuiado() {
        return this == LABORATORIO;
    }
}
