package fz.exemple.aep.controllers;

import fz.exemple.aep.config.SecurityConfig;
import fz.exemple.aep.dto.DoacaoResponse;
import fz.exemple.aep.services.DoacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DoacaoController.class)
@Import(SecurityConfig.class)
class DoacaoControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    DoacaoService doacaoService;

    @Test
    void deveCriarDoacaoComDadosValidos() throws Exception {
        var resposta = new DoacaoResponse("d1", "user1", "Arroz", 10, null);

        when(doacaoService.criar(any())).thenReturn(resposta);

        mvc.perform(post("/api/doacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioId": "user1",
                                  "item": "Arroz",
                                  "quantidade": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.containsString("/api/doacoes/d1")
                ))
                .andExpect(jsonPath("$.id").value("d1"))
                .andExpect(jsonPath("$.item").value("Arroz"))
                .andExpect(jsonPath("$.quantidade").value(10));
    }

    @Test
    void deveRetornar400QuandoItemForVazio() throws Exception {
        mvc.perform(post("/api/doacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioId": "user1",
                                  "item": "",
                                  "quantidade": 10
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.item").exists());
    }

    @Test
    void deveRetornar400QuandoQuantidadeNaoForPositiva() throws Exception {
        mvc.perform(post("/api/doacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioId": "user1",
                                  "item": "Arroz",
                                  "quantidade": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.quantidade").exists());
    }

    @Test
    void deveListarTodasAsDoacoes() throws Exception {
        var resposta = new DoacaoResponse("d1", "user1", "Arroz", 10, null);

        when(doacaoService.listarTodos()).thenReturn(List.of(resposta));

        mvc.perform(get("/api/doacoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("d1"));
    }

    @Test
    void deveListarDoacoesPorUsuario() throws Exception {
        var resposta = new DoacaoResponse("d1", "user1", "Arroz", 10, null);

        when(doacaoService.listarPorUsuario("user1")).thenReturn(List.of(resposta));

        mvc.perform(get("/api/doacoes/usuario/user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value("user1"));
    }

    @Test
    void deveBuscarDoacaoPorIdQuandoExistir() throws Exception {
        var resposta = new DoacaoResponse("d1", "user1", "Arroz", 10, null);

        when(doacaoService.buscarPorId("d1")).thenReturn(Optional.of(resposta));

        mvc.perform(get("/api/doacoes/d1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("d1"));
    }

    @Test
    void deveRetornar404AoBuscarDoacaoPorIdInexistente() throws Exception {
        when(doacaoService.buscarPorId("xyz")).thenReturn(Optional.empty());

        mvc.perform(get("/api/doacoes/xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarDoacaoComDadosValidos() throws Exception {
        var resposta = new DoacaoResponse("d1", "user1", "Feijão", 5, null);

        when(doacaoService.atualizar(eq("d1"), any())).thenReturn(Optional.of(resposta));

        mvc.perform(put("/api/doacoes/d1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioId": "user1",
                                  "item": "Feijão",
                                  "quantidade": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.item").value("Feijão"));
    }

    @Test
    void deveRetornar404AoAtualizarDoacaoInexistente() throws Exception {
        when(doacaoService.atualizar(eq("xyz"), any())).thenReturn(Optional.empty());

        mvc.perform(put("/api/doacoes/xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "usuarioId": "user1",
                                  "item": "Feijão",
                                  "quantidade": 5
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveDeletarDoacao() throws Exception {
        mvc.perform(delete("/api/doacoes/d1"))
                .andExpect(status().isNoContent());

        verify(doacaoService).deletar("d1");
    }
}
