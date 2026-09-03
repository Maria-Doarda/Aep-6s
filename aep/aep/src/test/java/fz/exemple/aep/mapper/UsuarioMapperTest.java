package fz.exemple.aep.mapper;

import fz.exemple.aep.UsuarioMapper;
import fz.exemple.aep.dto.EnderecoDTO;
import fz.exemple.aep.dto.UsuarioCreateRequest;
import fz.exemple.aep.dto.UsuarioUpdateRequest;
import fz.exemple.aep.models.Endereco;
import fz.exemple.aep.models.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioMapperTest {

    @Test
    void deveMapearUsuarioComEndereco() {
        var request = new UsuarioCreateRequest();
        request.setNome("Ana");
        request.setEmail("ana@teste.com");
        request.setEnderecos(List.of(
                new EnderecoDTO("Rua A, 123", "Maringá", "PR")
        ));

        Usuario usuario = UsuarioMapper.toEntity(request);

        assertEquals("Ana", usuario.getNome());
        assertEquals("ana@teste.com", usuario.getEmail());
        assertEquals(1, usuario.getEnderecos().size());
        assertEquals("Rua A, 123", usuario.getEnderecos().get(0).getRua());
    }

    @Test
    void deveCriarListaVaziaQuandoEnderecosForNulo() {
        var request = new UsuarioCreateRequest();
        request.setNome("Ana");
        request.setEmail("ana@teste.com");
        request.setEnderecos(null);

        Usuario usuario = UsuarioMapper.toEntity(request);

        assertNotNull(usuario.getEnderecos());
        assertTrue(usuario.getEnderecos().isEmpty());
    }

    @Test
    void deveAtualizarUsuarioELimparEnderecosQuandoForNulo() {
        var usuarioExistente = new Usuario();
        usuarioExistente.setNome("Nome antigo");
        usuarioExistente.setEnderecos(List.of(new Endereco()));

        var request = new UsuarioUpdateRequest();
        request.setNome("Nome novo");
        request.setEmail("novo@teste.com");
        request.setEnderecos(null);

        Usuario usuarioAtualizado = UsuarioMapper.toEntity(request, usuarioExistente);

        assertEquals("Nome novo", usuarioAtualizado.getNome());
        assertEquals("novo@teste.com", usuarioAtualizado.getEmail());
        assertTrue(usuarioAtualizado.getEnderecos().isEmpty());
    }
}