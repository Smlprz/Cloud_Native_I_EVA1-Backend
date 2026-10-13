package com.petshop.rabbitadmin.web.dto;

public record QueueInfoResponse(String name, int mensajes, int consumidores) {
}
