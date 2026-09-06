-- Esquema base del microservicio de Productos (Pedidos360)
-- Base de datos: productos_db

CREATE TABLE productos (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(120)  NOT NULL,
    descripcion     VARCHAR(500)  NULL,
    precio          DECIMAL(15,2) NOT NULL,
    stock           INT           NOT NULL DEFAULT 0,
    categoria       VARCHAR(80)   NULL,
    imagen_url      VARCHAR(300)  NULL,
    id_vendedor     BIGINT        NULL,
    creado_en       TIMESTAMP     NULL,
    actualizado_en  TIMESTAMP     NULL,
    CONSTRAINT pk_productos PRIMARY KEY (id),
    CONSTRAINT chk_productos_precio CHECK (precio >= 0),
    CONSTRAINT chk_productos_stock  CHECK (stock >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_productos_categoria ON productos (categoria);
CREATE INDEX idx_productos_nombre    ON productos (nombre);
