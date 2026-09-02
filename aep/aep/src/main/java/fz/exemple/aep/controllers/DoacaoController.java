package fz.exemple.aep.controllers;

import fz.exemple.aep.dto.DoacaoCreateRequest;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.dto.DoacaoUpdateRequest;
import fz.exemple.aep.services.DoacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/doacoes")
public class DoacaoController {
    private final DoacaoService doacaoService;

    public DoacaoController(DoacaoService doacaoService) {
        this.doacaoService = doacaoService;
    }

    @PostMapping
    public ResponseEntity<DoacaoResponse> criar(@Valid @RequestBody DoacaoCreateRequest request) {
        DoacaoResponse response = doacaoService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DoacaoResponse>> listarTodos() {
        return ResponseEntity.ok(doacaoService.listarTodos());
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<DoacaoResponse>> listarPorUsuario(@PathVariable String usuarioId) {
        return ResponseEntity.ok(doacaoService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoacaoResponse> buscarPorId(@PathVariable String id) {
        return doacaoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoacaoResponse> atualizar(@PathVariable String id, @Valid @RequestBody DoacaoUpdateRequest request) {
        return doacaoService.atualizar(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id) {
        doacaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}