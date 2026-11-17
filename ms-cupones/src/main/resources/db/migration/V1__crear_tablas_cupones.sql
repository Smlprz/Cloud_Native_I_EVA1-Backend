-- Cupones de descuento. Un cupon puede ser de porcentaje (1 a 100 %) o de monto fijo.
CREATE TABLE cupones (
    id             BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    codigo         VARCHAR(40)    NOT NULL,
    tipo           VARCHAR(20)    NOT NULL,
    valor          DECIMAL(12, 2) NOT NULL,
    monto_minimo   DECIMAL(12, 2) NOT NULL DEFAULT 0,
    vence_en       DATETIME,
    usos_maximos   INT,
    usos_actuales  INT            NOT NULL DEFAULT 0,
    activo         BOOLEAN        NOT NULL DEFAULT TRUE,
    creado_en      DATETIME       NOT NULL,
    CONSTRAINT uk_cupones_codigo UNIQUE (codigo)
);

-- Cada usuario puede usar un cupon una sola vez (la restriccion unica lo garantiza aunque lleguen dos
-- solicitudes a la vez).
CREATE TABLE cupon_usos (
    id               BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    cupon_id         BIGINT         NOT NULL,
    usuario_id       VARCHAR(100)   NOT NULL,
    monto_original   DECIMAL(12, 2) NOT NULL,
    descuento        DECIMAL(12, 2) NOT NULL,
    creado_en        DATETIME       NOT NULL,
    CONSTRAINT fk_usos_cupon FOREIGN KEY (cupon_id) REFERENCES cupones (id),
    CONSTRAINT uk_uso_usuario UNIQUE (cupon_id, usuario_id)
);

CREATE INDEX idx_usos_cupon ON cupon_usos (cupon_id);
