CREATE TABLE resenas (
    id              BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    producto_id     BIGINT        NOT NULL,
    usuario_id      VARCHAR(100)  NOT NULL,
    usuario_nombre  VARCHAR(150),
    puntaje         INT           NOT NULL,
    comentario      VARCHAR(1000),
    creado_en       DATETIME      NOT NULL,
    INDEX idx_resenas_producto (producto_id)
);
