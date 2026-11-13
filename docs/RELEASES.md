# Versões e entregas

Cada entrega da AEP é identificada por **tag anotada** no Git + **Release** no GitHub.
Este arquivo registra o que cada versão contém e como reproduzir a evidência de cobertura.

| Tag | Entrega | Commit base | Data | Conteúdo |
|---|---|---|---|---|
| `v1.0-entrega1` | 1ª entrega | `4574ae3` | 11/09/2026 | CRUD de `Usuario` e `Doacao`, CLI, `GET /resumo`, 55 testes, JaCoCo com gate de 70%, README |
| `v2.0-entrega2` | 2ª entrega | commit do fechamento (a própria tag) | 13/11/2026 | PoC final: tela web, ambiente online, documentação técnica, quadro de tarefas, 93 testes e 98,7% de cobertura |

---

## v1.0-entrega1

**Escopo:** primeira versão funcional da PoC (ODS 2 — Fome Zero).

- API REST em `http://localhost:8081` com CRUD completo de usuários e doações
- MongoDB 7.0 (`poc_doacoes`) com duas coleções e subdocumento `enderecos[]`
- `GET /api/doacoes/resumo` (total de doações, quantidade e itens distintos)
- Menu interativo no terminal (cadastro e listagem sem HTML)
- Testes: **55** (JUnit + Mockito + MockMvc)
- Cobertura: **95,2% de linhas** (excluindo o pacote `cli`)

**Evidência de cobertura:**

```powershell
cd aep/aep
.\mvnw.cmd clean verify
# [INFO] Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
# [INFO] All coverage checks have been met.
# [INFO] BUILD SUCCESS
# relatório: target/site/jacoco/index.html
```

---

## v2.0-entrega2

**Escopo:** conclusão da PoC (ODS 2 — Fome Zero) com interface web, ambiente publicado,
documentação técnica e quadro de tarefas.

**Ambiente online:** <https://aep-6s-doacao-de-alimentos.onrender.com> — health check em
`/actuator/health` respondendo `{"status":"UP"}`.

### O que entrou nesta entrega

| Área | Resultado |
|---|---|
| Interface web | painel em `/` (resumo, doadores com endereços dinâmicos, doações) e página pública em `/doar.html` (4 etapas) — HTML, CSS e JS puros, sem build e sem CORS |
| Ambiente publicado | `Dockerfile` multi-stage, perfil `prod`, seed `demo`, `render.yaml`, MongoDB Atlas M0 e `docs/DEPLOY.md` |
| Robustez | health check próprio (`MongoPingHealthIndicator`) e `MongoUnavailableHandler` devolvendo 503 com `ProblemDetail` quando o banco cai |
| Documentação técnica | `docs/ARQUITETURA.md` (camadas, fluxo, erros) e `docs/MODELAGEM.md` (coleções, JSON, índices) |
| Gestão | `docs/KANBAN.md` + quadro no GitHub Projects, com 29 issues e histórico de movimentação |
| Qualidade | **93 testes**, **98,7% de linhas** (463/469) e **98,0% de branches** (50/51), sem excluir pacotes |

### Funcionalidades novas em relação à `v1.0-entrega1`

| | `v1.0-entrega1` | `v2.0-entrega2` |
|---|---|---|
| Interface | menu no terminal | painel web + página pública de doação |
| Execução | `localhost` | publicado no Render, dados no Atlas |
| Testes | 55 | **93** |
| Cobertura de linhas | 95,2% (sem o pacote `cli`) | **98,7%** (todos os pacotes) |
| Documentação | README | + arquitetura, modelagem, deploy, releases e kanban |
| Operação | — | health check, handler de 503, `compose.yaml` com healthcheck e `restart` |

### Stack

Java 17 · Spring Boot 4.1.0 · Spring Data MongoDB · Bean Validation · Actuator · Lombok ·
Maven Wrapper · JUnit 5 · Mockito · MockMvc · JaCoCo 0.8.16 · MongoDB 7.0 (Atlas M0) ·
Docker multi-stage · Render · HTML/CSS/JS sem framework.

### Evidência de cobertura

```powershell
cd aep/aep
.\mvnw.cmd clean verify
# Tests run: 93, Failures: 0, Errors: 0, Skipped: 0
# All coverage checks have been met.
# BUILD SUCCESS
# relatório: target/site/jacoco/index.html
```

### Vídeo de demonstração (3 a 5 min)

A preencher no fechamento: link do vídeo com a evolução da solução, o projeto final, a
demonstração da PoC, as tecnologias, os testes com cobertura e os resultados alcançados.

### Fora do escopo desta versão

Continuam no backlog do `docs/KANBAN.md` (B3–B6): `resumo` por aggregation pipeline,
índices compostos, filtros combinados em `GET /api/doacoes` e validação da existência do
`usuarioId` ao criar doação.

**Tag:**

```powershell
git tag -a v2.0-entrega2 -m "AEP6S - 2a entrega: PoC final (ODS 2)"
git push origin --tags
```

---

## Como publicar a Release no GitHub

1. `git push origin Gabriel --tags`
2. GitHub → **Releases** → **Draft a new release** → escolher a tag (`v1.0-entrega1`, depois `v2.0-entrega2`)
3. Título: `AEP6S — 1ª entrega (PoC ODS 2)` / `AEP6S — 2ª entrega (PoC final)`
4. Descrição: copiar a seção correspondente deste arquivo
5. Anexar/comentar o link do vídeo de demonstração

---

## Histórico de tags

```bash
git tag -l -n1
```

| Tag | Criada em | Commit |
|---|---|---|
| `v1.0-entrega1` | 11/11/2026 | `4574ae3` — Merge PR #4 (estado entregue na 1ª entrega) |
