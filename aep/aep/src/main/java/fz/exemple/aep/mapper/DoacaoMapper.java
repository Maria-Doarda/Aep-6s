package fz.exemple.aep.mapper;

import fz.exemple.aep.dto.DoacaoCreateRequest;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.dto.DoacaoUpdateRequest;
import fz.exemple.aep.models.Doacao;

import java.util.ArrayList;
import java.util.List;

public final class DoacaoMapper {

    private DoacaoMapper() {}

    public static Doacao toEntity(DoacaoCreateRequest request) {
        Doacao doacao = new Doacao();
        doacao.setUsuarioId(request.getUsuarioId());
        doacao.setItem(request.getItem());
        doacao.setQuantidade(request.getQuantidade());
        doacao.setDataDoacao(request.getDataDoacao());
        return doacao;
    }

    public static Doacao toEntity(DoacaoUpdateRequest request, Doacao existing) {
        existing.setUsuarioId(request.getUsuarioId());
        existing.setItem(request.getItem());
        existing.setQuantidade(request.getQuantidade());
        existing.setDataDoacao(request.getDataDoacao());
        return existing;
    }

    public static DoacaoResponse toResponse(Doacao doacao) {
        return new DoacaoResponse(doacao.getId(), doacao.getUsuarioId(), doacao.getItem(), doacao.getQuantidade(), doacao.getDataDoacao());
    }

    public static List<DoacaoResponse> toResponseList(List<Doacao> doacaoList) {
        List<DoacaoResponse> responses = new ArrayList<>();
        for (Doacao d : doacaoList) {
            responses.add(toResponse(d));
        }
        return responses;
    }
}