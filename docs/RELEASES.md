# Versões e entregas

Cada entrega da AEP é identificada por **tag anotada** no Git + **Release** no GitHub.
Este arquivo registra o que cada versão contém e como reproduzir a evidência de cobertura.

| Tag | Entrega | Commit base | Data | Conteúdo |
|---|---|---|---|---|
| `v1.0-entrega1` | 1ª entrega | `4574ae3` | 11/09/2026 | CRUD de `Usuario` e `Doacao`, CLI, `GET /resumo`, 55 testes, JaCoCo com gate de 70%, README |
| `v2.0-entrega2` | 2ª entrega | *(a definir no fechamento)* | — | Evolução da PoC, documentação técnica, quadro de tarefas, testes e cobertura finais |

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

**Escopo:** conclusão da PoC com documentação técnica, quadro de tarefas e cobertura revisada.

Já concluído nesta entrega:

| Item | Resultado |
|---|---|
| Documentação técnica | `docs/ARQUITETURA.md` + `docs/MODELAGEM.md` |
| Quadro de tarefas | `docs/KANBAN.md`, 29 issues e board público no GitHub Projects |
| Cobertura | **87 testes**, **99,3% de linhas** e **97,9% de branches**, sem excluir o pacote `cli` |

Ainda falta preencher no fechamento:

- Funcionalidades novas em relação à `v1.0-entrega1` (cards `B3`–`B7` em `docs/KANBAN.md`)
- Link do vídeo de demonstração (3 a 5 min)

**Tag:**

```powershell
git tag -a v2.0-entrega2 -m "AEP6S - 2a entrega: PoC final (ODS 2)"
git push origin Gabriel --tags
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
