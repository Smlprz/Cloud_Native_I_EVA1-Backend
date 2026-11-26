package com.petshop.resenas.service;

import com.petshop.resenas.domain.Resena;
import com.petshop.resenas.messaging.ResenaCreadaEvent;
import com.petshop.resenas.messaging.ResenaPublisher;
import com.petshop.resenas.repository.ResenaRepository;
import com.petshop.resenas.web.dto.ResenaRequest;
import com.petshop.resenas.web.dto.ResenaResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ResenaService {

    private final ResenaRepository repository;
    private final ResenaPublisher publisher;

    public ResenaService(ResenaRepository repository, ResenaPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public List<ResenaResponse> listarPorProducto(Long productoId) {
        return repository.findByProductoIdOrderByCreadoEnDesc(productoId).stream()
                .map(ResenaResponse::from)
                .toList();
    }

    public ResenaResponse crear(String usuarioId, String usuarioNombre, ResenaRequest request) {
        Resena resena = new Resena(request.productoId(), usuarioId, usuarioNombre,
                request.puntaje(), request.comentario());
        Resena guardada = repository.save(resena);
        publisher.publicar(new ResenaCreadaEvent(guardada.getId(), guardada.getProductoId(), guardada.getPuntaje()));
        return ResenaResponse.from(guardada);
    }

    /** Solo el autor puede borrar su resena. */
    public void eliminar(Long id, String usuarioId) {
        Resena resena = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La resena no existe"));
        if (!resena.getUsuarioId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes borrar la resena de otro usuario");
        }
        repository.delete(resena);
    }
}
