-- Transacciones iniciadas en Webpay. pago_id queda nulo mientras el usuario esta en el formulario de Transbank.
CREATE TABLE webpay_transacciones (
    token      VARCHAR(100)   NOT NULL PRIMARY KEY,
    orden_id   BIGINT         NOT NULL,
    usuario_id VARCHAR(100)   NOT NULL,
    monto      DECIMAL(12, 2) NOT NULL,
    buy_order  VARCHAR(26)    NOT NULL,
    pago_id    BIGINT,
    creado_en  DATETIME       NOT NULL
);
