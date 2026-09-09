-- Esquema base del microservicio de Carrito / Pedidos (Pedidos360)
-- Base de datos: carrito_db

CREATE TABLE carritos (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id      VARCHAR(100) NOT NULL,
    estado          VARCHAR(20)  NOT NULL DEFAULT 'ABIERTO',
    creado_en       TIMESTAMP    NULL,
    actualizado_en  TIMESTAMP    NULL,
    CONSTRAINT pk_carritos PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_carritos_usuario ON carritos (usuario_id, estado);

CREATE TABLE carrito_items (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    carrito_id       BIGINT        NOT NULL,
    producto_id      BIGINT        NOT NULL,
    nombre_producto  VARCHAR(120)  NOT NULL,
    precio_unitario  DECIMAL(15,2) NOT NULL,
    cantidad         INT           NOT NULL,
    CONSTRAINT pk_carrito_items PRIMARY KEY (id),
    CONSTRAINT fk_carrito_items_carrito FOREIGN KEY (carrito_id) REFERENCES carritos (id) ON DELETE CASCADE,
    CONSTRAINT chk_carrito_items_cantidad CHECK (cantidad > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_carrito_items_carrito ON carrito_items (carrito_id);

CREATE TABLE pedidos (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    usuario_id  VARCHAR(100)  NOT NULL,
    total       DECIMAL(15,2) NOT NULL,
    estado      VARCHAR(20)   NOT NULL DEFAULT 'CONFIRMADO',
    creado_en   TIMESTAMP     NULL,
    CONSTRAINT pk_pedidos PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_pedidos_usuario ON pedidos (usuario_id);

CREATE TABLE pedido_items (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    pedido_id        BIGINT        NOT NULL,
    producto_id      BIGINT        NOT NULL,
    nombre_producto  VARCHAR(120)  NOT NULL,
    precio_unitario  DECIMAL(15,2) NOT NULL,
    cantidad         INT           NOT NULL,
    CONSTRAINT pk_pedido_items PRIMARY KEY (id),
    CONSTRAINT fk_pedido_items_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos (id) ON DELETE CASCADE,
    CONSTRAINT chk_pedido_items_cantidad CHECK (cantidad > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_pedido_items_pedido ON pedido_items (pedido_id);
