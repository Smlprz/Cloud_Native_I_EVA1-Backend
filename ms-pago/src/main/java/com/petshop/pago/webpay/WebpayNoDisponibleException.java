package com.petshop.pago.webpay;

public class WebpayNoDisponibleException extends RuntimeException {

    public WebpayNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
