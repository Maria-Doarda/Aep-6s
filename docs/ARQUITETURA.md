# Arquitetura — AEP6S (ODS 2)

Documento técnico da PoC de doação de alimentos. Complementa o [`README.md`](../README.md) (execução) e o [`MODELAGEM.md`](MODELAGEM.md) (banco de dados).

## 1. Visão geral

Aplicação **monolítica** Spring Boot 4.1.0 em Java 17, exposta como API REST na porta **8081** e persistida no **MongoDB 7.0** (database `poc_doacoes`). Um menu interativo no terminal roda junto com a API, usando os mesmos serviços.

```
┌──────────────┐   HTTP/JSON    ┌──────────────────────────────────────────┐
│ curl/Postman │ ─────────────► │  Spring Boot (Tomcat, :8081)             │
│ navegador    │                │                                          │
└──────────────┘                │  controller → service → repository      │
                                │        ↕ dto/mapper      ↕ model         │
┌──────────────┐   System.in    │                                          │
│ terminal     │ ─────────────► │  cli.CadastroCli (CommandLineRunner)      │
└──────────────┘                └───────────────────┬──────────────────────┘
                                                    │ Spring Data MongoDB
                                                    ▼
                                        ┌───────────────────────┐
                                        │ MongoDB :27017        │
                                        │  poc_doacoes          │
                                        │  ├─ usuarios          │
                                        │  └─ doacoes           │
                                        └───────────────────────┘
```

## 2. Camadas e responsabilidades

| Pacote | Responsabilidade | Não faz |
|---|---|---|
| `controllers` | Recebe HTTP, valida (`@Valid`), devolve status e `Location` | não conhece Mongo nem regra de negócio |
| `services` | Regras de negócio, orquestra repositório e mapper | não conhece HTTP |
| `repositories` | Interfaces `MongoRepository` + *derived queries* | não tem lógica |
| `models` | Documentos (`@Document`): `Usuario`, `Doacao`, `Endereco` | não tem validação de entrada |
| `dto` | Contratos de entrada/saída (`*Request`, `*Response`, `EnderecoDTO`) | não é persistido |
| `mapper` (`UsuarioMapper`, `DoacaoMapper`) | Converte DTO ↔ entidade | não acessa banco |
| `exception` | `@RestControllerAdvice` + exceção de recurso não encontrado | — |
| `config` | `SecurityConfig` (libera tudo, desliga CSRF) | — |
| `cli` | Menu no terminal, ativo fora do perfil `test` | não acessa repositório direto |

Regra de dependência: `controllers → services → repositories → models`. DTO e mapper são usados na borda, então o modelo nunca vaza para a resposta HTTP.

## 3. Fluxo de uma requisição

Exemplo — `POST /api/doacoes`:

1. `SecurityConfig` deixa passar (todas as rotas são públicas)
2. `DoacaoController.criar(@Valid DoacaoCreateRequest)` valida o JSON; erro → `400` com `{campo: mensagem}`
3. `DoacaoService.criar(request)` chama `exigirDoador(usuarioId)`: se o `usuarioId` não existir em `usuarios`, lança `RecursoNaoEncontradoException` → `404`
4. `DoacaoMapper.toEntity(request)` converte o DTO no documento `Doacao`
5. `DoacaoRepository.save(entity)` grava na coleção `doacoes`; o Mongo gera o `ObjectId` (os índices de `@Indexed`/`@CompoundIndex` já existem desde a subida)
6. `DoacaoMapper.toResponse(saved)` monta o `DoacaoResponse`
7. Controller responde **`201 Created`** com header `Location: /api/doacoes/{id}` e o corpo do recurso

Leitura — `GET /api/doacoes/{id}`: o service devolve `Optional<DoacaoResponse>`; o controller converte em `200` ou `404` (sem exception).

Agregação — `GET /api/doacoes/resumo` e `/resumo/por-item`: o service monta uma `Aggregation`
e chama `MongoTemplate.aggregate(...)`. O `$group` roda no servidor MongoDB e a aplicação
recebe só o resultado, sem carregar os documentos.

## 4. Tratamento de erros

| Situação | Status | Corpo | Origem |
|---|---|---|---|
| Campo inválido (`email` sem `@`, `quantidade <= 0`, `nome` vazio) | `400` | `{"email": "deve ser um endereço de e-mail válido"}` | `MethodArgumentNotValidException` |
| ID inexistente | `404` | *(vazio)* | `Optional.empty()` no controller |
| Doador inexistente no `POST`/`PUT` de doação | `404` | `{"erro": "Doador nao encontrado: ..."}` | `RecursoNaoEncontradoException` |
| Falha inesperada | `500` | `{"erro": "..."}` | `RuntimeException` |

Todas as conversões ficam em `exception/GlobalExceptionHandler` (`@RestControllerAdvice`).

> Observação: o `404` de *doação/usuário inexistente por id* vem do `Optional.empty()` no
> controller; o `404` de *doador inexistente na gravação* vem de `RecursoNaoEncontradoException`,
> lançada no service porque a regra só pode ser verificada no momento de salvar.

## 5. Segurança

`config/SecurityConfig` libera **todas** as rotas (`anyRequest().permitAll()`) e desliga CSRF, porque a PoC é um CRUD aberto para avaliação. A dependência `spring-boot-starter-security` está no `pom.xml` apenas para manter a cadeia de filtros configurável — não há login, token nem usuário.

## 6. Interface de terminal (CLI)

`cli/CadastroCli` implementa `CommandLineRunner` e é anotado com `@Profile("!test")`, então **não** sobe durante os testes. Ele oferece 6 opções (cadastrar/listar usuário e doação) e reaproveita `UsuarioService`/`DoacaoService` — a mesma regra da API, sem duplicação. A validação usa o `Validator` do Jakarta diretamente, para dar a mesma mensagem do `400`.

## 7. Configuração de execução

| Item | Valor | Onde |
|---|---|---|
| Porta HTTP | `8081` | `application.properties` |
| Mongo | `localhost:27017`, database `poc_doacoes` | `application.properties` |
| Container | `mongo:7.0`, `aepmongojava2026_mongo` | `compose.yaml` |
| Autostart do compose | `spring.docker.compose.enabled=true` | `application.properties` |

## 8. Testes

105 testes JUnit 5 com Mockito e MockMvc, sem banco real:

| Tipo | Classes | O que cobre |
|---|---|---|
| `@WebMvcTest` (slice) | `UsuarioControllerTest`, `DoacaoControllerTest` | status, `Location`, JSON, validação |
| Unitário | `UsuarioServiceTest`, `DoacaoServiceTest` | regras e mapeamento com repositório mockado |
| Unitário | `UsuarioMapperTest`, `DoacaoMapperTest` | conversões, listas nulas, atualização |
| Unitário | `GlobalExceptionHandlerTest` | 400 / 404 / 500 |
| Unitário | `CadastroCliTest` | menu do terminal, validações e caminhos de erro |
| Unitário | `ResponseDtosTest` | construtores, getters e setters dos DTOs de resposta |
| Unitário | `DemoDataSeederTest` | seed idempotente e tolerância a banco fora do ar |
| Unitário | `MongoPingHealthIndicatorTest` | health `UP` no ping e `DOWN` com a causa |

Cobertura medida pelo JaCoCo com gate de 70%, **sem exclusão de pacotes**: 98,8% de linhas (508/514) e 98,4% de branches (60/61). O relatório sai em `target/site/jacoco/index.html`. As 6 linhas descobertas são o `MongoUnavailableHandler` (3), trechos de `AepApplication` e `CadastroCli` (2+1), cujo efeito é verificado no ambiente publicado.

## 9. Decisões de projeto

| Decisão | Motivo |
|---|---|
| Referência por `usuarioId` (String), sem `@DBRef` | leitura simples, sem *lazy loading* implícito; a integridade é validada na aplicação |
| `Endereco` embutido em `Usuario` | endereço só existe no contexto do doador → documento aninhado, não coleção |
| DTO separado do modelo | não expor `ObjectId`/campos internos e permitir validação por endpoint |
| Mapper estático em vez de MapStruct | menos dependências para uma PoC de 3 integrantes |
| CLI e API no mesmo módulo | reaproveitamento direto dos serviços, sem duplicar regra |
