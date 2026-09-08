package fz.exemple.aep.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveTratarRecursoNaoEncontrado() {
        var exception = new RecursoNaoEncontradoException("Usuário não encontrado");

        ResponseEntity<Map<String, String>> response = handler.handleRecursoNaoEncontrado(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Usuário não encontrado", response.getBody().get("erro"));
    }

    @Test
    void deveTratarErrosDeValidacao() {
        var fieldError = new FieldError("objeto", "nome", "não pode ser vazio");
        var bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        var exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidationErrors(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("não pode ser vazio", response.getBody().get("nome"));
    }

    @Test
    void deveTratarRuntimeExceptionGenerica() {
        var exception = new RuntimeException("Falha inesperada");

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Falha inesperada", response.getBody().get("erro"));
    }
}
