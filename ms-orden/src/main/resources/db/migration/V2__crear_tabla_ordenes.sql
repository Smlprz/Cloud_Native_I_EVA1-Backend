-- V1__init_orden.sql quedo vacio por error y ya fue aplicado en las bases desplegadas,
-- asi que no se puede modificar (Flyway valida su checksum). La tabla se crea aqui.
CREATE TABLE IF NOT EXISTS ordenes (
    id          BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id  VARCHAR(100)   NOT NULL,
    total       DECIMAL(12, 2) NOT NULL,
    estado      VARCHAR(30)    NOT NULL,
    creado_en   DATETIME       NOT NULL
);

CREATE INDEX idx_ordenes_usuario ON ordenes (usuario_id);
