package com.petshop.analytics.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Foto de las metricas en tiempo real que ve el dashboard (por REST y por WebSocket).
 *
 * totalEventos         eventos de navegacion recibidos (clics)
 * eventosUltimoMinuto  ritmo actual de actividad
 * eventosPorTipo       cuantos eventos hay de cada tipo (vistas, agregados al carrito, compras...)
 * productosMasVistos   top 5 de productos por visitas
 * usuariosActivos      usuarios con actividad en los ultimos minutos
 * pagos                pagos aprobados, rechazados y bloqueados por fraude
 * alertasFraude        ultimas alertas del antifraude de ms-pago
 * mensajesDescartados  mensajes invalidos que se saltaron sin detener el consumo (quedan en el log)
 */
public record MetricasSnapshot(
        long totalEventos,
        long eventosUltimoMinuto,
        Map<String, Long> eventosPorTipo,
        List<ProductoVisto> productosMasVistos,
        int usuariosActivos,
        Pagos pagos,
        List<AlertaFraude> alertasFraude,
        long mensajesDescartados,
        Instant actualizado
) {

    public record ProductoVisto(Long productoId, String nombre, long vistas) {
    }

    public record Pagos(long aprobados, long rechazados, long bloqueadosPorFraude) {
    }

    public record AlertaFraude(Long ordenId, String tarjetaUltimos4, String motivo, Instant momento) {
    }
}
