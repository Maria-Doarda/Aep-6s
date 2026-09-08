package fz.exemple.aep.mapper;

import fz.exemple.aep.UsuarioMapper;
import fz.exemple.aep.dto.EnderecoDTO;
import fz.exemple.aep.dto.UsuarioCreateRequest;
import fz.exemple.aep.dto.UsuarioResponse;
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

    @Test
    void deveAtualizarUsuarioComNovosEnderecos() {
        var usuarioExistente = new Usuario();
        usuarioExistente.setNome("Nome antigo");
        usuarioExistente.setEnderecos(List.of(new Endereco()));

        var request = new UsuarioUpdateRequest();
        request.setNome("Nome novo");
        request.setEmail("novo@teste.com");
        request.setEnderecos(List.of(new EnderecoDTO("Rua B, 456", "Curitiba", "PR")));

        Usuario usuarioAtualizado = UsuarioMapper.toEntity(request, usuarioExistente);

        assertEquals(1, usuarioAtualizado.getEnderecos().size());
        assertEquals("Rua B, 456", usuarioAtualizado.getEnderecos().get(0).getRua());
    }

    @Test
    void deveConverterUsuarioParaResponseComEnderecos() {
        var usuario = new Usuario();
        usuario.setId("abc123");
        usuario.setNome("Ana");
        usuario.setEmail("ana@teste.com");
        var endereco = new Endereco();
        endereco.setRua("Rua A, 123");
        endereco.setCidade("Maringá");
        endereco.setEstado("PR");
        usuario.setEnderecos(List.of(endereco));

        UsuarioResponse response = UsuarioMapper.toResponse(usuario);

        assertEquals("abc123", response.getId());
        assertEquals("Ana", response.getNome());
        assertEquals("ana@teste.com", response.getEmail());
        assertEquals(1, response.getEnderecos().size());
        assertEquals("Rua A, 123", response.getEnderecos().get(0).getRua());
    }

    @Test
    void deveConverterUsuarioParaResponseComListaVaziaQuandoEnderecosForNulo() {
        var usuario = new Usuario();
        usuario.setNome("Ana");
        usuario.setEmail("ana@teste.com");
        usuario.setEnderecos(null);

        UsuarioResponse response = UsuarioMapper.toResponse(usuario);

        assertNotNull(response.getEnderecos());
        assertTrue(response.getEnderecos().isEmpty());
    }

    @Test
    void deveConverterListaDeUsuariosParaListaDeResponses() {
        var usuario1 = new Usuario();
        usuario1.setNome("Ana");
        var usuario2 = new Usuario();
        usuario2.setNome("Bia");

        List<UsuarioResponse> responses = UsuarioMapper.toResponseList(List.of(usuario1, usuario2));

        assertEquals(2, responses.size());
        assertEquals("Ana", responses.get(0).getNome());
        assertEquals("Bia", responses.get(1).getNome());
    }

    @Test
    void deveConverterListaVaziaDeUsuariosParaListaVaziaDeResponses() {
        List<UsuarioResponse> responses = UsuarioMapper.toResponseList(List.of());

        assertTrue(responses.isEmpty());
    }

    @Test
    void deveConverterEnderecoParaDTO() {
        var endereco = new Endereco();
        endereco.setRua("Rua A, 123");
        endereco.setCidade("Maringá");
        endereco.setEstado("PR");

        EnderecoDTO dto = UsuarioMapper.toEnderecoDTO(endereco);

        assertEquals("Rua A, 123", dto.getRua());
        assertEquals("Maringá", dto.getCidade());
        assertEquals("PR", dto.getEstado());
    }

    @Test
    void deveRetornarNuloAoConverterEnderecoNuloParaDTO() {
        assertNull(UsuarioMapper.toEnderecoDTO(null));
    }
}