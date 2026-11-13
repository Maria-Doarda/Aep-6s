# Modelagem do banco — MongoDB `poc_doacoes`

Como o banco NoSQL foi modelado, por que, e como atende aos requisitos do **2º semestre** (múltiplas coleções, relacionamento entre coleções e documentos complexos).

Complementa [`ARQUITETURA.md`](ARQUITETURA.md).

## 1. Coleções

| Coleção | Documento (classe Java) | Tipo | Papel |
|---|---|---|---|
| `usuarios` | `models/Usuario` | documento com **subdocumentos** | doador (pessoa ou estabelecimento) |
| `doacoes` | `models/Doacao` | documento simples | doação de um item, vinculada a um usuário |

O MongoDB cria as coleções no primeiro `save` — não há script de inicialização.

## 2. Documento `usuarios` (objeto complexo)

O usuário **não** tem tabela de endereços separada: a lista de subdocumentos vive dentro do documento. É o caso clássico de "dados que só fazem sentido junto do pai".

```json
{
  "_id": "670f1c2a9b3e4d5f6a7b8c90",
  "nome": "Ana Silva",
  "email": "ana@teste.com",
  "enderecos": [
    {
      "rua": "Rua A, 123",
      "cidade": "Maringá",
      "estado": "PR"
    },
    {
      "rua": "Av. Brasil, 456",
      "cidade": "Maringá",
      "estado": "PR"
    }
  ]
}
```

| Campo | Tipo | Regra |
|---|---|---|
| `_id` | ObjectId | gerado pelo Mongo |
| `nome` | string | obrigatório, até 100 caracteres |
| `email` | string | obrigatório e formato de e-mail |
| `enderecos` | **array de subdocumentos** | pode ser vazio; cada item tem `rua`, `cidade`, `estado` (UF com 2 letras) |

Classe: `models/Usuario` (`@Document(collection = "usuarios")`) + `models/Endereco` (sem anotação — é subdocumento).

## 3. Documento `doacoes` (com referência)

```json
{
  "_id": "670f1c2a9b3e4d5f6a7b8c91",
  "usuarioId": "670f1c2a9b3e4d5f6a7b8c90",
  "item": "Arroz",
  "quantidade": 10,
  "data_doacao": "2026-09-02"
}
```

| Campo | Tipo | Regra |
|---|---|---|
| `usuarioId` | string | **referência** ao `_id` de `usuarios` |
| `item` | string | obrigatório (`@NotBlank`) |
| `quantidade` | int | maior que zero (`@Positive`) |
| `data_doacao` | data (`LocalDate`) | sem `@NotNull` no DTO; o CLI preenche com a data de hoje quando o campo vem vazio. Nome no banco em `snake_case` via `@Field("data_doacao")` |

## 4. Relacionamento entre coleções

```
usuarios                          doacoes
┌───────────────────────┐         ┌───────────────────────┐
│ _id: ObjectId  ◄──────┼─────────┤ usuarioId: String     │
│ nome, email           │  1 : N  │ item, quantidade      │
│ enderecos[ ... ]      │         │ data_doacao           │
└───────────────────────┘         └───────────────────────┘
```

- **Referência por valor** (`usuarioId` como string), **não** `@DBRef`.
- Consulta: `DoacaoRepository.findByUsuarioId(String)` → `GET /api/doacoes/usuario/{usuarioId}`.
- Motivo da escolha: `@DBRef` obriga uma segunda ida ao banco por documento e complica a serialização; para uma PoC com leitura majoritária, guardar o ID e resolver na aplicação é mais simples e previsível.
- **Ponto de melhoria conhecido:** hoje a API aceita `usuarioId` inexistente (doação órfã). A validação no `DoacaoService` está no backlog (issue #31).

## 5. Como atende ao 2º semestre

| Requisito | Evidência no projeto |
|---|---|
| Múltiplas coleções | `usuarios` e `doacoes` |
| Relacionamento entre coleções | `doacoes.usuarioId` → `usuarios._id`, com endpoint de listagem por usuário |
| Coleção com objetos complexos | `usuarios.enderecos[]`: lista de subdocumentos aninhados |
| CRUD | POST/GET/PUT/DELETE nos dois recursos |

## 6. Operações por endpoint

| Endpoint | Operação no Mongo |
|---|---|
| `POST /api/usuarios` | `insertOne` em `usuarios` |
| `GET /api/usuarios` | `find({})` |
| `GET /api/usuarios/{id}` | `findOne({_id})` |
| `PUT /api/usuarios/{id}` | `findOne` + `save` (substitui o documento, inclusive `enderecos`) |
| `DELETE /api/usuarios/{id}` | `deleteOne({_id})` |
| `POST /api/doacoes` | `insertOne` em `doacoes` |
| `GET /api/doacoes` | `find({})` |
| `GET /api/doacoes/{id}` | `findOne({_id})` |
| `GET /api/doacoes/usuario/{usuarioId}` | `find({usuarioId})` |
| `GET /api/doacoes/resumo` | aggregation `$group`: total de doações, soma das quantidades e itens distintos (`$addToSet` + `$size`) |
| `GET /api/doacoes/resumo/por-item` | aggregation `$group` por `item` + `$sort` pela quantidade |
| `PUT` / `DELETE /api/doacoes/{id}` | `save` / `deleteOne` |

> Os dois resumos rodam no servidor MongoDB (`aggregation pipeline`), sem trazer os documentos
> para a aplicação. Antes o cálculo era feito em memória com `findAll()`.

## 7. Índices

Criados automaticamente na subida da aplicação (`spring.data.mongodb.auto-index-creation=true`)
a partir das anotações de `Doacao`:

| Índice | Campos | Origem | Serve para |
|---|---|---|---|
| `_id_` | `doacoes._id` | MongoDB | busca por id |
| `usuarioId` | `doacoes.usuarioId` | `@Indexed` | `findByUsuarioId` e validação do doador |
| `item` | `doacoes.item` | `@Indexed` | filtros e agrupamento por item |
| `usuario_data` | `doacoes.usuarioId` + `doacoes.data_doacao` (desc) | `@CompoundIndex` | doações de um doador em ordem cronológica |

Ainda no backlog: `email_1` único em `usuarios.email` (impedir e-mail duplicado) e
`data_doacao_1` para relatórios por período.

## 8. Inspeção rápida do banco

```bash
docker exec -it aepmongojava2026_mongo mongosh

use poc_doacoes
db.usuarios.find().pretty()
db.doacoes.find().pretty()
db.usuarios.findOne({ "enderecos.cidade": "Maringá" })   # consulta em subdocumento
db.doacoes.aggregate([
  { $group: { _id: "$item", total: { $sum: "$quantidade" } } },
  { $sort: { total: -1 } }
])
```
