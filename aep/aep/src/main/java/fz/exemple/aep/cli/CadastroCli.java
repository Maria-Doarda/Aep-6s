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

    public CadastroCli(UsuarioService usuarioService, DoacaoService doacaoService, Validator validator) {
        this.usuarioService = usuarioService;
        this.doacaoService = doacaoService;
        this.validator = validator;
    }

    @Override
    public void run(String... args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("=== AEP6S terminal (API continua em http://localhost:8081) ===");
        try {
            while (true) {
                imprimirMenu();
                String opcao = lerLinha(scanner, "> ");
                switch (opcao.trim()) {
                    case "1" -> cadastrarUsuario(scanner);
                    case "2" -> cadastrarDoacao(scanner);
                    case "3" -> listarUsuarios();
                    case "4" -> listarDoacoesPorUsuario(scanner);
                    case "5" -> listarTodasDoacoes();
                    case "0" -> {
                        System.out.println("Saindo do menu. A API continua rodando em http://localhost:8081");
                        return;
                    }
                    default -> System.out.println("Opcao invalida. Digite 0-5.");
                }
            }
        } catch (NoSuchElementException | IllegalStateException e) {
            System.out.println("Entrada do terminal indisponivel. Pulando menu interativo, API continua rodando.");
        }
    }

    private void imprimirMenu() {
        System.out.println();
        System.out.println("1) cadastrar usuario");
        System.out.println("2) cadastrar doacao");
        System.out.println("3) listar usuarios");
        System.out.println("4) listar doacoes por usuario");
        System.out.println("5) listar todas as doacoes");
        System.out.println("0) sair do menu (API continua)");
    }

    private void cadastrarUsuario(Scanner scanner) {
        try {
            String nome = lerLinha(scanner, "Nome: ");
            String email = lerLinha(scanner, "Email: ");

            List<EnderecoDTO> enderecos = new ArrayList<>();
            String add = lerLinha(scanner, "Adicionar endereco? (s/n): ");
            while (add.equalsIgnoreCase("s")) {
                String rua = lerLinha(scanner, "Rua: ");
                String cidade = lerLinha(scanner, "Cidade: ");
                String estado = lerLinha(scanner, "Estado (2 letras, ex PR): ");
                EnderecoDTO dto = new EnderecoDTO(rua, cidade, estado);
                Set<ConstraintViolation<EnderecoDTO>> violacoesEnd = validator.validate(dto);
                if (!violacoesEnd.isEmpty()) {
                    System.out.println("Endereco invalido:");
                    violacoesEnd.forEach(v -> System.out.println(" - " + v.getPropertyPath() + ": " + v.getMessage()));
                } else {
                    enderecos.add(dto);
                }
                add = lerLinha(scanner, "Adicionar outro endereco? (s/n): ");
            }

            UsuarioCreateRequest req = new UsuarioCreateRequest();
            req.setNome(nome);
            req.setEmail(email);
            req.setEnderecos(enderecos);

            Set<ConstraintViolation<UsuarioCreateRequest>> violacoes = validator.validate(req);
            if (!violacoes.isEmpty()) {
                System.out.println("Usuario invalido (400):");
                violacoes.forEach(v -> System.out.println(" - " + v.getPropertyPath() + ": " + v.getMessage()));
                return;
            }

            UsuarioResponse resp = usuarioService.criar(req);
            System.out.println("Usuario criado! id=" + resp.getId() + " -> /api/usuarios/" + resp.getId());
        } catch (DataAccessException e) {
            System.out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        } catch (Exception e) {
            System.out.println("Erro ao cadastrar usuario: " + e.getMessage());
        }
    }

    private void cadastrarDoacao(Scanner scanner) {
        try {
            String usuarioId = lerLinha(scanner, "UsuarioId: ");
            if (usuarioService.buscarPorId(usuarioId).isEmpty()) {
                System.out.println("Aviso: usuarioId nao encontrado. A doacao sera salva orfa (mesmo comportamento da API).");
            }
            String item = lerLinha(scanner, "Item (ex Arroz): ");
            int quantidade = lerInt(scanner, "Quantidade (>0): ");
            String dataStr = lerLinha(scanner, "Data (yyyy-MM-dd, vazio=hoje): ");

            LocalDate data = null;
            if (!dataStr.isBlank()) {
                try {
                    data = LocalDate.parse(dataStr.trim());
                } catch (DateTimeParseException e) {
                    System.out.println("Data invalida. Use yyyy-MM-dd.");
                    return;
                }
            } else {
                data = LocalDate.now();
            }

            DoacaoCreateRequest req = new DoacaoCreateRequest();
            req.setUsuarioId(usuarioId);
            req.setItem(item);
            req.setQuantidade(quantidade);
            req.setDataDoacao(data);

            Set<ConstraintViolation<DoacaoCreateRequest>> violacoes = validator.validate(req);
            if (!violacoes.isEmpty()) {
                System.out.println("Doacao invalida (400):");
                violacoes.forEach(v -> System.out.println(" - " + v.getPropertyPath() + ": " + v.getMessage()));
                return;
            }

            DoacaoResponse resp = doacaoService.criar(req);
            System.out.println("Doacao criada! id=" + resp.getId() + " -> /api/doacoes/" + resp.getId());
        } catch (DataAccessException e) {
            System.out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        } catch (Exception e) {
            System.out.println("Erro ao cadastrar doacao: " + e.getMessage());
        }
    }

    private void listarUsuarios() {
        try {
            List<UsuarioResponse> usuarios = usuarioService.listarTodos();
            if (usuarios.isEmpty()) {
                System.out.println("Nenhum usuario cadastrado.");
                return;
            }
            System.out.println("id | nome | email");
            usuarios.forEach(u -> System.out.println(u.getId() + " | " + u.getNome() + " | " + u.getEmail()));
        } catch (DataAccessException e) {
            System.out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        }
    }

    private void listarDoacoesPorUsuario(Scanner scanner) {
        try {
            String usuarioId = lerLinha(scanner, "UsuarioId: ");
            List<DoacaoResponse> doacoes = doacaoService.listarPorUsuario(usuarioId);
            if (doacoes.isEmpty()) {
                System.out.println("Nenhuma doacao para este usuario.");
                return;
            }
            System.out.println("id | item | qtd | data");
            doacoes.forEach(d -> System.out.println(d.getId() + " | " + d.getItem() + " | " + d.getQuantidade() + " | " + d.getDataDoacao()));
        } catch (DataAccessException e) {
            System.out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        }
    }

    private void listarTodasDoacoes() {
        try {
            List<DoacaoResponse> doacoes = doacaoService.listarTodos();
            if (doacoes.isEmpty()) {
                System.out.println("Nenhuma doacao cadastrada.");
                return;
            }
            System.out.println("id | usuarioId | item | qtd | data");
            doacoes.forEach(d -> System.out.println(d.getId() + " | " + d.getUsuarioId() + " | " + d.getItem() + " | " + d.getQuantidade() + " | " + d.getDataDoacao()));
        } catch (DataAccessException e) {
            System.out.println("MongoDB indisponivel (503). Suba com: docker compose up -d");
        }
    }

    private String lerLinha(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private int lerInt(Scanner scanner, String prompt) {
        while (true) {
            String valor = lerLinha(scanner, prompt);
            try {
                return Integer.parseInt(valor.trim());
            } catch (NumberFormatException e) {
                System.out.println("Digite um numero inteiro.");
            }
        }
    }
}
