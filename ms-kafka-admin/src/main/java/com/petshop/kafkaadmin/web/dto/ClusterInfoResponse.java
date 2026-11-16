package com.petshop.kafkaadmin.web.dto;

import java.util.List;

/** Informacion del cluster: id, controlador y brokers disponibles. */
public record ClusterInfoResponse(String clusterId, int controller, List<Broker> brokers) {

    public record Broker(int id, String host, int port) {
    }
}
