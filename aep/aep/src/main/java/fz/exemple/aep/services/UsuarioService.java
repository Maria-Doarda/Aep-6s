package fz.exemple.aep.services;

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

    public Usuario criar(Usuario usuario){
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarTodos(){
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> buscarPorId(String id){
        return usuarioRepository.findById(id);
    }

    public Usuario atualizar(String id, Usuario dadosAtualizados){
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        usuario.setNome(dadosAtualizados.getNome());
        usuario.setEmail(dadosAtualizados.getEmail());
        usuario.setEnderecos(dadosAtualizados.getEnderecos());
        return usuarioRepository.save(usuario);

    }

    public void deletar(String id){
        usuarioRepository.deleteById(id);
    }
}
