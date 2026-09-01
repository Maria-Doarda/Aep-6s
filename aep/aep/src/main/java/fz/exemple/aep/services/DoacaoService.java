package fz.exemple.aep.services;

import fz.exemple.aep.models.Doacao;
import fz.exemple.aep.models.Usuario;
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

    public Doacao criar(Doacao doacao){
        return doacaoRepository.save(doacao);
    }

    public List<Doacao> listarTodos(){
        return doacaoRepository.findAll();
    }

    public Optional<Doacao> buscarPorId(String id){
        return doacaoRepository.findById(id);
    }

    public List<Doacao> listarPorUsuario(String usuarioId) {
        return doacaoRepository.findByUsuarioId(usuarioId);
    }

    public Doacao atualizar(String id, Doacao dadosAtualizados) {
        Doacao doacao = doacaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doação não encontrada"));

        doacao.setItem(dadosAtualizados.getItem());
        doacao.setQuantidade(dadosAtualizados.getQuantidade());
        doacao.setData_doacao(dadosAtualizados.getData_doacao());
        doacao.setUsuarioId(dadosAtualizados.getUsuarioId());

        return doacaoRepository.save(doacao);
    }

    public void deletar(String id){
        doacaoRepository.deleteById(id);
    }
}
