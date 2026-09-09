-- Esquema del microservicio de Identidad (Pedidos360)
-- Base de datos: auth_db

CREATE TABLE accesos (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id  VARCHAR(100) NOT NULL,
    nombre      VARCHAR(150) NULL,
    email       VARCHAR(150) NULL,
    roles       VARCHAR(200) NULL,
    ip          VARCHAR(45)  NULL,
    creado_en   TIMESTAMP    NULL,
    CONSTRAINT pk_accesos PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_accesos_usuario ON accesos (usuario_id, creado_en);
