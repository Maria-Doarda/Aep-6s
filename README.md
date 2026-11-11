# AEP6S — ODS 2 | CRUD de Doação de Alimentos

**Alunos:** Sophia Machado Silva (24087451-2) • Gabriel de Oliveira Gnoatto (23298801-2) • Maria Eduarda Pereira Ribeiro (24224683-2)

![Java 17](https://img.shields.io/badge/Java-17-blue)
![Spring Boot 4.1.0](https://img.shields.io/badge/Spring%20Boot-4.1.0-green)
![MongoDB 7.0](https://img.shields.io/badge/MongoDB-7.0-green)
![Tests passing](https://img.shields.io/badge/tests-87%20passing-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-99%25%20lines-brightgreen)
![Build](https://img.shields.io/badge/build-mvn%20clean%20verify-blue)

> Spring Boot 4.1.0 + Java 17 + MongoDB (`poc_doacoes`) — API REST para cadastrar doadores (`Usuario`) e doações de alimentos (`Doacao`).

---

## Problema, público e ODS 2

O desperdício de alimentos convive com a insegurança alimentar nos centros urbanos: sobra comida de um lado e falta do outro por falta de um canal simples de doação.

O projeto atende doadores (pessoas e estabelecimentos) e ONGs e bancos de alimentos que recebem e redistribuem as doações, apoiando o ODS 2 (Fome Zero) nas metas 2.1 e 2.2. O `GET /api/doacoes/resumo` acompanha os indicadores: total de doações, quantidade e itens distintos.

---

## O que o projeto faz

- **Usuários (doadores):** criar, listar, buscar, atualizar e remover. Criar retorna 201 com o endereço do novo recurso; atualizar retorna 200 ou 404; remover retorna 204 e pode ser repetida sem erro.
- **Doações:** criar, listar, buscar por ID, listar por usuário e ver o resumo. Mesmos códigos da lista acima.
- **Terminal (sem HTML):** menu interativo no console para cadastrar e listar, rodando junto com a API.
- Dados inválidos retornam 400, IDs inexistentes retornam 404. Não precisa de login.

Módulo Maven: `aep/aep` | API: `http://localhost:8081` | Mongo: `poc_doacoes` em `localhost:27017`

---

## Pré-requisitos

| Ferramenta | Versão | Como conferir |
|---|---|---|
| Java JDK | 17 ou superior | `java -version` |
| Git | qualquer | `git --version` |
| Docker Desktop | com `docker compose` v2 | `docker compose version` |
| curl (ou Postman) | qualquer | `curl --version` |

Não precisa instalar Maven: o projeto traz o Maven Wrapper (`aep/aep/mvnw` e `mvnw.cmd`).

---

## Como rodar

Na pasta `aep/aep`:

```bash
docker compose up -d
docker ps --filter name=aepmongojava2026_mongo   # STATUS deve ser Up
```

```powershell
# Windows
.\mvnw.cmd compile -DskipTests
.\mvnw.cmd spring-boot:run
```

```bash
# Mac/Linux
./mvnw compile -DskipTests
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8081`. Deixe esse terminal aberto. Na primeira vez o download das dependências demora; depois fica rápido.

---

## Testando a API

Em outro terminal:

```bash
# 1. Criar usuário (copie o id da resposta)
curl -X POST http://localhost:8081/api/usuarios \
  -H "Content-Type: application/json" \
  -d "{\"nome\":\"Ana Silva\",\"email\":\"ana@teste.com\",\"enderecos\":[{\"rua\":\"Rua A, 123\",\"cidade\":\"Maringa\",\"estado\":\"PR\"}]}"

# 2. Criar doação (troque SEU_ID_AQUI pelo id copiado)
curl -X POST http://localhost:8081/api/doacoes \
  -H "Content-Type: application/json" \
  -d "{\"usuarioId\":\"SEU_ID_AQUI\",\"item\":\"Arroz\",\"quantidade\":10,\"dataDoacao\":\"2026-09-02\"}"

# 3. Consultas
curl http://localhost:8081/api/usuarios
curl http://localhost:8081/api/doacoes
curl http://localhost:8081/api/doacoes/usuario/SEU_ID_AQUI
curl http://localhost:8081/api/doacoes/resumo
# esperado: {"totalDoacoes":1,"totalQuantidade":10,"itensDistintos":1}
```

Dá para testar também pelo menu do terminal (opções 1 a 5) ou pelo navegador/Postman, sem autenticação.

Para parar: `Ctrl + C` no terminal da aplicação e `docker compose down` (adicione `-v` para apagar os dados).

---

## Tabela de endpoints

| Método | URL | Status | Descrição |
|---|---|---|---|
| `POST` | `/api/usuarios` | `201 + Location` | Cria usuário |
| `GET` | `/api/usuarios` | `200` | Lista todos |
| `GET` | `/api/usuarios/{id}` | `200` / `404` | Busca por ID |
| `PUT` | `/api/usuarios/{id}` | `200` / `404` | Atualiza |
| `DELETE` | `/api/usuarios/{id}` | `204` | Remove |
| `POST` | `/api/doacoes` | `201 + Location` | Cria doação |
| `GET` | `/api/doacoes` | `200` | Lista todas |
| `GET` | `/api/doacoes/resumo` | `200` | Total, quantidade e itens distintos |
| `GET` | `/api/doacoes/{id}` | `200` / `404` | Busca por ID |
| `GET` | `/api/doacoes/usuario/{usuarioId}` | `200` | Lista por usuário |
| `PUT` | `/api/doacoes/{id}` | `200` / `404` | Atualiza |
| `DELETE` | `/api/doacoes/{id}` | `204` | Remove |

---

## Documentação técnica

| Documento | Conteúdo |
|---|---|
| [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) | camadas, fluxo de uma requisição, tratamento de erros, segurança, CLI e decisões de projeto |
| [`docs/MODELAGEM.md`](docs/MODELAGEM.md) | coleções `usuarios` e `doacoes`, documentos JSON reais, relacionamento por `usuarioId`, subdocumento `enderecos[]` e índices |
| [`docs/KANBAN.md`](docs/KANBAN.md) | quadro de tarefas e evolução do trabalho |
| [`docs/RELEASES.md`](docs/RELEASES.md) | versões, tags e comando reproduzível da cobertura |

---

## Metodologia e quadro de tarefas

O trabalho foi organizado por um quadro Kanban versionado junto ao código: cada card tem responsável, data e evidência (commit/PR). O histórico do quadro mostra a evolução da 1ª para a 2ª entrega.

**Quadro:** [`docs/KANBAN.md`](docs/KANBAN.md) — colunas *A fazer · Em andamento · Concluído*, com backlog priorizado pelos critérios da AEP.
**Board navegável:** [github.com/users/GnoattoGabriel/projects/1](https://github.com/users/GnoattoGabriel/projects/1) — cada card é uma issue do repositório, com label e milestone.

Também usamos branches por integrante (`Gabriel`, `Sophiaa`, `main`) com Pull Requests para integrar cada etapa.

---

## Versões e entregas

| Tag | Entrega | O que contém |
|---|---|---|
| `v1.0-entrega1` | 1ª entrega | CRUD de usuários e doações, CLI, `GET /resumo`, 55 testes, cobertura ≥ 70%, README |
| `v2.0-entrega2` | 2ª entrega | PoC final, documentação técnica, quadro de tarefas e cobertura revisada |

Detalhes, notas de release e comandos para reproduzir a cobertura: [`docs/RELEASES.md`](docs/RELEASES.md). Para listar as tags: `git tag -l -n1`.

---

## Testes e cobertura

```bash
# na pasta aep/aep
.\mvnw.cmd clean verify   # Windows — 87 testes, exige cobertura mínima de 70%
./mvnw clean verify        # Mac/Linux
```

Testes JUnit com Mockito e MockMvc (`src/test`: controllers, services, mappers, handler e CLI). A cobertura usa JaCoCo sobre **todo** o código (nenhum pacote excluído) e o gate mínimo é de 70% de linhas.

| Métrica | Valor |
|---|---|
| Testes | **87** |
| Linhas cobertas | **99,3%** (412/415) |
| Branches cobertos | **97,9%** (46/47) |

O relatório sai em `aep/aep/target/site/jacoco/index.html` e o log mostra `All coverage checks have been met`.

---

## Problemas comuns

| Erro | Solução |
|---|---|
| `MongoTimeoutException` em `localhost:27017` | `docker compose up -d` e confira com `docker ps` |
| Porta `27017` ocupada | `docker compose down` ou pare o outro Mongo |
| `java -version` mostra 8 ou 11 | Instale o JDK 17 e abra um novo terminal |
| `400` ao criar / `404` ao buscar | Confira o JSON (`email` com `@`, `quantidade > 0`) e use um ID listado no `GET` |

---

## Estrutura do projeto

```
aep/aep/
├── pom.xml / compose.yaml / mvnw           # build, Mongo 7.0, wrapper
├── src/main/resources/application.properties
├── src/test/java/fz/exemple/aep/           # testes (controllers, services, mappers, handler)
└── src/main/java/fz/exemple/aep/
    ├── controllers/ services/ repositories/
    ├── models/ (Usuario, Doacao, Endereco)
    ├── dto/ mapper/ cli/ config/ exception/
    └── AepApplication.java

docs/
├── KANBAN.md       # quadro de tarefas e metodologia
├── RELEASES.md     # versões, tags e evidência de cobertura
├── ARQUITETURA.md  # camadas, fluxo de requisição, erros e testes
└── MODELAGEM.md    # coleções, documentos JSON, relacionamento e índices
```
