package fz.exemple.aep.services;

import fz.exemple.aep.dto.DoacaoCreateRequest;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.dto.DoacaoUpdateRequest;
import fz.exemple.aep.mapper.DoacaoMapper;
import fz.exemple.aep.models.Doacao;
import fz.exemple.aep.repositories.DoacaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DoacaoService {

    private final DoacaoRepository doacaoRepository;

    public DoacaoService(DoacaoRepository doacaoRepository) {
        this.doacaoRepository = doacaoRepository;
    }

    public DoacaoResponse criar(DoacaoCreateRequest request) {
        Doacao doacao = DoacaoMapper.toEntity(request);
        Doacao saved = doacaoRepository.save(doacao);
        return DoacaoMapper.toResponse(saved);
    }

    public List<DoacaoResponse> listarTodos() {
        return DoacaoMapper.toResponseList(doacaoRepository.findAll());
    }

    public Optional<DoacaoResponse> buscarPorId(String id) {
        return doacaoRepository.findById(id)
                .map(DoacaoMapper::toResponse);
    }

    public List<DoacaoResponse> listarPorUsuario(String usuarioId) {
        return DoacaoMapper.toResponseList(doacaoRepository.findByUsuarioId(usuarioId));
    }

    public Optional<DoacaoResponse> atualizar(String id, DoacaoUpdateRequest request) {
        return doacaoRepository.findById(id)
                .map(existing -> {
                    Doacao updated = DoacaoMapper.toEntity(request, existing);
                    Doacao saved = doacaoRepository.save(updated);
                    return DoacaoMapper.toResponse(saved);
                });
    }

    public void deletar(String id) {
        doacaoRepository.deleteById(id);
    }
}