-- M01: bloqueo por intentos fallidos (HU38), sesiones / cierre de sesión (HU3, HU37)
-- y recuperación de contraseña (HU4). Compatible con MySQL 8 y H2 en modo MySQL.

ALTER TABLE usuarios ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN bloqueado_hasta TIMESTAMP NULL;

-- Una fila por inicio de sesión. El refresh token se guarda solo como hash SHA-256
-- y se actualiza en cada rotación; cerrar sesión marca revocada_en.
CREATE TABLE sesiones (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id          BIGINT        NOT NULL,
    refresh_token_hash  VARCHAR(64)  NOT NULL,
    ip                  VARCHAR(45),
    dispositivo         VARCHAR(255),
    creada_en           TIMESTAMP     NOT NULL,
    ultimo_uso          TIMESTAMP     NOT NULL,
    expira_en           TIMESTAMP     NOT NULL,
    revocada_en         TIMESTAMP     NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_sesiones_token UNIQUE (refresh_token_hash),
    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_sesiones_usuario ON sesiones (usuario_id, creada_en);

-- Tokens de un solo uso para restablecer la contraseña. Solo se guarda el hash.
CREATE TABLE tokens_recuperacion (
    id          BIGINT     NOT NULL AUTO_INCREMENT,
    usuario_id  BIGINT     NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    creado_en   TIMESTAMP  NOT NULL,
    expira_en   TIMESTAMP  NOT NULL,
    usado_en    TIMESTAMP  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_tokens_recuperacion_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_recuperacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_tokens_recuperacion_usuario ON tokens_recuperacion (usuario_id);
