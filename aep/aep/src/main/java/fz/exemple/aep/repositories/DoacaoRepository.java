package fz.exemple.aep.repositories;

import fz.exemple.aep.models.Doacao;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DoacaoRepository extends MongoRepository<Doacao, String> {
    List<Doacao> findByUsuarioId(String usuarioId);
}
