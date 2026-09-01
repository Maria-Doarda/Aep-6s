package fz.exemple.aep.controllers;

import fz.exemple.aep.models.Doacao;
import fz.exemple.aep.models.Usuario;
import fz.exemple.aep.services.DoacaoService;
import fz.exemple.aep.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doacoes")
public class DoacaoController {
    private final DoacaoService doacaoService;

    public DoacaoController(DoacaoService doacaoService) {
        this.doacaoService = doacaoService;
    }

    @PostMapping
    public ResponseEntity<Doacao> criar(@RequestBody Doacao doacao){
        return ResponseEntity.ok(doacaoService.criar(doacao));
    }

    @GetMapping
    public ResponseEntity<List<Doacao>> listarTodos(){
        return ResponseEntity.ok(doacaoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doacao> buscarPorId(@PathVariable String id){
        return doacaoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Doacao> atualizar(@PathVariable String id, @RequestBody Doacao doacao){
        return ResponseEntity.ok(doacaoService.atualizar(id, doacao));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable String id){
        doacaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
