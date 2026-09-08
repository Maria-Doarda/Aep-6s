package fz.exemple.aep.controllers;

import fz.exemple.aep.config.SecurityConfig;
import fz.exemple.aep.dto.UsuarioResponse;
import fz.exemple.aep.services.UsuarioService;
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

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UsuarioService usuarioService;

    @Test
    void deveCriarUsuarioComDadosValidos() throws Exception {
        var resposta = new UsuarioResponse(
                "abc123",
                "Ana",
                "ana@teste.com",
                List.of()
        );

        when(usuarioService.criar(any())).thenReturn(resposta);

        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Ana",
                                  "email": "ana@teste.com",
                                  "enderecos": []
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.containsString("/api/usuarios/abc123")
                ))
                .andExpect(jsonPath("$.id").value("abc123"))
                .andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@teste.com"));
    }

    @Test
    void deveRetornar400QuandoEmailForInvalido() throws Exception {
        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Ana",
                                  "email": "email-invalido",
                                  "enderecos": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void deveRetornar400QuandoNomeForVazio() throws Exception {
        mvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "",
                                  "email": "ana@teste.com",
                                  "enderecos": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nome").exists());
    }

    @Test
    void deveListarTodosOsUsuarios() throws Exception {
        var resposta = new UsuarioResponse("abc123", "Ana", "ana@teste.com", List.of());

        when(usuarioService.listarTodos()).thenReturn(List.of(resposta));

        mvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("abc123"))
                .andExpect(jsonPath("$[0].nome").value("Ana"));
    }

    @Test
    void deveBuscarUsuarioPorIdQuandoExistir() throws Exception {
        var resposta = new UsuarioResponse("abc123", "Ana", "ana@teste.com", List.of());

        when(usuarioService.buscarPorId("abc123")).thenReturn(Optional.of(resposta));

        mvc.perform(get("/api/usuarios/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc123"));
    }

    @Test
    void deveRetornar404AoBuscarUsuarioPorIdInexistente() throws Exception {
        when(usuarioService.buscarPorId("xyz")).thenReturn(Optional.empty());

        mvc.perform(get("/api/usuarios/xyz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveAtualizarUsuarioComDadosValidos() throws Exception {
        var resposta = new UsuarioResponse("abc123", "Ana Nova", "ananova@teste.com", List.of());

        when(usuarioService.atualizar(eq("abc123"), any())).thenReturn(Optional.of(resposta));

        mvc.perform(put("/api/usuarios/abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Ana Nova",
                                  "email": "ananova@teste.com",
                                  "enderecos": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Nova"));
    }

    @Test
    void deveRetornar404AoAtualizarUsuarioInexistente() throws Exception {
        when(usuarioService.atualizar(eq("xyz"), any())).thenReturn(Optional.empty());

        mvc.perform(put("/api/usuarios/xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Ana",
                                  "email": "ana@teste.com",
                                  "enderecos": []
                                }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveDeletarUsuario() throws Exception {
        mvc.perform(delete("/api/usuarios/abc123"))
                .andExpect(status().isNoContent());

        verify(usuarioService).deletar("abc123");
    }
}