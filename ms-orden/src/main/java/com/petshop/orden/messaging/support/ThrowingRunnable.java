package com.petshop.orden.messaging.support;

/** Trabajo de un consumidor: puede lanzar cualquier excepcion; {@link AckSupport} decide que hacer. */
@FunctionalInterface
public interface ThrowingRunnable {

    void run() throws Exception;
}
