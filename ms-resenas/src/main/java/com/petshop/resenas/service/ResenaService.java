package com.petshop.resenas.service;

import com.petshop.resenas.domain.Resena;
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

    public ResenaService(ResenaRepository repository) {
        this.repository = repository;
    }

    public List<ResenaResponse> listarPorProducto(Long productoId) {
        return repository.findByProductoIdOrderByCreadoEnDesc(productoId).stream()
                .map(ResenaResponse::from)
                .toList();
    }

    public ResenaResponse crear(String usuarioId, String usuarioNombre, ResenaRequest request) {
        Resena resena = new Resena(request.productoId(), usuarioId, usuarioNombre,
                request.puntaje(), request.comentario());
        return ResenaResponse.from(repository.save(resena));
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
