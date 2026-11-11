package fz.exemple.aep.cli;

import fz.exemple.aep.dto.DoacaoCreateRequest;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.dto.UsuarioCreateRequest;
import fz.exemple.aep.dto.UsuarioResponse;
import fz.exemple.aep.services.DoacaoService;
import fz.exemple.aep.services.UsuarioService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("CadastroCli - menu do terminal")
class CadastroCliTest {

    private UsuarioService usuarioService;
    private DoacaoService doacaoService;
    private ByteArrayOutputStream saida;
    private CadastroCli cli;

    @BeforeEach
    void setUp() {
        usuarioService = mock(UsuarioService.class);
        doacaoService = mock(DoacaoService.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        cli = new CadastroCli(usuarioService, doacaoService, validator);
        saida = new ByteArrayOutputStream();
        cli.setOut(new PrintStream(saida, true, StandardCharsets.UTF_8));
    }

    private void executar(String entrada) {
        cli.setScanner(new java.util.Scanner(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8))));
        cli.run();
    }

    private String tela() {
        return saida.toString(StandardCharsets.UTF_8);
    }

    // ---------- menu ----------

    @Test
    @DisplayName("opcao invalida avisa e volta ao menu")
    void opcaoInvalida_avisaEVoltaAoMenu() {
        executar("9\n0\n");

        assertTrue(tela().contains("Opcao invalida. Digite 0-5."));
        assertTrue(tela().contains("Saindo do menu. A API continua rodando"));
    }

    @Test
    @DisplayName("entrada encerrada nao quebra o menu")
    void entradaEncerrada_naoQuebra() {
        assertDoesNotThrow(() -> executar(""));
        assertTrue(tela().contains("Entrada do terminal indisponivel"));
    }

    @Test
    @DisplayName("imprime o menu com as opcoes 0 a 5")
    void imprimeMenu() {
        executar("0\n");

        assertTrue(tela().contains("1) cadastrar usuario"));
        assertTrue(tela().contains("5) listar todas as doacoes"));
        assertTrue(tela().contains("0) sair do menu"));
    }

    // ---------- cadastro de usuario ----------

    @Test
    @DisplayName("cadastra usuario valido sem endereco")
    void cadastrarUsuario_validoSemEndereco() {
        when(usuarioService.criar(any())).thenReturn(new UsuarioResponse("abc123", "Ana", "ana@teste.com", List.of()));

        executar("1\nAna\nana@teste.com\nn\n0\n");

        assertTrue(tela().contains("Usuario criado! id=abc123 -> /api/usuarios/abc123"));

        ArgumentCaptor<UsuarioCreateRequest> captor = ArgumentCaptor.forClass(UsuarioCreateRequest.class);
        verify(usuarioService).criar(captor.capture());
        assertEquals("Ana", captor.getValue().getNome());
        assertEquals("ana@teste.com", captor.getValue().getEmail());
        assertTrue(captor.getValue().getEnderecos().isEmpty());
    }

    @Test
    @DisplayName("email invalido nao chama o servico")
    void cadastrarUsuario_emailInvalido_naoCria() {
        executar("1\nAna\ninvalido\nn\n0\n");

        assertTrue(tela().contains("Usuario invalido (400)"));
        assertTrue(tela().contains("email"));
        verify(usuarioService, never()).criar(any());
    }

    @Test
    @DisplayName("nome vazio nao chama o servico")
    void cadastrarUsuario_nomeVazio_naoCria() {
        executar("1\n\nana@teste.com\nn\n0\n");

        assertTrue(tela().contains("Usuario invalido (400)"));
        verify(usuarioService, never()).criar(any());
    }

    @Test
    @DisplayName("endereco invalido e descartado e o valido e mantido")
    void cadastrarUsuario_enderecoInvalidoDescartaOValidoMantem() {
        when(usuarioService.criar(any())).thenReturn(new UsuarioResponse("abc123", "Ana", "ana@teste.com", List.of()));

        executar("1\nAna\nana@teste.com\ns\nRua A\nMaringa\nPARANA\ns\nRua B, 10\nMaringa\nPR\nn\n0\n");

        assertTrue(tela().contains("Endereco invalido:"));

        ArgumentCaptor<UsuarioCreateRequest> captor = ArgumentCaptor.forClass(UsuarioCreateRequest.class);
        verify(usuarioService).criar(captor.capture());
        assertEquals(1, captor.getValue().getEnderecos().size());
        assertEquals("PR", captor.getValue().getEnderecos().get(0).getEstado());
    }

    // ---------- cadastro de doacao ----------

    @Test
    @DisplayName("cadastra doacao com a data informada")
    void cadastrarDoacao_comDataInformada() {
        when(usuarioService.buscarPorId("u1"))
                .thenReturn(Optional.of(new UsuarioResponse("u1", "Ana", "ana@teste.com", List.of())));
        when(doacaoService.criar(any()))
                .thenReturn(new DoacaoResponse("d1", "u1", "Arroz", 10, LocalDate.of(2026, 9, 2)));

        executar("2\nu1\nArroz\n10\n2026-09-02\n0\n");

        assertTrue(tela().contains("Doacao criada! id=d1 -> /api/doacoes/d1"));
        assertFalse(tela().contains("Aviso: usuarioId nao encontrado"));

        ArgumentCaptor<DoacaoCreateRequest> captor = ArgumentCaptor.forClass(DoacaoCreateRequest.class);
        verify(doacaoService).criar(captor.capture());
        assertEquals(LocalDate.of(2026, 9, 2), captor.getValue().getDataDoacao());
        assertEquals("Arroz", captor.getValue().getItem());
        assertEquals(10, captor.getValue().getQuantidade());
    }

    @Test
    @DisplayName("data vazia usa a data de hoje")
    void cadastrarDoacao_dataVaziaUsaHoje() {
        when(usuarioService.buscarPorId(anyString())).thenReturn(Optional.empty());
        when(doacaoService.criar(any())).thenReturn(new DoacaoResponse("d1", "u1", "Arroz", 3, LocalDate.now()));

        executar("2\nu1\nArroz\n3\n\n0\n");

        ArgumentCaptor<DoacaoCreateRequest> captor = ArgumentCaptor.forClass(DoacaoCreateRequest.class);
        verify(doacaoService).criar(captor.capture());
        assertEquals(LocalDate.now(), captor.getValue().getDataDoacao());
    }

    @Test
    @DisplayName("data em formato invalido nao cria a doacao")
    void cadastrarDoacao_dataInvalida_naoCria() {
        when(usuarioService.buscarPorId(anyString())).thenReturn(Optional.empty());

        executar("2\nu1\nArroz\n10\n31/12/2026\n0\n");

        assertTrue(tela().contains("Data invalida. Use yyyy-MM-dd."));
        verify(doacaoService, never()).criar(any());
    }

    @Test
    @DisplayName("usuario inexistente gera aviso de doacao orfa")
    void cadastrarDoacao_usuarioInexistente_avisaOrfa() {
        when(usuarioService.buscarPorId("u404")).thenReturn(Optional.empty());
        when(doacaoService.criar(any())).thenReturn(new DoacaoResponse("d1", "u404", "Arroz", 1, LocalDate.now()));

        executar("2\nu404\nArroz\n1\n2026-09-02\n0\n");

        assertTrue(tela().contains("Aviso: usuarioId nao encontrado"));
    }

    @Test
    @DisplayName("quantidade nao numerica pede novamente")
    void cadastrarDoacao_quantidadeNaoNumerica_pedeNovamente() {
        when(usuarioService.buscarPorId(anyString())).thenReturn(Optional.empty());
        when(doacaoService.criar(any())).thenReturn(new DoacaoResponse("d1", "u1", "Arroz", 12, LocalDate.now()));

        executar("2\nu1\nArroz\nabc\n12\n2026-09-02\n0\n");

        assertTrue(tela().contains("Digite um numero inteiro."));

        ArgumentCaptor<DoacaoCreateRequest> captor = ArgumentCaptor.forClass(DoacaoCreateRequest.class);
        verify(doacaoService).criar(captor.capture());
        assertEquals(12, captor.getValue().getQuantidade());
    }

    @Test
    @DisplayName("quantidade zero e recusada pela validacao")
    void cadastrarDoacao_quantidadeZero_invalida() {
        when(usuarioService.buscarPorId(anyString())).thenReturn(Optional.empty());

        executar("2\nu1\nArroz\n0\n2026-09-02\n0\n");

        assertTrue(tela().contains("Doacao invalida (400)"));
        verify(doacaoService, never()).criar(any());
    }

    @Test
    @DisplayName("item vazio e recusado pela validacao")
    void cadastrarDoacao_itemVazio_invalida() {
        when(usuarioService.buscarPorId(anyString())).thenReturn(Optional.empty());

        executar("2\nu1\n\n5\n2026-09-02\n0\n");

        assertTrue(tela().contains("Doacao invalida (400)"));
        verify(doacaoService, never()).criar(any());
    }

    // ---------- listagens ----------

    @Test
    @DisplayName("lista usuarios vazia informa que nao ha cadastro")
    void listarUsuarios_vazio() {
        when(usuarioService.listarTodos()).thenReturn(List.of());

        executar("3\n0\n");

        assertTrue(tela().contains("Nenhum usuario cadastrado."));
    }

    @Test
    @DisplayName("lista usuarios com conteudo mostra id, nome e email")
    void listarUsuarios_comConteudo() {
        when(usuarioService.listarTodos())
                .thenReturn(List.of(new UsuarioResponse("abc", "Ana", "ana@teste.com", List.of())));

        executar("3\n0\n");

        assertTrue(tela().contains("id | nome | email"));
        assertTrue(tela().contains("abc | Ana | ana@teste.com"));
    }

    @Test
    @DisplayName("lista doacoes por usuario vazia")
    void listarDoacoesPorUsuario_vazio() {
        when(doacaoService.listarPorUsuario("u1")).thenReturn(List.of());

        executar("4\nu1\n0\n");

        assertTrue(tela().contains("Nenhuma doacao para este usuario."));
    }

    @Test
    @DisplayName("lista doacoes por usuario com conteudo")
    void listarDoacoesPorUsuario_comConteudo() {
        when(doacaoService.listarPorUsuario("u1"))
                .thenReturn(List.of(new DoacaoResponse("d1", "u1", "Arroz", 10, LocalDate.of(2026, 9, 2))));

        executar("4\nu1\n0\n");

        assertTrue(tela().contains("id | item | qtd | data"));
        assertTrue(tela().contains("d1 | Arroz | 10 | 2026-09-02"));
    }

    @Test
    @DisplayName("lista todas as doacoes vazia")
    void listarTodasDoacoes_vazio() {
        when(doacaoService.listarTodos()).thenReturn(List.of());

        executar("5\n0\n");

        assertTrue(tela().contains("Nenhuma doacao cadastrada."));
    }

    @Test
    @DisplayName("lista todas as doacoes com conteudo")
    void listarTodasDoacoes_comConteudo() {
        when(doacaoService.listarTodos())
                .thenReturn(List.of(new DoacaoResponse("d1", "u1", "Arroz", 10, LocalDate.of(2026, 9, 2))));

        executar("5\n0\n");

        assertTrue(tela().contains("id | usuarioId | item | qtd | data"));
        assertTrue(tela().contains("d1 | u1 | Arroz | 10 | 2026-09-02"));
    }

    // ---------- falhas de infraestrutura ----------

    @Test
    @DisplayName("Mongo fora do ar ao cadastrar usuario mostra mensagem de 503")
    void cadastrarUsuario_mongoIndisponivel() {
        when(usuarioService.criar(any())).thenThrow(new DataAccessResourceFailureException("down"));

        executar("1\nAna\nana@teste.com\nn\n0\n");

        assertTrue(tela().contains("MongoDB indisponivel (503). Suba com: docker compose up -d"));
    }

    @Test
    @DisplayName("erro inesperado ao cadastrar usuario e reportado")
    void cadastrarUsuario_erroInesperado() {
        when(usuarioService.criar(any())).thenThrow(new IllegalStateException("boom"));

        executar("1\nAna\nana@teste.com\nn\n0\n");

        assertTrue(tela().contains("Erro ao cadastrar usuario: boom"));
    }

    @Test
    @DisplayName("Mongo fora do ar ao cadastrar doacao mostra mensagem de 503")
    void cadastrarDoacao_mongoIndisponivel() {
        when(usuarioService.buscarPorId(anyString())).thenReturn(Optional.empty());
        when(doacaoService.criar(any())).thenThrow(new DataAccessResourceFailureException("down"));

        executar("2\nu1\nArroz\n5\n2026-09-02\n0\n");

        assertTrue(tela().contains("MongoDB indisponivel (503). Suba com: docker compose up -d"));
    }

    @Test
    @DisplayName("erro inesperado ao cadastrar doacao e reportado")
    void cadastrarDoacao_erroInesperado() {
        when(usuarioService.buscarPorId(anyString())).thenReturn(Optional.empty());
        when(doacaoService.criar(any())).thenThrow(new IllegalStateException("boom"));

        executar("2\nu1\nArroz\n5\n2026-09-02\n0\n");

        assertTrue(tela().contains("Erro ao cadastrar doacao: boom"));
    }

    @Test
    @DisplayName("Mongo fora do ar nas listagens mostra mensagem de 503")
    void listagens_mongoIndisponivel() {
        when(usuarioService.listarTodos()).thenThrow(new DataAccessResourceFailureException("down"));
        when(doacaoService.listarTodos()).thenThrow(new DataAccessResourceFailureException("down"));
        when(doacaoService.listarPorUsuario(anyString())).thenThrow(new DataAccessResourceFailureException("down"));

        executar("3\n4\nu1\n5\n0\n");

        assertEquals(3, tela().split("MongoDB indisponivel \\(503\\)", -1).length - 1);
    }

    // ---------- interpretacao de data ----------

    @Test
    @DisplayName("interpretarData aceita yyyy-MM-dd")
    void interpretarData_formatoIso() {
        assertEquals(LocalDate.of(2026, 9, 2), cli.interpretarData("2026-09-02"));
    }

    @Test
    @DisplayName("interpretarData usa hoje quando vazio")
    void interpretarData_vazioUsaHoje() {
        assertEquals(LocalDate.now(), cli.interpretarData(""));
        assertEquals(LocalDate.now(), cli.interpretarData(null));
    }

    @Test
    @DisplayName("interpretarData devolve null para formato invalido")
    void interpretarData_invalido() {
        assertNull(cli.interpretarData("02/09/2026"));
        assertTrue(tela().contains("Data invalida. Use yyyy-MM-dd."));
    }
}
