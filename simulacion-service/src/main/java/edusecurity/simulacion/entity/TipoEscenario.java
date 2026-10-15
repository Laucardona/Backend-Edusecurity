package edusecurity.simulacion.entity;

/** Tipos de laboratorio de phishing disponibles. */
public enum TipoEscenario {
    /** Ataques preparados con información de la víctima o de su empresa (spear phishing, whaling). */
    PHISHING_DIRIGIDO,
    /** Phishing masivo por correo electrónico. */
    PHISHING_CORREO,
    /** Phishing por mensajes de texto. */
    SMISHING
}
