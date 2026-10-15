-- Resultados de simulaciones terminadas (HU43/HU44), enviados por simulacion-service a
-- POST /internal/progreso/simulaciones cuando una sesión se cierra (I7.4).
-- usuario_id, sesion_uuid y escenario_uuid pertenecen a otros servicios (sin FK).
-- sesion_uuid es único: reenviar el mismo resultado no crea duplicados.

CREATE TABLE simulaciones_completadas (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id          BIGINT        NOT NULL,
    sesion_uuid         VARCHAR(36)   NOT NULL,
    escenario_uuid      VARCHAR(36)   NOT NULL,
    tipo                VARCHAR(30)   NOT NULL,
    titulo              VARCHAR(150)  NOT NULL,
    puntaje             INT           NOT NULL,
    puntaje_maximo      INT           NOT NULL,
    porcentaje          DOUBLE        NOT NULL,
    aprobado            BOOLEAN       NOT NULL,
    estado              VARCHAR(20)   NOT NULL,
    finalizada_en       TIMESTAMP     NOT NULL,
    fecha_registro      TIMESTAMP     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_simulaciones_sesion UNIQUE (sesion_uuid)
);

CREATE INDEX idx_simulaciones_usuario ON simulaciones_completadas (usuario_id);
