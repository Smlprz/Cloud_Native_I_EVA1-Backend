package com.petshop.analytics.ws;

import com.petshop.analytics.service.MetricasService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada segundo revisa si cambiaron las metricas y, si hay dashboards conectados, les envia la foto nueva.
 * Aunque no haya cambios se refresca cada 5 s, porque "usuarios activos" y "eventos del ultimo minuto" decaen con el tiempo.
 */
@Component
public class MetricasBroadcaster {

    private static final int TICKS_ENTRE_REFRESCOS = 5;

    private final MetricasService metricas;
    private final MetricasWebSocketHandler handler;
    private final ApiGatewayWsDifusor gateway;
    private long ultimaVersion = -1;
    private int ticksSinEnviar = 0;

    public MetricasBroadcaster(MetricasService metricas, MetricasWebSocketHandler handler, ApiGatewayWsDifusor gateway) {
        this.metricas = metricas;
        this.handler = handler;
        this.gateway = gateway;
    }

    @Scheduled(fixedDelay = 1000)
    void tick() {
        if (handler.conectados() == 0 && gateway.conectados() == 0) {
            return;
        }
        long version = metricas.version();
        ticksSinEnviar++;
        if (version != ultimaVersion || ticksSinEnviar >= TICKS_ENTRE_REFRESCOS || gateway.tomarNuevas()) {
            handler.difundir();
            gateway.difundir(handler.json());
            ultimaVersion = version;
            ticksSinEnviar = 0;
        }
    }
}
