# TODO — Próximos Passos AEP6S (ODS 2)

> CRUD `Usuario` + `Doacao` — Spring Boot 4.1.0, Java 17, MongoDB `poc_doacoes`. Este arquivo lista o que falta para deixar a conexão resiliente, implementar o `resumo` e começar a testar.

---

## 1. Deixar a conexão MongoDB resiliente

### 1.1 `aep/aep/compose.yaml:1` — adicionar `healthcheck` e `restart`

**Antes:**
```yaml
services:
  mongo:
    image: mongo:7.0
    container_name: aepmongojava2026_mongo
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_DATABASE: poc_doacoes
    volumes:
      - mongo-data:/data/db
volumes:
  mongo-data:
```

**Depois:**
```yaml
services:
  mongo:
    image: mongo:7.0
    container_name: aepmongojava2026_mongo
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_DATABASE: poc_doacoes
    volumes:
      - mongo-data:/data/db
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 5s
      timeout: 3s
      retries: 5
      start_period: 10s
    restart: unless-stopped
volumes:
  mongo-data:
```

Por que: sem `healthcheck` o Spring (`application.properties:5` `spring.docker.compose.enabled=true`) pode tentar conectar antes do Mongo estar pronto e falhar com `MongoTimeoutException`.

### 1.2 `aep/aep/src/main/resources/application.properties:2` — usar `uri`

**Antes:**
```properties
spring.application.name=aep
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=poc_doacoes
spring.docker.compose.enabled=true
```

**Depois:**
```properties
spring.application.name=aep
spring.data.mongodb.uri=mongodb://localhost:27017/poc_doacoes
spring.docker.compose.enabled=true
```

`host`/`port` ainda funciona, mas `uri` é o formato preferido no Boot 4.x.

### 1.3 `aep/aep/src/main/java/fz/exemple/aep/exception/GlobalExceptionHandler.java:29` — handler para Mongo fora do ar

**Antes:** só `RecursoNaoEncontradoException -> 404`, `MethodArgumentNotValidException -> 400` e `RuntimeException -> 500`.

**Depois — adicionar:**
```java
package fz.exemple.aep.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.MongoTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String,String>> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex){
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleValidationErrors(org.springframework.web.bind.MethodArgumentNotValidException ex){
        var erros = ex.getBindingResult().getFieldErrors().stream()
            .collect(java.util.stream.Collectors.toMap(
                fe -> fe.getField(),
                fe -> fe.getDefaultMessage(),
                (a,b) -> a));
        return ResponseEntity.badRequest().body(erros);
    }

    // NOVO: Mongo fora do ar vira 503 em vez de 500 genérico
    @ExceptionHandler({MongoTimeoutException.class, DataAccessException.class})
    public ResponseEntity<Map<String,String>> handleMongo(DataAccessException ex){
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "erro", "MongoDB indisponível, tente novamente",
                "detalhe", ex.getMostSpecificCause().getMessage()
            ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String,String>> handleRuntime(RuntimeException ex){
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("erro", ex.getMessage()));
    }
}
```

### 1.4 `aep/aep/pom.xml:34` — (opcional) Actuator para `/actuator/health`

Adicionar em `<dependencies>`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

E em `application.properties`:
```properties
management.health.mongo.enabled=true
management.endpoints.web.exposure.include=health
```

Verificação: `curl http://localhost:8080/actuator/health` deve mostrar `"mongo": {"status":"UP"}`.

**Verificação do bloco 1:**
```bash
docker compose down -v
docker compose up -d
docker ps --filter name=aepmongojava2026_mongo   # Up (healthy) após ~15s
.\mvnw.cmd spring-boot:run                        # Windows
./mvnw spring-boot:run                            # Mac/Linux
# derrube o mongo para testar o handler:
docker compose stop mongo
curl http://localhost:8080/api/usuarios           # deve retornar 503 com {"erro":"MongoDB indisponível"}
docker compose start mongo
```

---

## 2. Implementar `GET /api/doacoes/resumo`

> Gap: `AGENTS.md:20` documenta `GET /api/doacoes/resumo (total doações, quantidade, itens distintos)` mas `DoacaoController.java:1` não tem esse endpoint.

### 2.1 Criar `aep/aep/src/main/java/fz/exemple/aep/dto/ResumoResponse.java`

```java
package fz.exemple.aep.dto;

public class ResumoResponse {

    private long totalDoacoes;
    private int totalQuantidade;
    private long itensDistintos;

    public ResumoResponse(){}

    public ResumoResponse(long totalDoacoes, int totalQuantidade, long itensDistintos){
        this.totalDoacoes = totalDoacoes;
        this.totalQuantidade = totalQuantidade;
        this.itensDistintos = itensDistintos;
    }

    public long getTotalDoacoes(){ return totalDoacoes; }
    public void setTotalDoacoes(long totalDoacoes){ this.totalDoacoes = totalDoacoes; }

    public int getTotalQuantidade(){ return totalQuantidade; }
    public void setTotalQuantidade(int totalQuantidade){ this.totalQuantidade = totalQuantidade; }

    public long getItensDistintos(){ return itensDistintos; }
    public void setItensDistintos(long itensDistintos){ this.itensDistintos = itensDistintos; }
}
```

### 2.2 `aep/aep/src/main/java/fz/exemple/aep/services/DoacaoService.java:38` — adicionar método

```java
package fz.exemple.aep.services;

import fz.exemple.aep.dto.ResumoResponse;
// ... imports existentes

@Service
public class DoacaoService {

    private final DoacaoRepository doacaoRepository;

    public DoacaoService(DoacaoRepository doacaoRepository){
        this.doacaoRepository = doacaoRepository;
    }

    // ... métodos existentes criar/listarTodos/buscarPorId/listarPorUsuario/atualizar/deletar

    public ResumoResponse resumo(){
        List<Doacao> todas = doacaoRepository.findAll();
        long total = todas.size();
        int quantidade = todas.stream().mapToInt(Doacao::getQuantidade).sum();
        long distintos = todas.stream().map(Doacao::getItem).distinct().count();
        return new ResumoResponse(total, quantidade, distintos);
    }
}
```

### 2.3 `aep/aep/src/main/java/fz/exemple/aep/controllers/DoacaoController.java:34` — expor endpoint

```java
import fz.exemple.aep.dto.ResumoResponse;

@RestController
@RequestMapping("/api/doacoes")
public class DoacaoController {

    private final DoacaoService doacaoService;
    public DoacaoController(DoacaoService doacaoService){
        this.doacaoService = doacaoService;
    }

    @GetMapping("/resumo")
    public ResponseEntity<ResumoResponse> resumo(){
        return ResponseEntity.ok(doacaoService.resumo());
    }

    // ... demais endpoints já existentes (POST, GET, PUT, DELETE)
}
```

> Atenção: `"/resumo"` precisa vir **antes** de `"/{id}"` ou usar regex para não conflitar com `GET /{id}` quando `id=resumo`. Com `@GetMapping("/resumo")` acima de `@GetMapping("/{id}")` o Spring resolve corretamente.

**Verificação:**
```bash
curl -X POST http://localhost:8080/api/usuarios -H "Content-Type: application/json" -d '{"nome":"Ana","email":"ana@teste.com","enderecos":[]}'
# copie o id
curl -X POST http://localhost:8080/api/doacoes -H "Content-Type: application/json" -d '{"usuarioId":"ID","item":"Arroz","quantidade":10,"dataDoacao":"2026-09-02"}'
curl -X POST http://localhost:8080/api/doacoes -H "Content-Type: application/json" -d '{"usuarioId":"ID","item":"Feijão","quantidade":5,"dataDoacao":"2026-09-02"}'
curl http://localhost:8080/api/doacoes/resumo
# esperado: {"totalDoacoes":2,"totalQuantidade":15,"itensDistintos":2}
```

---

## 3. Setup básico para começar a testar (JUnit + JaCoCo + Maven)

Só o setup necessário para rodar testes locais. Sem Testcontainers nesta etapa.

### 3.1 `aep/aep/pom.xml:34` — adicionar dependência de teste

Em `<dependencies>`, junto às existentes:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Isso traz JUnit 5.11 (gerenciado por `spring-boot-starter-parent:4.1.0:5`), Mockito, AssertJ, MockMvc e JSONPath. Não precisa declarar versão.

### 3.2 `aep/aep/pom.xml:59` — adicionar plugins de teste e cobertura

Dentro de `<build><plugins>`, manter `spring-boot-maven-plugin` e `maven-compiler-plugin` e adicionar:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <includes><include>**/*Test.java</include></includes>
    </configuration>
</plugin>
```

E no `maven-compiler-plugin`, adicionar execução para testes com Lombok (necessário para `dto/EnderecoDTO.java:9` `@Data`):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <executions>
        <execution>
            <id>default-compile</id>
            <phase>compile</phase>
            <goals><goal>compile</goal></goals>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </execution>
        <execution>
            <id>default-testCompile</id>
            <phase>test-compile</phase>
            <goals><goal>testCompile</goal></goals>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 3.3 Criar `aep/aep/src/test/resources/application-test.properties` (opcional, vazio por enquanto)

```properties
# usa o mesmo Mongo de dev; para isolar, adicione:
# spring.data.mongodb.database=poc_doacoes_test
spring.docker.compose.enabled=false
```

### 3.4 Comandos para começar a testar

```bash
# rodar só os testes (JUnit via Surefire)
.\mvnw.cmd test          # Windows
./mvnw test              # Mac/Linux

# gerar relatório de cobertura JaCoCo
.\mvnw.cmd jacoco:report
# abra: aep/aep/target/site/jacoco/index.html

# build completo com cobertura
.\mvnw.cmd clean verify
```

---

## 4. Dois exemplos de testes simples

Coloque em `aep/aep/src/test/java/fz/exemple/aep/`.

### 4.1 Exemplo 1 — Unit puro: `mapper/UsuarioMapperTest.java`

Testa `aep/aep/src/main/java/fz/exemple/aep/mapper/UsuarioMapper.java:16` sem Spring nem Mongo.

```java
package fz.exemple.aep.mapper;

import fz.exemple.aep.dto.EnderecoDTO;
import fz.exemple.aep.dto.UsuarioCreateRequest;
import fz.exemple.aep.models.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioMapperTest {

    @Test
    void toEntity_comEnderecos_populados_mapeiaTudo(){
        var req = new UsuarioCreateRequest();
        req.setNome("Ana");
        req.setEmail("ana@teste.com");
        req.setEnderecos(List.of(new EnderecoDTO("Rua A, 123", "Maringá", "PR")));

        Usuario entity = UsuarioMapper.toEntity(req);

        assertEquals("Ana", entity.getNome());
        assertEquals("ana@teste.com", entity.getEmail());
        assertEquals(1, entity.getEnderecos().size());
        assertEquals("Rua A, 123", entity.getEnderecos().get(0).getRua());
    }

    @Test
    void toEntity_enderecosNull_mantemListaVazia(){
        var req = new UsuarioCreateRequest();
        req.setNome("Ana");
        req.setEmail("ana@teste.com");
        req.setEnderecos(null);

        Usuario entity = UsuarioMapper.toEntity(req);

        assertNotNull(entity.getEnderecos());
        assertTrue(entity.getEnderecos().isEmpty());
    }

    @Test
    void toEntity_update_comEnderecoNull_limpaLista(){
        var existente = new Usuario();
        existente.setNome("Antigo");
        existente.setEnderecos(List.of(new fz.exemple.aep.models.Endereco()));

        var req = new fz.exemple.aep.dto.UsuarioUpdateRequest();
        req.setNome("Novo");
        req.setEmail("novo@teste.com");
        req.setEnderecos(null);

        Usuario atualizado = UsuarioMapper.toEntity(req, existente);

        assertEquals("Novo", atualizado.getNome());
        assertTrue(atualizado.getEnderecos().isEmpty());
    }
}
```

Rodar: `.\mvnw.cmd test -Dtest=UsuarioMapperTest` → 3 testes verdes, sem Docker.

### 4.2 Exemplo 2 — Slice com MockMvc: `controllers/UsuarioControllerTest.java`

Testa `aep/aep/src/main/java/fz/exemple/aep/controllers/UsuarioController.java:25` (HTTP + validação) com `UsuarioService` mockado via `@MockitoBean`. Precisa de `config/SecurityConfig.java:16` `permitAll`.

```java
package fz.exemple.aep.controllers;

import fz.exemple.aep.config.SecurityConfig;
import fz.exemple.aep.dto.UsuarioResponse;
import fz.exemple.aep.services.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

    @Autowired MockMvc mvc;
    @MockitoBean UsuarioService usuarioService;

    @Test
    void criar_comDadosValidos_retorna201ComLocation() throws Exception {
        var resp = new UsuarioResponse("abc123", "Ana", "ana@teste.com", List.of());
        when(usuarioService.criar(any())).thenReturn(resp);

        mvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Ana","email":"ana@teste.com","enderecos":[]}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/usuarios/abc123")))
            .andExpect(jsonPath("$.id").value("abc123"))
            .andExpect(jsonPath("$.email").value("ana@teste.com"));
    }

    @Test
    void criar_comEmailInvalido_retorna400() throws Exception {
        mvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Ana","email":"invalido","enderecos":[]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void criar_comNomeVazio_retorna400() throws Exception {
        mvc.perform(post("/api/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"","email":"ana@teste.com","enderecos":[]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.nome").exists());
    }
}
```

Rodar: `.\mvnw.cmd test -Dtest=UsuarioControllerTest` → 3 testes verdes.

> Dica: `GlobalExceptionHandler.java:22` transforma `MethodArgumentNotValidException` em `400` com `Map<field, mensagem>`, por isso `jsonPath("$.email")` funciona.

---

## Checklist

- [ ] `compose.yaml` com `healthcheck` + `restart`
- [ ] `application.properties` com `uri`
- [ ] `GlobalExceptionHandler` com handler `503` para Mongo
- [ ] (opcional) `actuator` + `management.health.mongo.enabled`
- [ ] `ResumoResponse` + `DoacaoService.resumo()` + `DoacaoController GET /resumo`
- [ ] `pom.xml` com `spring-boot-starter-test` + `jacoco-maven-plugin` + `maven-surefire-plugin` + `default-testCompile` Lombok
- [ ] `src/test/resources/application-test.properties`
- [ ] `UsuarioMapperTest` (unit puro)
- [ ] `UsuarioControllerTest` (MockMvc slice)
- [ ] Rodar `.\mvnw.cmd test` e abrir `target/site/jacoco/index.html`
