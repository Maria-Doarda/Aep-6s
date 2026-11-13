package fz.exemple.aep.services;

import fz.exemple.aep.dto.DoacaoCreateRequest;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.dto.DoacaoUpdateRequest;
import fz.exemple.aep.DoacaoMapper;
import fz.exemple.aep.dto.ResumoPorItemResponse;
import fz.exemple.aep.exception.RecursoNaoEncontradoException;
import fz.exemple.aep.models.Doacao;
import fz.exemple.aep.repositories.DoacaoRepository;
import fz.exemple.aep.repositories.UsuarioRepository;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.stereotype.Service;
import fz.exemple.aep.dto.ResumoResponse;

import java.util.List;
import java.util.Optional;

@Service
public class DoacaoService {

    private static final String COLECAO = "doacoes";

    private final DoacaoRepository doacaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MongoTemplate mongoTemplate;

    public DoacaoService(DoacaoRepository doacaoRepository,
                         UsuarioRepository usuarioRepository,
                         MongoTemplate mongoTemplate) {
        this.doacaoRepository = doacaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mongoTemplate = mongoTemplate;
    }

    public DoacaoResponse criar(DoacaoCreateRequest request) {
        exigirDoador(request.getUsuarioId());
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
                    exigirDoador(request.getUsuarioId());
                    Doacao updated = DoacaoMapper.toEntity(request, existing);
                    Doacao saved = doacaoRepository.save(updated);
                    return DoacaoMapper.toResponse(saved);
                });
    }

    public void deletar(String id) {
        doacaoRepository.deleteById(id);
    }

    /**
     * Resumo calculado no proprio MongoDB com aggregation pipeline ($group):
     * total de doacoes, soma das quantidades e quantidade de itens distintos
     * (via $addToSet + $size). Antes era feito em memoria com findAll().
     */
    public ResumoResponse resumo() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group()
                        .count().as("totalDoacoes")
                        .sum("quantidade").as("totalQuantidade")
                        .addToSet("item").as("itens"),
                Aggregation.project("totalDoacoes", "totalQuantidade")
                        .and(ArrayOperators.Size.lengthOfArray("itens")).as("itensDistintos")
        );

        Document resultado = mongoTemplate
                .aggregate(aggregation, COLECAO, Document.class)
                .getUniqueMappedResult();

        if (resultado == null) {
            return new ResumoResponse(0L, 0, 0L);
        }

        return new ResumoResponse(
                numero(resultado.get("totalDoacoes")).longValue(),
                numero(resultado.get("totalQuantidade")).intValue(),
                numero(resultado.get("itensDistintos")).longValue()
        );
    }

    /**
     * Agrupamento por item: soma das quantidades e numero de doacoes de cada item,
     * ordenado do maior para o menor. Tambem roda no servidor ($group + $sort).
     */
    public List<ResumoPorItemResponse> resumoPorItem() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.group("item")
                        .sum("quantidade").as("quantidadeTotal")
                        .count().as("totalDoacoes"),
                Aggregation.project("quantidadeTotal", "totalDoacoes")
                        .and("_id").as("item"),
                Aggregation.sort(Sort.Direction.DESC, "quantidadeTotal")
        );

        return mongoTemplate.aggregate(aggregation, COLECAO, Document.class)
                .getMappedResults()
                .stream()
                .map(doc -> new ResumoPorItemResponse(
                        doc.getString("item"),
                        numero(doc.get("quantidadeTotal")).intValue(),
                        numero(doc.get("totalDoacoes")).longValue()))
                .toList();
    }

    /** Doacao orfa nao entra: o usuarioId precisa existir na colecao de usuarios. */
    private void exigirDoador(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank() || !usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNaoEncontradoException("Doador nao encontrado: " + usuarioId);
        }
    }

    private static Number numero(Object valor) {
        return valor instanceof Number numero ? numero : 0;
    }
}
