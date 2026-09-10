package fz.exemple.aep.services;

import fz.exemple.aep.dto.UsuarioCreateRequest;
import fz.exemple.aep.dto.UsuarioResponse;
import fz.exemple.aep.dto.UsuarioUpdateRequest;
import fz.exemple.aep.models.Usuario;
import fz.exemple.aep.repositories.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    UsuarioRepository usuarioRepository;

    @InjectMocks
    UsuarioService usuarioService;

    @Test
    void deveCriarUsuario() {
        var request = new UsuarioCreateRequest();
        request.setNome("Ana");
        request.setEmail("ana@teste.com");

        var salvo = new Usuario();
        salvo.setId("abc123");
        salvo.setNome("Ana");
        salvo.setEmail("ana@teste.com");

        when(usuarioRepository.save(any())).thenReturn(salvo);

        UsuarioResponse response = usuarioService.criar(request);

        assertEquals("abc123", response.getId());
        assertEquals("Ana", response.getNome());
        assertEquals("ana@teste.com", response.getEmail());
    }

    @Test
    void deveListarTodosOsUsuarios() {
        var ana = new Usuario();
        ana.setNome("Ana");
        var bia = new Usuario();
        bia.setNome("Bia");

        when(usuarioRepository.findAll()).thenReturn(List.of(ana, bia));

        List<UsuarioResponse> responses = usuarioService.listarTodos();

        assertEquals(2, responses.size());
        assertEquals("Ana", responses.get(0).getNome());
        assertEquals("Bia", responses.get(1).getNome());
    }

    @Test
    void deveBuscarUsuarioPorIdQuandoExistir() {
        var usuario = new Usuario();
        usuario.setId("abc123");
        usuario.setNome("Ana");

        when(usuarioRepository.findById("abc123")).thenReturn(Optional.of(usuario));

        Optional<UsuarioResponse> response = usuarioService.buscarPorId("abc123");

        assertTrue(response.isPresent());
        assertEquals("Ana", response.get().getNome());
    }

    @Test
    void deveRetornarVazioAoBuscarUsuarioPorIdInexistente() {
        when(usuarioRepository.findById("xyz")).thenReturn(Optional.empty());

        Optional<UsuarioResponse> response = usuarioService.buscarPorId("xyz");

        assertTrue(response.isEmpty());
    }

    @Test
    void deveAtualizarUsuarioQuandoExistir() {
        var existente = new Usuario();
        existente.setId("abc123");
        existente.setNome("Nome antigo");

        var request = new UsuarioUpdateRequest();
        request.setNome("Nome novo");
        request.setEmail("novo@teste.com");

        when(usuarioRepository.findById("abc123")).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<UsuarioResponse> response = usuarioService.atualizar("abc123", request);

        assertTrue(response.isPresent());
        assertEquals("Nome novo", response.get().getNome());
        assertEquals("novo@teste.com", response.get().getEmail());
    }

    @Test
    void deveRetornarVazioAoAtualizarUsuarioInexistente() {
        var request = new UsuarioUpdateRequest();
        request.setNome("Nome novo");

        when(usuarioRepository.findById("xyz")).thenReturn(Optional.empty());

        Optional<UsuarioResponse> response = usuarioService.atualizar("xyz", request);

        assertTrue(response.isEmpty());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveDeletarUsuarioPorId() {
        usuarioService.deletar("abc123");

        verify(usuarioRepository).deleteById("abc123");
    }
}
