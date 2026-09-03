package fz.exemple.aep.services;

import fz.exemple.aep.models.Doacao;
import fz.exemple.aep.repositories.DoacaoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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
}