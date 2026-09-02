package fz.exemple.aep.services;

import fz.exemple.aep.dto.UsuarioCreateRequest;
import fz.exemple.aep.dto.UsuarioResponse;
import fz.exemple.aep.dto.UsuarioUpdateRequest;
import fz.exemple.aep.mapper.UsuarioMapper;
import fz.exemple.aep.models.Usuario;
import fz.exemple.aep.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public UsuarioResponse criar(UsuarioCreateRequest request) {
        Usuario usuario = UsuarioMapper.toEntity(request);
        Usuario saved = usuarioRepository.save(usuario);
        return UsuarioMapper.toResponse(saved);
    }

    public List<UsuarioResponse> listarTodos() {
        return UsuarioMapper.toResponseList(usuarioRepository.findAll());
    }

    public Optional<UsuarioResponse> buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .map(UsuarioMapper::toResponse);
    }

    public Optional<UsuarioResponse> atualizar(String id, UsuarioUpdateRequest request) {
        return usuarioRepository.findById(id)
                .map(existing -> {
                    Usuario updated = UsuarioMapper.toEntity(request, existing);
                    Usuario saved = usuarioRepository.save(updated);
                    return UsuarioMapper.toResponse(saved);
                });
    }

    public void deletar(String id) {
        usuarioRepository.deleteById(id);
    }
}