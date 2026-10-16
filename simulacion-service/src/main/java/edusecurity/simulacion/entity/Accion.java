package edusecurity.simulacion.entity;

/** Lo que el estudiante haría con el mensaje. */
public enum Accion {
    REPORTAR,
    ELIMINAR,
    VERIFICAR_POR_OTRO_CANAL,
    CONSERVAR,
    RESPONDER,
    ABRIR_ENLACE;

    /** Acciones que no exponen al usuario aunque el mensaje sea phishing. */
    public boolean esSeguraAnteAtaque() {
        return this == REPORTAR || this == ELIMINAR || this == VERIFICAR_POR_OTRO_CANAL;
    }

    /** Acciones que, ante un mensaje de phishing, entregan datos o ejecutan el ataque. */
    public boolean esPeligrosaAnteAtaque() {
        return this == RESPONDER || this == ABRIR_ENLACE;
    }
}
