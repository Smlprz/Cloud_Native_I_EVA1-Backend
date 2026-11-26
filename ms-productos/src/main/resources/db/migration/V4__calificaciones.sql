-- Calificacion de cada producto, calculada con las resenas que llegan por la cola resena.creada.
ALTER TABLE productos
    ADD COLUMN resenas_cantidad      INT           NOT NULL DEFAULT 0,
    ADD COLUMN calificacion_promedio DECIMAL(3, 2) NULL;

-- Resenas ya contadas: evita sumar dos veces la misma resena si el mensaje llega duplicado.
CREATE TABLE resenas_procesadas (
    id          BIGINT    NOT NULL AUTO_INCREMENT,
    resena_id   BIGINT    NOT NULL,
    creado_en   TIMESTAMP NULL,
    CONSTRAINT pk_resenas_procesadas PRIMARY KEY (id),
    CONSTRAINT uq_resenas_procesadas_resena UNIQUE (resena_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
