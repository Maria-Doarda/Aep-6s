package fz.exemple.aep.mapper;

import fz.exemple.aep.DoacaoMapper;
import fz.exemple.aep.dto.DoacaoCreateRequest;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.dto.DoacaoUpdateRequest;
import fz.exemple.aep.models.Doacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DoacaoMapperTest {

    @Test
    void deveMapearCreateRequestParaEntidade() {
        var request = new DoacaoCreateRequest();
        request.setUsuarioId("user1");
        request.setItem("Arroz");
        request.setQuantidade(10);
        request.setDataDoacao(LocalDate.of(2026, 1, 10));

        Doacao doacao = DoacaoMapper.toEntity(request);

        assertEquals("user1", doacao.getUsuarioId());
        assertEquals("Arroz", doacao.getItem());
        assertEquals(10, doacao.getQuantidade());
        assertEquals(LocalDate.of(2026, 1, 10), doacao.getDataDoacao());
    }

    @Test
    void deveAtualizarEntidadeExistenteComUpdateRequest() {
        var doacaoExistente = new Doacao();
        doacaoExistente.setId("d1");
        doacaoExistente.setUsuarioId("user1");
        doacaoExistente.setItem("Arroz");
        doacaoExistente.setQuantidade(10);

        var request = new DoacaoUpdateRequest();
        request.setUsuarioId("user2");
        request.setItem("Feijão");
        request.setQuantidade(5);
        request.setDataDoacao(LocalDate.of(2026, 2, 20));

        Doacao doacaoAtualizada = DoacaoMapper.toEntity(request, doacaoExistente);

        assertEquals("d1", doacaoAtualizada.getId());
        assertEquals("user2", doacaoAtualizada.getUsuarioId());
        assertEquals("Feijão", doacaoAtualizada.getItem());
        assertEquals(5, doacaoAtualizada.getQuantidade());
        assertEquals(LocalDate.of(2026, 2, 20), doacaoAtualizada.getDataDoacao());
    }

    @Test
    void deveConverterEntidadeParaResponse() {
        var doacao = new Doacao();
        doacao.setId("d1");
        doacao.setUsuarioId("user1");
        doacao.setItem("Arroz");
        doacao.setQuantidade(10);
        doacao.setDataDoacao(LocalDate.of(2026, 1, 10));

        DoacaoResponse response = DoacaoMapper.toResponse(doacao);

        assertEquals("d1", response.getId());
        assertEquals("user1", response.getUsuarioId());
        assertEquals("Arroz", response.getItem());
        assertEquals(10, response.getQuantidade());
        assertEquals(LocalDate.of(2026, 1, 10), response.getDataDoacao());
    }

    @Test
    void deveConverterListaDeEntidadesParaListaDeResponses() {
        var arroz = new Doacao();
        arroz.setItem("Arroz");
        var feijao = new Doacao();
        feijao.setItem("Feijão");

        List<DoacaoResponse> responses = DoacaoMapper.toResponseList(List.of(arroz, feijao));

        assertEquals(2, responses.size());
        assertEquals("Arroz", responses.get(0).getItem());
        assertEquals("Feijão", responses.get(1).getItem());
    }

    @Test
    void deveConverterListaVaziaDeEntidadesParaListaVaziaDeResponses() {
        List<DoacaoResponse> responses = DoacaoMapper.toResponseList(List.of());

        assertTrue(responses.isEmpty());
    }
}
