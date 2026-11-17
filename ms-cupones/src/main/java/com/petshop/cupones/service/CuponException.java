package com.petshop.cupones.service;

import org.springframework.http.HttpStatus;

/** Un cupon que no se puede usar (o crear), con el codigo HTTP que le corresponde y un mensaje claro para el cliente. */
public class CuponException extends RuntimeException {

    private final HttpStatus status;

    private CuponException(HttpStatus status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public static CuponException noExiste() {
        return new CuponException(HttpStatus.NOT_FOUND, "El cupon no existe o no esta activo");
    }

    public static CuponException vencido() {
        return new CuponException(HttpStatus.BAD_REQUEST, "El cupon esta vencido");
    }

    public static CuponException montoMinimo(java.math.BigDecimal minimo) {
        return new CuponException(HttpStatus.BAD_REQUEST, "El cupon exige una compra minima de $" + minimo.toPlainString());
    }

    public static CuponException agotado() {
        return new CuponException(HttpStatus.CONFLICT, "El cupon ya alcanzo su maximo de usos");
    }

    public static CuponException yaUsado() {
        return new CuponException(HttpStatus.CONFLICT, "Ya usaste este cupon");
    }

    public static CuponException codigoDuplicado(String codigo) {
        return new CuponException(HttpStatus.CONFLICT, "Ya existe un cupon con el codigo " + codigo);
    }

    public static CuponException invalido(String mensaje) {
        return new CuponException(HttpStatus.BAD_REQUEST, mensaje);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
