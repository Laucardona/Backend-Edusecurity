-- Esquema del microservicio de progreso (edusecurity_progreso)
-- Los ids de usuario/lección/módulo referencian otros microservicios
-- (sin claves foráneas: cada servicio tiene su propia base de datos).

CREATE TABLE progreso (
    id                  BIGINT   NOT NULL AUTO_INCREMENT,
    usuario_id          BIGINT   NOT NULL,
    leccion_id          BIGINT   NOT NULL,
    modulo_id           BIGINT   NOT NULL,
    porcentaje          DOUBLE   NOT NULL,
    completado          BOOLEAN  NOT NULL DEFAULT FALSE,
    fecha_creacion      TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_progreso_usuario_leccion UNIQUE (usuario_id, leccion_id)
);

CREATE INDEX idx_progreso_usuario ON progreso (usuario_id);
CREATE INDEX idx_progreso_leccion ON progreso (leccion_id);
CREATE INDEX idx_progreso_modulo ON progreso (modulo_id);
