package fz.exemple.aep.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DTOs de resposta - construtores, getters e setters")
class ResponseDtosTest {

    @Test
    @DisplayName("UsuarioResponse mantem os valores do construtor")
    void usuarioResponse_construtor() {
        EnderecoDTO endereco = new EnderecoDTO("Rua A, 123", "Maringa", "PR");
        UsuarioResponse response = new UsuarioResponse("u1", "Ana", "ana@teste.com", List.of(endereco));

        assertEquals("u1", response.getId());
        assertEquals("Ana", response.getNome());
        assertEquals("ana@teste.com", response.getEmail());
        assertEquals(1, response.getEnderecos().size());
        assertEquals("Rua A, 123", response.getEnderecos().get(0).getRua());
    }

    @Test
    @DisplayName("UsuarioResponse aceita setters")
    void usuarioResponse_setters() {
        UsuarioResponse response = new UsuarioResponse();
        response.setId("u2");
        response.setNome("Bruno");
        response.setEmail("bruno@teste.com");
        response.setEnderecos(List.of());

        assertEquals("u2", response.getId());
        assertEquals("Bruno", response.getNome());
        assertEquals("bruno@teste.com", response.getEmail());
        assertEquals(0, response.getEnderecos().size());
    }

    @Test
    @DisplayName("DoacaoResponse mantem os valores do construtor")
    void doacaoResponse_construtor() {
        DoacaoResponse response = new DoacaoResponse("d1", "u1", "Arroz", 10, LocalDate.of(2026, 9, 2));

        assertEquals("d1", response.getId());
        assertEquals("u1", response.getUsuarioId());
        assertEquals("Arroz", response.getItem());
        assertEquals(10, response.getQuantidade());
        assertEquals(LocalDate.of(2026, 9, 2), response.getDataDoacao());
    }

    @Test
    @DisplayName("DoacaoResponse aceita setters")
    void doacaoResponse_setters() {
        DoacaoResponse response = new DoacaoResponse();
        response.setId("d2");
        response.setUsuarioId("u2");
        response.setItem("Feijao");
        response.setQuantidade(5);
        response.setDataDoacao(LocalDate.of(2026, 1, 1));

        assertEquals("d2", response.getId());
        assertEquals("u2", response.getUsuarioId());
        assertEquals("Feijao", response.getItem());
        assertEquals(5, response.getQuantidade());
        assertEquals(LocalDate.of(2026, 1, 1), response.getDataDoacao());
    }

    @Test
    @DisplayName("ResumoPorItemResponse mantem os valores do construtor")
    void resumoPorItemResponse_construtor() {
        ResumoPorItemResponse response = new ResumoPorItemResponse("Arroz", 12, 2L);

        assertEquals("Arroz", response.getItem());
        assertEquals(12, response.getQuantidadeTotal());
        assertEquals(2L, response.getTotalDoacoes());
    }

    @Test
    @DisplayName("ResumoPorItemResponse aceita setters")
    void resumoPorItemResponse_setters() {
        ResumoPorItemResponse response = new ResumoPorItemResponse();
        response.setItem("Feijao");
        response.setQuantidadeTotal(5);
        response.setTotalDoacoes(1L);

        assertEquals("Feijao", response.getItem());
        assertEquals(5, response.getQuantidadeTotal());
        assertEquals(1L, response.getTotalDoacoes());
    }
}
