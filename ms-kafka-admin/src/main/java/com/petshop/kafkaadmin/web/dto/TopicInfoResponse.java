package com.petshop.kafkaadmin.web.dto;

import java.util.List;
import java.util.Map;

/** Descripcion de un topico: particiones, replicas, ISR y configuraciones. */
public record TopicInfoResponse(
        String name,
        int partitions,
        int replicationFactor,
        boolean internal,
        Map<String, String> config,
        List<PartitionInfo> partitionsDetail
) {

    /** leader = id del broker lider; replicas / isr = ids de brokers (isr: replicas sincronizadas). */
    public record PartitionInfo(int partition, int leader, List<Integer> replicas, List<Integer> isr) {
    }
}
