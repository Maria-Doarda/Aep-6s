package fz.exemple.aep.cli;

import fz.exemple.aep.dto.DoacaoCreateRequest;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.dto.EnderecoDTO;
import fz.exemple.aep.dto.UsuarioCreateRequest;
import fz.exemple.aep.dto.UsuarioResponse;
import fz.exemple.aep.services.DoacaoService;
import fz.exemple.aep.services.UsuarioService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

@Component
@Profile("!test")
public class CadastroCli implements CommandLineRunner {

    private final UsuarioService usuarioService;
    private final DoacaoService doacaoService;
    private final Validator validator;

    private Scanner scanner;
    private PrintStream out = System.out;

    public CadastroCli(UsuarioService usuarioService, DoacaoService doacaoService, Validator validator) {
        this.usuarioService = usuarioService;
        this.doacaoService = doacaoService;
        this.validator = validator;
    }

    /** Injeta a entrada do usuário; em produção o menu usa System.in. */
    void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }

    /** Injeta a saída do menu; em produção o menu usa System.out. */
    void setOut(PrintStream out) {
        this.out = out;
    }

    @Override
    public void run(String... args) {
        if (scanner == null) {
            scanner = new Scanner(System.in);
        }
        out.println();
        out.println("=== AEP6S terminal (API continua em http://localhost:8081) ===");
        try {
            while (true) {
                imprimirMenu();
                String opcao = lerLinha("> ");
                switch (opcao.trim()) {
                    case "1" -> cadastrarUsuario();
                    case "2" -> cadastrarDoacao();
                    case "3" -> listarUsuarios();
                    case "4" -> listarDoacoesPorUsuario();
                    case "5" -> listarTodasDoacoes();
                    case "0" -> {
                        out.println("Saindo do menu. A API continua rodando em http://localhost:8081");
                        return;
                    }
                    default -> out.println("Opcao invalida. Digite 0-5.");
                }
            }
        } catch (NoSuchElementException | IllegalStateException e) {
            out.println("Entrada do terminal indisponivel. Pulando menu interativo, API continua rodando.");
        }
    }

    private void imprimirMenu() {
        out.println();
        out.println("1) cadastrar usuario");
        out.println("2) cadastrar doacao");
        out.println("3) listar usuarios");
        out.println("4) listar doacoes por usuario");
        out.println("5) listar todas as doacoes");
        out.println("0) sair do menu (API continua)");
    }

    private void cadastrarUsuario() {
        try {
            String nome = lerLinha("Nome: ");
            String email = lerLinha("Email: ");

            List<EnderecoDTO> enderecos = new ArrayList<>();
            String add = lerLinha("Adicionar endereco? (s/n): ");
            while (add.equalsIgnoreCase("s")) {
                String rua = lerLinha("Rua: ");
                String cidade = lerLinha("Cidade: ");
                String estado = lerLinha("Estado (2 letras, ex PR): ");
                EnderecoDTO dto = new EnderecoDTO(rua, cidade, estado);
                Set<ConstraintViolation<EnderecoDTO>> violacoesEnd = validator.validate(dto);
                if (!violacoesEnd.isEmpty()) {
                    out.println("Endereco invalido:");
                    violacoesEnd.forEach(v -> out.println(" - " + v.getPropertyPath() + ": " + v.getMessage()));
                } else {
                    enderecos.add(dto);
                }
                add = lerLinha("Adicionar outro endereco? (s/n): ");
            }

            UsuarioCreateRequest req = new UsuarioCreateRequest();
            req.setNome(nome);
            req.setEmail(email);
            req.setEnderecos(enderecos);

            Set<ConstraintViolation<UsuarioCreateRequest>> violacoes = validator.validate(req);
            if (!violacoes.isEmpty()) {
                out.println("Usuario invalido (400):");
                violacoes.forEach(v -> out.println(" - " + v.getPropertyPath() + ": " + v.getMessage()));
                return;
            }

            UsuarioResponse resp = usuarioService.criar(req);
            out.println("Usuario criado! id=" + resp.getId() + " -> /api/usuarios/" + resp.getId());
        } catch (DataAccessException e) {
            out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        } catch (Exception e) {
            out.println("Erro ao cadastrar usuario: " + e.getMessage());
        }
    }

    private void cadastrarDoacao() {
        try {
            String usuarioId = lerLinha("UsuarioId: ");
            if (usuarioService.buscarPorId(usuarioId).isEmpty()) {
                out.println("Aviso: usuarioId nao encontrado. A doacao sera salva orfa (mesmo comportamento da API).");
            }
            String item = lerLinha("Item (ex Arroz): ");
            int quantidade = lerInt("Quantidade (>0): ");
            String dataStr = lerLinha("Data (yyyy-MM-dd, vazio=hoje): ");

            LocalDate data = interpretarData(dataStr);
            if (data == null) {
                return;
            }

            DoacaoCreateRequest req = new DoacaoCreateRequest();
            req.setUsuarioId(usuarioId);
            req.setItem(item);
            req.setQuantidade(quantidade);
            req.setDataDoacao(data);

            Set<ConstraintViolation<DoacaoCreateRequest>> violacoes = validator.validate(req);
            if (!violacoes.isEmpty()) {
                out.println("Doacao invalida (400):");
                violacoes.forEach(v -> out.println(" - " + v.getPropertyPath() + ": " + v.getMessage()));
                return;
            }

            DoacaoResponse resp = doacaoService.criar(req);
            out.println("Doacao criada! id=" + resp.getId() + " -> /api/doacoes/" + resp.getId());
        } catch (DataAccessException e) {
            out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        } catch (Exception e) {
            out.println("Erro ao cadastrar doacao: " + e.getMessage());
        }
    }

    /**
     * Interpreta a data digitada no menu.
     *
     * @return a data informada, hoje quando o campo vem vazio, ou {@code null} quando o formato é inválido
     */
    LocalDate interpretarData(String dataStr) {
        if (dataStr == null || dataStr.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(dataStr.trim());
        } catch (DateTimeParseException e) {
            out.println("Data invalida. Use yyyy-MM-dd.");
            return null;
        }
    }

    private void listarUsuarios() {
        try {
            List<UsuarioResponse> usuarios = usuarioService.listarTodos();
            if (usuarios.isEmpty()) {
                out.println("Nenhum usuario cadastrado.");
                return;
            }
            out.println("id | nome | email");
            usuarios.forEach(u -> out.println(u.getId() + " | " + u.getNome() + " | " + u.getEmail()));
        } catch (DataAccessException e) {
            out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        }
    }

    private void listarDoacoesPorUsuario() {
        try {
            String usuarioId = lerLinha("UsuarioId: ");
            List<DoacaoResponse> doacoes = doacaoService.listarPorUsuario(usuarioId);
            if (doacoes.isEmpty()) {
                out.println("Nenhuma doacao para este usuario.");
                return;
            }
            out.println("id | item | qtd | data");
            doacoes.forEach(d -> out.println(d.getId() + " | " + d.getItem() + " | " + d.getQuantidade() + " | " + d.getDataDoacao()));
        } catch (DataAccessException e) {
            out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        }
    }

    private void listarTodasDoacoes() {
        try {
            List<DoacaoResponse> doacoes = doacaoService.listarTodos();
            if (doacoes.isEmpty()) {
                out.println("Nenhuma doacao cadastrada.");
                return;
            }
            out.println("id | usuarioId | item | qtd | data");
            doacoes.forEach(d -> out.println(d.getId() + " | " + d.getUsuarioId() + " | " + d.getItem() + " | " + d.getQuantidade() + " | " + d.getDataDoacao()));
        } catch (DataAccessException e) {
            out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        }
    }

    private String lerLinha(String prompt) {
        out.print(prompt);
        out.flush();
        return scanner.nextLine();
    }

    private int lerInt(String prompt) {
        while (true) {
            String valor = lerLinha(prompt);
            try {
                return Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                out.println("Digite um numero inteiro.");
            }
        }
    }
}
