-- Cupones de descuento (ms-cupones): el pedido guarda cuanto se descuento y con que cupon.
-- El total del pedido ya viene con el descuento restado.
ALTER TABLE pedidos
    ADD COLUMN descuento    DECIMAL(15,2) NOT NULL DEFAULT 0,
    ADD COLUMN cupon_codigo VARCHAR(40)   NULL;
