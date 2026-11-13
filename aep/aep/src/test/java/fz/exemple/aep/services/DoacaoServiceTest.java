package fz.exemple.aep.services;

import fz.exemple.aep.dto.DoacaoCreateRequest;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.dto.DoacaoUpdateRequest;
import fz.exemple.aep.dto.ResumoPorItemResponse;
import fz.exemple.aep.dto.ResumoResponse;
import fz.exemple.aep.exception.RecursoNaoEncontradoException;
import fz.exemple.aep.models.Doacao;
import fz.exemple.aep.repositories.DoacaoRepository;
import fz.exemple.aep.repositories.UsuarioRepository;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoacaoServiceTest {

    @Mock
    DoacaoRepository doacaoRepository;

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    MongoTemplate mongoTemplate;

    @InjectMocks
    DoacaoService doacaoService;

    /** Stub do aggregation pipeline: o Mongo devolve apenas o documento final do $group. */
    private void stubAgregacao(Document... resultados) {
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("doacoes"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(resultados), new Document()));
    }

    @Test
    void deveGerarResumoComAggregationNoMongo() {
        stubAgregacao(new Document("totalDoacoes", 3)
                .append("totalQuantidade", 17)
                .append("itensDistintos", 2));

        ResumoResponse resumo = doacaoService.resumo();

        assertEquals(3, resumo.getTotalDoacoes());
        assertEquals(17, resumo.getTotalQuantidade());
        assertEquals(2, resumo.getItensDistintos());
        verify(mongoTemplate).aggregate(any(Aggregation.class), eq("doacoes"), eq(Document.class));
        verify(doacaoRepository, never()).findAll();
    }

    @Test
    void deveGerarResumoZeradoQuandoNaoExistiremDoacoes() {
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("doacoes"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(), new Document()));

        ResumoResponse resumo = doacaoService.resumo();

        assertEquals(0, resumo.getTotalDoacoes());
        assertEquals(0, resumo.getTotalQuantidade());
        assertEquals(0, resumo.getItensDistintos());
    }

    @Test
    void deveSomarQuantidadeQuandoOValorNaoForNumerico() {
        stubAgregacao(new Document("totalDoacoes", 1));

        ResumoResponse resumo = doacaoService.resumo();

        assertEquals(1, resumo.getTotalDoacoes());
        assertEquals(0, resumo.getTotalQuantidade());
        assertEquals(0, resumo.getItensDistintos());
    }

    @Test
    void deveAgruparResumoPorItem() {
        stubAgregacao(
                new Document("item", "Arroz").append("quantidadeTotal", 12).append("totalDoacoes", 2),
                new Document("item", "Feijão").append("quantidadeTotal", 5).append("totalDoacoes", 1)
        );

        List<ResumoPorItemResponse> resumo = doacaoService.resumoPorItem();

        assertEquals(2, resumo.size());
        assertEquals("Arroz", resumo.get(0).getItem());
        assertEquals(12, resumo.get(0).getQuantidadeTotal());
        assertEquals(2, resumo.get(0).getTotalDoacoes());
        assertEquals("Feijão", resumo.get(1).getItem());
        assertEquals(5, resumo.get(1).getQuantidadeTotal());
        assertEquals(1, resumo.get(1).getTotalDoacoes());
    }

    @Test
    void deveRetornarListaVaziaQuandoNaoHouverDoacoesParaAgrupar() {
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("doacoes"), eq(Document.class)))
                .thenReturn(new AggregationResults<>(List.of(), new Document()));

        List<ResumoPorItemResponse> resumo = doacaoService.resumoPorItem();

        assertTrue(resumo.isEmpty());
    }

    @Test
    void deveCriarDoacaoQuandoODoadorExistir() {
        var request = new DoacaoCreateRequest();
        request.setUsuarioId("user1");
        request.setItem("Arroz");
        request.setQuantidade(10);

        var salva = new Doacao();
        salva.setId("d1");
        salva.setUsuarioId("user1");
        salva.setItem("Arroz");
        salva.setQuantidade(10);

        when(usuarioRepository.existsById("user1")).thenReturn(true);
        when(doacaoRepository.save(any())).thenReturn(salva);

        DoacaoResponse response = doacaoService.criar(request);

        assertEquals("d1", response.getId());
        assertEquals("Arroz", response.getItem());
        assertEquals(10, response.getQuantidade());
    }

    @Test
    void deveRecusarDoacaoQuandoODoadorNaoExistir() {
        var request = new DoacaoCreateRequest();
        request.setUsuarioId("fantasma");
        request.setItem("Arroz");
        request.setQuantidade(10);

        when(usuarioRepository.existsById("fantasma")).thenReturn(false);

        var erro = assertThrows(RecursoNaoEncontradoException.class,
                () -> doacaoService.criar(request));

        assertTrue(erro.getMessage().contains("fantasma"));
        verify(doacaoRepository, never()).save(any());
    }

    @Test
    void deveRecusarDoacaoComUsuarioIdEmBranco() {
        var request = new DoacaoCreateRequest();
        request.setUsuarioId(" ");
        request.setItem("Arroz");
        request.setQuantidade(10);

        assertThrows(RecursoNaoEncontradoException.class, () -> doacaoService.criar(request));
        verifyNoInteractions(usuarioRepository);
        verify(doacaoRepository, never()).save(any());
    }

    @Test
    void deveRecusarDoacaoSemUsuarioId() {
        var request = new DoacaoCreateRequest();
        request.setUsuarioId(null);
        request.setItem("Arroz");
        request.setQuantidade(10);

        assertThrows(RecursoNaoEncontradoException.class, () -> doacaoService.criar(request));
        verifyNoInteractions(usuarioRepository);
        verify(doacaoRepository, never()).save(any());
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
        when(usuarioRepository.existsById("user1")).thenReturn(true);
        when(doacaoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<DoacaoResponse> response = doacaoService.atualizar("d1", request);

        assertTrue(response.isPresent());
        assertEquals("Feijão", response.get().getItem());
        assertEquals(5, response.get().getQuantidade());
    }

    @Test
    void deveRecusarAtualizacaoQuandoODoadorNaoExistir() {
        var existente = new Doacao();
        existente.setId("d1");
        existente.setItem("Arroz");

        var request = new DoacaoUpdateRequest();
        request.setUsuarioId("fantasma");
        request.setItem("Feijão");
        request.setQuantidade(5);

        when(doacaoRepository.findById("d1")).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsById("fantasma")).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> doacaoService.atualizar("d1", request));
        verify(doacaoRepository, never()).save(any());
    }

    @Test
    void deveRetornarVazioAoAtualizarDoacaoInexistente() {
        var request = new DoacaoUpdateRequest();
        request.setItem("Feijão");

        when(doacaoRepository.findById("xyz")).thenReturn(Optional.empty());

        Optional<DoacaoResponse> response = doacaoService.atualizar("xyz", request);

        assertTrue(response.isEmpty());
        verify(doacaoRepository, never()).save(any());
        verify(usuarioRepository, never()).existsById(anyString());
    }

    @Test
    void deveDeletarDoacaoPorId() {
        doacaoService.deletar("d1");

        verify(doacaoRepository).deleteById("d1");
    }
}
