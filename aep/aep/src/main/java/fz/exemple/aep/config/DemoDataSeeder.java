package fz.exemple.aep.config;

import fz.exemple.aep.models.Doacao;
import fz.exemple.aep.models.Endereco;
import fz.exemple.aep.models.Usuario;
import fz.exemple.aep.repositories.DoacaoRepository;
import fz.exemple.aep.repositories.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Popula o banco com dados de exemplo quando a aplicacao sobe com o perfil {@code demo}.
 *
 * <p>Existe para que a tela publique e o endpoint {@code GET /api/doacoes/resumo} nao
 * apareçam vazios na primeira visita de quem vai avaliar o projeto. O seed so roda
 * quando as duas colecoes estao vazias, entao reiniciar a aplicacao nao duplica registros.</p>
 *
 * <p>O perfil {@code demo} e combinado com {@code prod} apenas no ambiente hospedado
 * ({@code SPRING_PROFILES_ACTIVE=prod,demo}). Em desenvolvimento e nos testes ele nao sobe.</p>
 */
@Component
@Profile("demo")
public class DemoDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final DoacaoRepository doacaoRepository;

    public DemoDataSeeder(UsuarioRepository usuarioRepository, DoacaoRepository doacaoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.doacaoRepository = doacaoRepository;
    }

    @Override
    public void run(String... args) {
        try {
            semear();
        } catch (RuntimeException ex) {
            // Banco fora do ar na primeira subida nao pode derrubar a aplicacao
            log.warn("Seed demo nao executado: {}", ex.getMessage());
        }
    }

    private void semear() {
        if (usuarioRepository.count() > 0 || doacaoRepository.count() > 0) {
            log.info("Seed demo ignorado: o banco ja possui dados.");
            return;
        }

        Usuario ana = usuarioRepository.save(criarUsuario(
                "Ana Silva",
                "ana.silva@exemplo.com",
                List.of(
                        criarEndereco("Rua das Acácias, 120", "Maringá", "PR"),
                        criarEndereco("Avenida Brasil, 456", "Maringá", "PR"))));

        Usuario bancoDeAlimentos = usuarioRepository.save(criarUsuario(
                "Banco de Alimentos de Maringá",
                "contato@bancodealimentos.org",
                List.of(criarEndereco("Avenida Colombo, 5790", "Maringá", "PR"))));

        LocalDate hoje = LocalDate.now();
        doacaoRepository.saveAll(List.of(
                criarDoacao(ana.getId(), "Arroz", 10, hoje.minusDays(1)),
                criarDoacao(bancoDeAlimentos.getId(), "Feijão", 5, hoje.minusDays(3)),
                criarDoacao(bancoDeAlimentos.getId(), "Leite", 8, hoje)));

        log.info("Seed demo: 2 doadores e 3 doacoes criados.");
    }

    private Usuario criarUsuario(String nome, String email, List<Endereco> enderecos) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setEnderecos(enderecos);
        return usuario;
    }

    private Endereco criarEndereco(String rua, String cidade, String estado) {
        Endereco endereco = new Endereco();
        endereco.setRua(rua);
        endereco.setCidade(cidade);
        endereco.setEstado(estado);
        return endereco;
    }

    private Doacao criarDoacao(String usuarioId, String item, int quantidade, LocalDate dataDoacao) {
        Doacao doacao = new Doacao();
        doacao.setUsuarioId(usuarioId);
        doacao.setItem(item);
        doacao.setQuantidade(quantidade);
        doacao.setDataDoacao(dataDoacao);
        return doacao;
    }
}
