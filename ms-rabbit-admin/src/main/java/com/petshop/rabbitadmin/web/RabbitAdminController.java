package com.petshop.rabbitadmin.web;

import com.petshop.rabbitadmin.service.RabbitAdminService;
import com.petshop.rabbitadmin.web.dto.BindingRequest;
import com.petshop.rabbitadmin.web.dto.ExchangeRequest;
import com.petshop.rabbitadmin.web.dto.QueueInfoResponse;
import com.petshop.rabbitadmin.web.dto.QueueRequest;
import jakarta.validation.Valid;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API de administracion de RabbitMQ. Exige JWT de Azure AD con rol ADMIN y scope rabbit.admin
 * (o carrito.write mientras ese scope no exista en el App Registration).
 */
@RestController
@RequestMapping("/api/rabbit")
@PreAuthorize("hasRole('ADMIN') and hasAnyAuthority('SCOPE_rabbit.admin','SCOPE_carrito.write')")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping("/exchanges")
    @ResponseStatus(HttpStatus.CREATED)
    public void crearExchange(@Valid @RequestBody ExchangeRequest request) {
        service.crearExchange(request);
    }

    @DeleteMapping("/exchanges/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
    }

    @PostMapping("/queues")
    @ResponseStatus(HttpStatus.CREATED)
    public void crearCola(@Valid @RequestBody QueueRequest request) {
        service.crearCola(request);
    }

    @GetMapping("/queues/{name}")
    public QueueInfoResponse infoCola(@PathVariable String name) {
        return service.infoCola(name);
    }

    @DeleteMapping("/queues/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCola(@PathVariable String name) {
        service.eliminarCola(name);
    }

    @PostMapping("/bindings")
    @ResponseStatus(HttpStatus.CREATED)
    public void crearBinding(@Valid @RequestBody BindingRequest request) {
        service.crearBinding(request);
    }

    @DeleteMapping("/bindings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBinding(@Valid @RequestBody BindingRequest request) {
        service.eliminarBinding(request);
    }

    @ExceptionHandler(AmqpException.class)
    public void rabbitNoDisponible(AmqpException e) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "RabbitMQ no disponible: " + e.getMessage());
    }
}
