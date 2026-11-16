package com.petshop.analytics.store;

import com.petshop.analytics.event.NavegacionEvent;

import java.util.List;

/**
 * Persistencia del historial de eventos y de los perfiles de navegacion. Dos implementaciones:
 * en memoria (desarrollo y pruebas) y Amazon DynamoDB (nube), elegidas con analytics.store.
 */
public interface AnalyticsStore {

    /**
     * Guarda el evento en el historial. Devuelve false si ya estaba (mensaje duplicado: Kafka entrega
     * "al menos una vez"), y entonces el llamador no debe volver a sumarlo al perfil.
     */
    boolean guardarEvento(NavegacionEvent evento);

    /** Ultimos eventos del usuario, el mas reciente primero. */
    List<NavegacionEvent> ultimosEventos(String usuarioId, int limite);

    /** Perfil del usuario (vacio si todavia no tiene). */
    PerfilUsuario obtenerPerfil(String usuarioId);

    void guardarPerfil(PerfilUsuario perfil);

    /** Nombre del almacen, para los logs y el endpoint de informacion. */
    String nombre();
}
