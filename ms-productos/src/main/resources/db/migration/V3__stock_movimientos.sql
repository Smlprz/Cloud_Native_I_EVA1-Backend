-- Registro de las ordenes cuyo stock ya fue descontado (ms-productos consume stock.descontar).
-- RabbitMQ garantiza "al menos una entrega": el UNIQUE sobre orden_id evita descontar dos veces
-- el stock de la misma orden si el mensaje llega duplicado.

CREATE TABLE stock_movimientos (
    id          BIGINT    NOT NULL AUTO_INCREMENT,
    orden_id    BIGINT    NOT NULL,
    creado_en   TIMESTAMP NULL,
    CONSTRAINT pk_stock_movimientos PRIMARY KEY (id),
    CONSTRAINT uq_stock_movimientos_orden UNIQUE (orden_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
