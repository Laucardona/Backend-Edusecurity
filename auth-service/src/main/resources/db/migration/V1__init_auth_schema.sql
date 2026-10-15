-- Esquema del microservicio de autenticación (edusecurity_auth)
-- Compatible con MySQL 8 y H2 en modo MySQL (usado por los tests)

CREATE TABLE roles (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(60)  NOT NULL,
    descripcion   VARCHAR(250),
    PRIMARY KEY (id),
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
);

CREATE TABLE usuarios (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(150)  NOT NULL,
    correo        VARCHAR(150)  NOT NULL,
    password      VARCHAR(100)  NOT NULL,
    activo        BOOLEAN       NOT NULL DEFAULT TRUE,
    rol_id        BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_correo UNIQUE (correo),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
);

CREATE INDEX idx_usuarios_rol ON usuarios (rol_id);
