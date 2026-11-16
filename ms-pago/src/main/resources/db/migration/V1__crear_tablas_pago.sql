-- Pagos de las ordenes. El numero de tarjeta y el CVV NUNCA se guardan: solo los ultimos 4 digitos
-- y un hash (SHA-256 con pepper) para detectar la misma tarjeta repetida.
CREATE TABLE pagos (
    id               BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    orden_id         BIGINT         NOT NULL,
    usuario_id       VARCHAR(100)   NOT NULL,
    monto            DECIMAL(12, 2) NOT NULL,
    tarjeta_ultimos4 VARCHAR(4)     NOT NULL,
    tarjeta_hash     VARCHAR(64)    NOT NULL,
    estado           VARCHAR(30)    NOT NULL,
    motivo           VARCHAR(255),
    creado_en        DATETIME       NOT NULL
);

CREATE INDEX idx_pagos_orden ON pagos (orden_id);
CREATE INDEX idx_pagos_usuario ON pagos (usuario_id);

-- Alertas generadas por la regla antifraude (misma tarjeta repetida en una ventana corta).
CREATE TABLE alertas_fraude (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id       VARCHAR(100) NOT NULL,
    orden_id         BIGINT       NOT NULL,
    tarjeta_ultimos4 VARCHAR(4)   NOT NULL,
    tarjeta_hash     VARCHAR(64)  NOT NULL,
    intentos         INT          NOT NULL,
    ventana_segundos INT          NOT NULL,
    creado_en        DATETIME     NOT NULL
);

CREATE INDEX idx_alertas_creado ON alertas_fraude (creado_en);
