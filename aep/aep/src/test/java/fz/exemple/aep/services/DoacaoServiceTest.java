package fz.exemple.aep.services;

import fz.exemple.aep.dto.DoacaoCreateRequest;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.dto.DoacaoUpdateRequest;
import fz.exemple.aep.models.Doacao;
import fz.exemple.aep.repositories.DoacaoRepository;
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
class DoacaoServiceTest {

    @Mock
    DoacaoRepository doacaoRepository;

    @InjectMocks
    DoacaoService doacaoService;

    @Test
    void deveGerarResumoDasDoacoes() {
        var arroz = new Doacao();
        arroz.setItem("Arroz");
        arroz.setQuantidade(10);

        var feijao = new Doacao();
        feijao.setItem("Feijão");
        feijao.setQuantidade(5);

        var maisArroz = new Doacao();
        maisArroz.setItem("Arroz");
        maisArroz.setQuantidade(2);

        when(doacaoRepository.findAll())
                .thenReturn(List.of(arroz, feijao, maisArroz));

        var resumo = doacaoService.resumo();

        assertEquals(3, resumo.getTotalDoacoes());
        assertEquals(17, resumo.getTotalQuantidade());
        assertEquals(2, resumo.getItensDistintos());
    }

    @Test
    void deveGerarResumoZeradoQuandoNaoExistiremDoacoes() {
        when(doacaoRepository.findAll()).thenReturn(List.of());

        var resumo = doacaoService.resumo();

        assertEquals(0, resumo.getTotalDoacoes());
        assertEquals(0, resumo.getTotalQuantidade());
        assertEquals(0, resumo.getItensDistintos());
    }

    @Test
    void deveCriarDoacao() {
        var request = new DoacaoCreateRequest();
        request.setUsuarioId("user1");
        request.setItem("Arroz");
        request.setQuantidade(10);

        var salva = new Doacao();
        salva.setId("d1");
        salva.setUsuarioId("user1");
        salva.setItem("Arroz");
        salva.setQuantidade(10);

        when(doacaoRepository.save(any())).thenReturn(salva);

        DoacaoResponse response = doacaoService.criar(request);

        assertEquals("d1", response.getId());
        assertEquals("Arroz", response.getItem());
        assertEquals(10, response.getQuantidade());
    }

    @Test
    void deveListarTodasAsDoacoes() {
        var arroz = new Doacao();
        arroz.setItem("Arroz");
        var feijao = new Doacao();
        feijao.setItem("Feijão");

        when(doacaoRepository.findAll()).thenReturn(List.of(arroz, feijao));

        List<DoacaoResponse> responses = doacaoService.listarTodos();

        assertEquals(2, responses.size());
    }

    @Test
    void deveBuscarDoacaoPorIdQuandoExistir() {
        var doacao = new Doacao();
        doacao.setId("d1");
        doacao.setItem("Arroz");

        when(doacaoRepository.findById("d1")).thenReturn(Optional.of(doacao));

        Optional<DoacaoResponse> response = doacaoService.buscarPorId("d1");

        assertTrue(response.isPresent());
        assertEquals("Arroz", response.get().getItem());
    }

    @Test
    void deveRetornarVazioAoBuscarDoacaoPorIdInexistente() {
        when(doacaoRepository.findById("xyz")).thenReturn(Optional.empty());

        Optional<DoacaoResponse> response = doacaoService.buscarPorId("xyz");

        assertTrue(response.isEmpty());
    }

    @Test
    void deveListarDoacoesPorUsuario() {
        var doacao = new Doacao();
        doacao.setUsuarioId("user1");
        doacao.setItem("Arroz");

        when(doacaoRepository.findByUsuarioId("user1")).thenReturn(List.of(doacao));

        List<DoacaoResponse> responses = doacaoService.listarPorUsuario("user1");

        assertEquals(1, responses.size());
        assertEquals("Arroz", responses.get(0).getItem());
    }

    @Test
    void deveAtualizarDoacaoQuandoExistir() {
        var existente = new Doacao();
        existente.setId("d1");
        existente.setItem("Arroz");
        existente.setQuantidade(10);

        var request = new DoacaoUpdateRequest();
        request.setUsuarioId("user1");
        request.setItem("Feijão");
        request.setQuantidade(5);

        when(doacaoRepository.findById("d1")).thenReturn(Optional.of(existente));
        when(doacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<DoacaoResponse> response = doacaoService.atualizar("d1", request);

        assertTrue(response.isPresent());
        assertEquals("Feijão", response.get().getItem());
        assertEquals(5, response.get().getQuantidade());
    }

    @Test
    void deveRetornarVazioAoAtualizarDoacaoInexistente() {
        var request = new DoacaoUpdateRequest();
        request.setItem("Feijão");

        when(doacaoRepository.findById("xyz")).thenReturn(Optional.empty());

        Optional<DoacaoResponse> response = doacaoService.atualizar("xyz", request);

        assertTrue(response.isEmpty());
        verify(doacaoRepository, never()).save(any());
    }

    @Test
    void deveDeletarDoacaoPorId() {
        doacaoService.deletar("d1");

        verify(doacaoRepository).deleteById("d1");
    }
}