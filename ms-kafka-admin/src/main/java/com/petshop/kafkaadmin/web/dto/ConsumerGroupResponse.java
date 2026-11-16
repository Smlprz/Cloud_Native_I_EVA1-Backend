package com.petshop.kafkaadmin.web.dto;

import java.util.List;

/**
 * Estado de un consumer group: miembros y, por cada particion, el offset confirmado, el offset final y el lag
 * (mensajes pendientes = endOffset - currentOffset).
 */
public record ConsumerGroupResponse(
        String groupId,
        String state,
        int members,
        long lagTotal,
        List<PartitionLag> partitions
) {

    public record PartitionLag(String topic, int partition, long currentOffset, long endOffset, long lag) {
    }
}
