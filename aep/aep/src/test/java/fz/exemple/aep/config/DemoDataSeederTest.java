package fz.exemple.aep.config;

import fz.exemple.aep.models.Doacao;
import fz.exemple.aep.models.Usuario;
import fz.exemple.aep.repositories.DoacaoRepository;
import fz.exemple.aep.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DemoDataSeeder - dados de exemplo do perfil demo")
class DemoDataSeederTest {

    private UsuarioRepository usuarioRepository;
    private DoacaoRepository doacaoRepository;
    private DemoDataSeeder seeder;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        doacaoRepository = mock(DoacaoRepository.class);
        seeder = new DemoDataSeeder(usuarioRepository, doacaoRepository);
    }

    private void simularIdsGerados() {
        AtomicInteger sequencia = new AtomicInteger(1);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> {
            Usuario usuario = invocacao.getArgument(0);
            usuario.setId("usuario-" + sequencia.getAndIncrement());
            return usuario;
        });
    }

    @Test
    @DisplayName("banco vazio cria 2 doadores e 3 doacoes")
    void bancoVazio_criaDadosDeExemplo() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(doacaoRepository.count()).thenReturn(0L);
        simularIdsGerados();

        seeder.run();

        ArgumentCaptor<Usuario> usuarios = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository, times(2)).save(usuarios.capture());
        assertEquals("Ana Silva", usuarios.getAllValues().get(0).getNome());
        assertEquals(2, usuarios.getAllValues().get(0).getEnderecos().size());
        assertEquals("Banco de Alimentos de Maringá", usuarios.getAllValues().get(1).getNome());
        assertEquals("PR", usuarios.getAllValues().get(1).getEnderecos().get(0).getEstado());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Doacao>> doacoes = ArgumentCaptor.forClass(List.class);
        verify(doacaoRepository).saveAll(doacoes.capture());

        List<Doacao> criadas = doacoes.getValue();
        assertEquals(3, criadas.size());
        assertEquals("Arroz", criadas.get(0).getItem());
        assertEquals(10, criadas.get(0).getQuantidade());
        assertEquals("usuario-1", criadas.get(0).getUsuarioId());
        assertEquals("Feijão", criadas.get(1).getItem());
        assertEquals("Leite", criadas.get(2).getItem());
        assertTrue(criadas.stream().allMatch(d -> d.getDataDoacao() != null));
    }

    @Test
    @DisplayName("banco com doadores nao recebe seed")
    void bancoComDoadores_naoCriaNada() {
        when(usuarioRepository.count()).thenReturn(2L);
        when(doacaoRepository.count()).thenReturn(0L);

        seeder.run();

        verify(usuarioRepository, never()).save(any());
        verify(doacaoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("banco so com doacoes nao recebe seed")
    void bancoSoComDoacoes_naoCriaNada() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(doacaoRepository.count()).thenReturn(3L);

        seeder.run();

        verify(usuarioRepository, never()).save(any());
        verify(doacaoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("banco indisponivel nao derruba a aplicacao")
    void bancoIndisponivel_naoLancaExcecao() {
        when(usuarioRepository.count()).thenThrow(new DataAccessResourceFailureException("down"));

        assertDoesNotThrow(() -> seeder.run());
        verify(usuarioRepository, never()).save(any());
        verify(doacaoRepository, never()).saveAll(any());
    }
}
