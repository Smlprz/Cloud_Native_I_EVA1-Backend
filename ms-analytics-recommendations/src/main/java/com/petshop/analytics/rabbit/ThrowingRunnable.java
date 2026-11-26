package com.petshop.analytics.rabbit;

/** Trabajo de un consumidor: puede lanzar cualquier excepcion; {@link AckSupport} decide que hacer. */
@FunctionalInterface
public interface ThrowingRunnable {

    void run() throws Exception;
}
