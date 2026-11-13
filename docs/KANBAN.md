# Quadro de tarefas — AEP6S (ODS 2)

> Metodologia de trabalho da equipe. Cada card é uma tarefa executada ou prevista, com **evidência** (branch/commit) e **data**.
> Atualizado em **11/11/2026**. Equipe: Gabriel de Oliveira Gnoatto, Maria Eduarda Pereira Ribeiro, Sophia Machado Silva.

**Colunas:** `Backlog` → `A fazer` → `Em andamento` → `Concluído`
Cards concluídos são empilhados na coluna mais à direita, do mais antigo para o mais recente.

---

## Quadro no GitHub

Este arquivo é a **fonte versionada** do quadro. O espelho navegável é o GitHub Projects:

**Board:** https://github.com/users/GnoattoGabriel/projects/1

Cada card também existe como **issue** no repositório, com label e milestone:

| Card | Issue | Card | Issue | Card | Issue |
|---|---|---|---|---|---|
| C1 | [#6](https://github.com/Maria-Doarda/Aep-6s/issues/6) | C11 | [#16](https://github.com/Maria-Doarda/Aep-6s/issues/16) | A1 | [#24](https://github.com/Maria-Doarda/Aep-6s/issues/24) |
| C2 | [#7](https://github.com/Maria-Doarda/Aep-6s/issues/7) | C12 | [#17](https://github.com/Maria-Doarda/Aep-6s/issues/17) | A2 | [#25](https://github.com/Maria-Doarda/Aep-6s/issues/25) |
| C3 | [#8](https://github.com/Maria-Doarda/Aep-6s/issues/8) | C13 | [#18](https://github.com/Maria-Doarda/Aep-6s/issues/18) | B1 | [#26](https://github.com/Maria-Doarda/Aep-6s/issues/26) |
| C4 | [#9](https://github.com/Maria-Doarda/Aep-6s/issues/9) | C14 | [#19](https://github.com/Maria-Doarda/Aep-6s/issues/19) | B2 | [#27](https://github.com/Maria-Doarda/Aep-6s/issues/27) |
| C5 | [#10](https://github.com/Maria-Doarda/Aep-6s/issues/10) | C15 | [#20](https://github.com/Maria-Doarda/Aep-6s/issues/20) | B3 | [#28](https://github.com/Maria-Doarda/Aep-6s/issues/28) |
| C6 | [#11](https://github.com/Maria-Doarda/Aep-6s/issues/11) | C16 | [#21](https://github.com/Maria-Doarda/Aep-6s/issues/21) | B4 | [#29](https://github.com/Maria-Doarda/Aep-6s/issues/29) |
| C7 | [#12](https://github.com/Maria-Doarda/Aep-6s/issues/12) | C17 | [#22](https://github.com/Maria-Doarda/Aep-6s/issues/22) | B5 | [#30](https://github.com/Maria-Doarda/Aep-6s/issues/30) |
| C8 | [#13](https://github.com/Maria-Doarda/Aep-6s/issues/13) | C18 | [#23](https://github.com/Maria-Doarda/Aep-6s/issues/23) | B6 | [#31](https://github.com/Maria-Doarda/Aep-6s/issues/31) |
| C9 | [#14](https://github.com/Maria-Doarda/Aep-6s/issues/14) | | | | |
| C10 | [#15](https://github.com/Maria-Doarda/Aep-6s/issues/15) | | | B8 | [#33](https://github.com/Maria-Doarda/Aep-6s/issues/33) |
| C19 | [#24](https://github.com/Maria-Doarda/Aep-6s/issues/24) | | | B9 | [#34](https://github.com/Maria-Doarda/Aep-6s/issues/34) |
| C20 | [#27](https://github.com/Maria-Doarda/Aep-6s/issues/27) | | | C21 | [#36](https://github.com/Maria-Doarda/Aep-6s/issues/36) |
| C23 | [#37](https://github.com/Maria-Doarda/Aep-6s/issues/37) | | | | |

**Labels:** `feat` (funcionalidade) · `test` (testes e cobertura) · `docs` · `chore` (infra/build)
**Milestones:** `Entrega 1` (fechado) · `Entrega 2` (aberto)

> O projeto é da conta `GnoattoGabriel` porque o repositório pertence a outra conta (`Maria-Doarda`) e o GitHub não permite vincular um Project a um repositório de dono diferente. O link acima é público e abre para qualquer pessoa.

---

## Concluído

| # | Card | Responsável | Data | Evidência |
|---|---|---|---|---|
| C1 | Estrutura inicial do projeto (Spring Boot 4.1.0 + Java 17 + MongoDB) | Gabriel | 01/09 | `bc45da5` |
| C2 | Modelagem das coleções `usuarios` e `doacoes` + subdocumento `enderecos[]` | Gabriel | 01/09 | `c8e462b` |
| C3 | CRUD de usuários com DTO, mapper e validação | Gabriel | 02/09 | `182c2a6` |
| C4 | CRUD de doações + vínculo `usuarioId` | Gabriel | 02/09 | `182c2a6` |
| C5 | Tratamento de erros (400/404) com `@RestControllerAdvice` | Gabriel | 02/09 | `182c2a6` |
| C6 | README com problema, ODS 2, tecnologias e instruções de execução | Gabriel | 02/09 | `586be46`, `931fa7b` |
| C7 | Primeiros testes automatizados (8 verdes) | Sophia | 03/09 | `b5e810b` |
| C8 | Suíte completa de testes (JUnit + Mockito + MockMvc) | Sophia | 07/09 | `dafa549` |
| C9 | JaCoCo configurado com gate mínimo de 70% | Sophia | 07/09 | `dafa549` |
| C10 | Cadastro via terminal (CLI) além da API | Gabriel | 10/09 | `cae3bc1`, `2cedea3` |
| C11 | Endpoint `GET /api/doacoes/resumo` com teste | Gabriel | 10/09 | `baed42e` |
| C12 | README final da 1ª entrega (badges, tabela de endpoints, problemas comuns) | Gabriel | 10/09 | `e73ee7e`, `2817694` |
| C13 | API na porta 8081 + variável `MONGO_INITDB_DATABASE` corrigida | Gabriel | 11/09 | `4f86a78`, `a5d4e55` |
| C14 | **1ª entrega concluída** (código + README + 55 testes + 95% de cobertura + vídeo) | Equipe | 11/09 | tag `v1.0-entrega1` |
| C15 | Sincronizar `main` remota com o branch local `Gabriel` (`git pull` + fast-forward) | Gabriel | 11/11 | `4574ae3` |
| C16 | Criar quadro de tarefas versionado (metodologia de trabalho) | Gabriel | 11/11 | `docs/KANBAN.md` |
| C17 | Limpeza do repositório: remover `target/` e `.idea/` do versionamento, `.gitignore` na raiz, remover `HELP.md` e `TODO.md` obsoleto | Gabriel | 11/11 | commit `chore: limpa repositorio` |
| C18 | Documentar versões e releases (`v1.0-entrega1`) | Gabriel | 11/11 | `docs/RELEASES.md`, tag `v1.0-entrega1` |
| C19 | Documentação técnica completa — `docs/ARQUITETURA.md` (camadas, fluxo, erros) e `docs/MODELAGEM.md` (coleções, JSON, índices) *(era A1)* | Gabriel | 11/11 | `docs/ARQUITETURA.md`, `docs/MODELAGEM.md` |
| C20 | Cobertura com o pacote `cli` incluído: `CadastroCli` com I/O injetável, `CadastroCliTest` (24 testes) e `ResponseDtosTest`; exclusão removida do `pom.xml` *(era B2)* | Gabriel | 11/11 | 87 testes, **99,3% de linhas**, 97,9% de branches |
| C21 | Tela web servida pela API (dashboard, doadores e doações) + pacote de deploy: `Dockerfile`, `.dockerignore`, perfil `prod`, actuator, seed `demo`, `render.yaml` e `docs/DEPLOY.md` | Gabriel | 11/11 | `src/main/resources/static/`, `Dockerfile`, `render.yaml`, `docs/DEPLOY.md` · 91 testes, **99,3% de linhas** |
| C22 | Corrigir a conexão do Mongo no Spring Boot 4: o prefixo virou `spring.mongodb` (o antigo `spring.data.mongodb` era ignorado e a app caía no banco `test`/`localhost`, o que derrubaria o deploy) + `.env`/`.env.example` versionados | Gabriel | 11/11 | `application.properties`, `application-prod.properties`, `.gitignore`, `.env.example`; conexão validada com o Atlas (seed 2/3 e resumo 3/23/3) |
| C23 | Publicar o ambiente online (Atlas + Render) e validar o endereço público *(era A3)* | Gabriel | 12/11 | `https://aep-6s-doacao-de-alimentos.onrender.com` — health `UP`, resumo 3/23/3, POST 201 com `Location`, 400 por campo, 404, DELETE 204 idempotente e assets 200 |
| C24 | Robustez do banco: health check próprio, handler de 503 e `compose.yaml` com `healthcheck`/`restart` *(era B8)* | Gabriel · Maria | 12/11 | `MongoPingHealthIndicator` + teste, `MongoUnavailableHandler`, `compose.yaml` (PR #41) |
| C25 | Interface web final: painel com menu lateral e página pública de doação em 4 etapas | Sophia | 13/11 | `index.html`, `doar.html`, `doar.js`, `doar.css` (PR #42) |
| C26 | Fechar a 2ª entrega: números finais, tag `v2.0-entrega2` e Release no GitHub *(era B1)* | Gabriel | 13/11 | 93 testes · 98,7% de linhas · tag `v2.0-entrega2` · Release |

---

## Em andamento

| # | Card | Responsável | Início | O que falta |
|---|---|---|---|---|
| A2 | Vídeo da 2ª entrega (3 a 5 min) | Equipe | — | roteiro cronometrado, gravação e publicação |

---

## A fazer

| # | Card | Prioridade | Critério da AEP atendido |
|---|---|---|---|
| B3 | `resumo` via aggregation pipeline (`$group`) + endpoint `resumo/por-item` | Média | Uso efetivo de NoSQL |
| B4 | Índices (`@Indexed` em `usuarioId`/`item`, `@CompoundIndex`) | Média | Modelagem NoSQL |
| B5 | Filtros combinados em `GET /api/doacoes` (`item`, `usuarioId`, `dataInicio`) | Média | Evolução funcional |
| B6 | Validar existência do `usuarioId` ao criar doação (hoje aceita doação órfã) | Média | Relacionamento entre coleções |
| B9 | Fixar a versão do JDK no `.mvn/jvm.config` para build reproduzível | Baixa | Build reproduzível |

---

## Backlog (ideias)

- Paginação e ordenação em `GET /api/usuarios` e `GET /api/doacoes`
- Relatório de doações por período
- Autenticação simples para doadores
- Editar doador e doação pela tela (a API já expõe `PUT`)

---

## Histórico do quadro

| Data | Movimentação |
|---|---|
| 01/09–07/09 | C1–C9 criados e concluídos (estrutura, CRUD, erros, README, testes, JaCoCo) |
| 10/09 | C10–C12 concluídos (CLI, `/resumo`, README final) |
| 11/09 | C13 e C14 concluídos — **1ª entrega entregue** |
| 11/11 | C15–C18 concluídos; A1–A2 em andamento; B1–B9 priorizados a partir da análise da 2ª entrega |
| 11/11 | **A1 → C19** (documentação técnica) e **B2 → C20** (cobertura com o `cli`): 87 testes, 99,3% de linhas. Restam A2 (vídeo) e B1, B3–B9 |
| 11/11 | **C21**: tela web + pacote de deploy prontos (Docker, perfil `prod`, seed `demo`, `render.yaml`, `docs/DEPLOY.md`). Abre **A3** (publicar no Render) e sai do backlog o seed de demonstração |
| 11/11 | **C22**: no Spring Boot 4 o prefixo do Mongo é `spring.mongodb`; as propriedades antigas eram ignoradas e a app gravava no banco `test`. Corrigido nos dois arquivos de configuração e validado contra o Atlas. `.env` fica fora do Git, `.env.example` versionado |
| 11/11 | **Merge da `main`** no `Gabriel` antes do PR: a equipe subiu o JaCoCo **0.8.16**, que elimina os avisos de `major version 69` do JDK 25 — **#34 resolvida**. `clean verify` segue com 91 testes e cobertura aprovada |
| 12/11 | **A3 → C23**: ambiente publicado e validado ponta a ponta no Render (health `UP` após a correção do health check). **B8 → C24**: `MongoPingHealthIndicator`, `MongoUnavailableHandler` e `compose.yaml` com `healthcheck`/`restart` (PR #41) |
| 13/11 | **C25**: interface web final no ar — painel com menu lateral e página pública de doação em 4 etapas (PR #42). **B1 → C26**: números finais (93 testes, 98,7% de linhas), tag `v2.0-entrega2` e Release publicada |

---

## Como atualizar

1. Ao começar uma tarefa, mova o card para **Em andamento** e preencha a data de início.
2. Ao terminar, mova para **Concluído** e cole a evidência (commit, PR ou link do vídeo).
3. Tarefas novas entram em **A fazer** com prioridade e o critério da AEP que elas atendem.
4. O quadro espelha o progresso real: `git log --oneline` deve conversar com a coluna **Concluído**.

> Espelho opcional no GitHub: **Projects → Board** com as mesmas três colunas, importando estes cards. O quadro deste arquivo é a fonte versionada da verdade.
