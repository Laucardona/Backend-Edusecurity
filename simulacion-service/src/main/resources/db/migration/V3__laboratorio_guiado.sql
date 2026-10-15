-- Laboratorios virtuales guiados (HU44). Un laboratorio es una máquina de estados en el backend:
-- cada paso muestra una situación y un recurso simulado (salida de terminal, registro, alerta o
-- pantalla, como texto) y el estudiante elige una opción que lo lleva al siguiente paso.
-- No se ejecuta código real ni hay sandbox (ADR-013).
-- Compatible con MySQL 8 y H2 en modo MySQL.

CREATE TABLE pasos_laboratorio (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    escenario_id         BIGINT        NOT NULL,
    codigo               VARCHAR(40)   NOT NULL,
    orden                INT           NOT NULL,
    titulo               VARCHAR(150)  NOT NULL,
    situacion            TEXT          NOT NULL,
    recurso_simulado     TEXT,
    es_final             BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uk_pasos_escenario_codigo UNIQUE (escenario_id, codigo),
    CONSTRAINT fk_pasos_escenario FOREIGN KEY (escenario_id) REFERENCES escenarios (id) ON DELETE CASCADE
);

CREATE INDEX idx_pasos_escenario ON pasos_laboratorio (escenario_id, orden);

-- siguiente_paso_codigo NULL termina el laboratorio.
CREATE TABLE opciones_paso (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    paso_id               BIGINT        NOT NULL,
    codigo                VARCHAR(10)   NOT NULL,
    texto                 VARCHAR(500)  NOT NULL,
    puntos                INT           NOT NULL,
    es_correcta           BOOLEAN       NOT NULL,
    retroalimentacion     TEXT          NOT NULL,
    siguiente_paso_codigo VARCHAR(40),
    PRIMARY KEY (id),
    CONSTRAINT uk_opciones_paso_codigo UNIQUE (paso_id, codigo),
    CONSTRAINT fk_opciones_paso FOREIGN KEY (paso_id) REFERENCES pasos_laboratorio (id) ON DELETE CASCADE
);

-- Una acción por paso y sesión: cada paso se responde una sola vez.
CREATE TABLE acciones_laboratorio (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    sesion_id            BIGINT        NOT NULL,
    paso_id              BIGINT        NOT NULL,
    opcion_id            BIGINT        NOT NULL,
    puntos               INT           NOT NULL,
    ocurrida_en          TIMESTAMP     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_acciones_sesion_paso UNIQUE (sesion_id, paso_id),
    CONSTRAINT fk_acciones_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones_simulacion (id) ON DELETE CASCADE,
    CONSTRAINT fk_acciones_paso FOREIGN KEY (paso_id) REFERENCES pasos_laboratorio (id),
    CONSTRAINT fk_acciones_opcion FOREIGN KEY (opcion_id) REFERENCES opciones_paso (id)
);

ALTER TABLE sesiones_simulacion ADD COLUMN paso_actual_id BIGINT NULL;
ALTER TABLE sesiones_simulacion
    ADD CONSTRAINT fk_sesiones_paso_actual FOREIGN KEY (paso_actual_id) REFERENCES pasos_laboratorio (id);
