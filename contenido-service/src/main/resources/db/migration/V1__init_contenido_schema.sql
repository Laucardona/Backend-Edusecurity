-- Esquema del microservicio de contenido educativo (edusecurity_contenido)
-- Compatible con MySQL 8 y H2 en modo MySQL (usado por los tests)

CREATE TABLE modulos (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    titulo             VARCHAR(150)  NOT NULL,
    descripcion        VARCHAR(1000),
    activo             BOOLEAN       NOT NULL DEFAULT TRUE,
    orden              INT,
    fecha_creacion     TIMESTAMP     NOT NULL,
    fecha_actualizacion TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_modulos_activo ON modulos (activo);

CREATE TABLE lecciones (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    titulo             VARCHAR(150)  NOT NULL,
    descripcion        VARCHAR(1000),
    contenido          VARCHAR(5000),
    orden              INT,
    activo             BOOLEAN       NOT NULL DEFAULT TRUE,
    modulo_id          BIGINT        NOT NULL,
    fecha_creacion     TIMESTAMP     NOT NULL,
    fecha_actualizacion TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_lecciones_modulo FOREIGN KEY (modulo_id) REFERENCES modulos (id)
);

CREATE INDEX idx_lecciones_modulo ON lecciones (modulo_id);
