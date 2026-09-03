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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}