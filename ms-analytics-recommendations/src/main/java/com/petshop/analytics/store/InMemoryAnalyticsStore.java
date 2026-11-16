package com.petshop.analytics.store;

import com.petshop.analytics.event.NavegacionEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/** Almacen en memoria (analytics.store=memory). Se pierde al reiniciar el servicio. */
@Component
@ConditionalOnProperty(name = "analytics.store", havingValue = "memory", matchIfMissing = true)
public class InMemoryAnalyticsStore implements AnalyticsStore {

    /** Eventos que se recuerdan por usuario. */
    private static final int MAX_EVENTOS_POR_USUARIO = 200;

    private final Map<String, ConcurrentSkipListMap<String, NavegacionEvent>> eventos = new ConcurrentHashMap<>();
    private final Map<String, PerfilUsuario> perfiles = new ConcurrentHashMap<>();

    @Override
    public boolean guardarEvento(NavegacionEvent evento) {
        ConcurrentSkipListMap<String, NavegacionEvent> deUsuario =
                eventos.computeIfAbsent(evento.usuarioId(), u -> new ConcurrentSkipListMap<>());
        // La misma clave (instante + id) identifica al evento: un duplicado no se vuelve a guardar.
        boolean nuevo = deUsuario.putIfAbsent(evento.timestamp() + "#" + evento.eventId(), evento) == null;
        while (deUsuario.size() > MAX_EVENTOS_POR_USUARIO) {
            deUsuario.pollFirstEntry();
        }
        return nuevo;
    }

    @Override
    public List<NavegacionEvent> ultimosEventos(String usuarioId, int limite) {
        ConcurrentSkipListMap<String, NavegacionEvent> deUsuario = eventos.get(usuarioId);
        if (deUsuario == null) {
            return List.of();
        }
        List<NavegacionEvent> lista = new ArrayList<>();
        for (NavegacionEvent e : deUsuario.descendingMap().values()) {
            if (lista.size() >= limite) {
                break;
            }
            lista.add(e);
        }
        return lista;
    }

    @Override
    public PerfilUsuario obtenerPerfil(String usuarioId) {
        PerfilUsuario guardado = perfiles.get(usuarioId);
        if (guardado == null) {
            return new PerfilUsuario(usuarioId);
        }
        // Copia: quien lo modifica no altera lo guardado hasta llamar a guardarPerfil.
        PerfilUsuario copia = new PerfilUsuario(usuarioId);
        guardado.getCategorias().forEach(copia::sumarCategoria);
        List<Long> vistos = new ArrayList<>(guardado.getVistosRecientes());
        java.util.Collections.reverse(vistos);
        vistos.forEach(copia::registrarVisto);
        guardado.getComprados().forEach(copia::registrarCompra);
        copia.marcarActualizado(guardado.getActualizado());
        return copia;
    }

    @Override
    public void guardarPerfil(PerfilUsuario perfil) {
        perfiles.put(perfil.getUsuarioId(), perfil);
    }

    @Override
    public String nombre() {
        return "memoria";
    }
}
