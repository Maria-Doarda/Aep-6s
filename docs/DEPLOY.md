# Deploy — ambiente online (Render + MongoDB Atlas)

Guia para publicar a API e a tela do AEP6S em um endereço público, com banco na nuvem.
Depois deste passo a passo, o professor abre a URL no navegador e testa o CRUD sem instalar nada.

```
navegador ──HTTPS──► Render (container Docker)  ──►  MongoDB Atlas
                      Spring Boot + tela                database poc_doacoes
```

| Peça | Serviço | Plano usado |
|---|---|---|
| Aplicação (API + tela) | [Render](https://render.com) — Web Service com Docker | Free |
| Banco de dados | [MongoDB Atlas](https://www.mongodb.com/atlas) — cluster M0 | Free (512 MB) |
| Keep-alive (opcional) | [UptimeRobot](https://uptimerobot.com) | Free |

Tudo o que é código já está no repositório: `Dockerfile`, `.dockerignore`,
`application-prod.properties`, `DemoDataSeeder` (dados de exemplo) e `render.yaml`.
O que falta são as contas nos serviços — passos 1 a 3.

---

## 0. Antes de começar

- [ ] O trabalho precisa estar na branch **`main`**, porque é dela que o Render publica.
      Se você desenvolveu em outra branch, abra o Pull Request e faça o merge primeiro.
- [ ] Conta no GitHub com acesso ao repositório `Maria-Doarda/Aep-6s`.
- [ ] Conta no MongoDB Atlas e no Render (as duas têm plano gratuito, sem cartão).

---

## 1. MongoDB Atlas (15 min)

1. Crie a conta e um projeto em <https://cloud.mongodb.com>.
2. **Build a Database** → **M0 Free** → provedor **AWS** → região **São Paulo (sa-east-1)**.
3. Em **Security → Database Access**, crie um usuário:
   - usuário: `aep6s`
   - senha: gere uma forte e **guarde** (ela vai na string de conexão)
   - permissão: *Read and write to any database*
4. Em **Security → Network Access**, adicione o IP `0.0.0.0/0`
   (*Allow access from anywhere*). O Render não tem IP fixo no plano free.
5. Em **Database → Connect → Drivers**, copie a string e acrescente o nome do banco:

   ```
   mongodb+srv://aep6s:SENHA@cluster0.xxxxx.mongodb.net/poc_doacoes?retryWrites=true&w=majority
   ```

> Se a senha tiver `@`, `:`, `/` ou `#`, troque por um caractere seguro ou use a versão
> codificada (`%40`, `%3A`, `%2F`, `%23`). Esse é o erro mais comum na primeira tentativa.

Guarde a string: ela é a variável `MONGO_URI` do passo 2.

---

## 2. Render (20 min)

### Opção A — pelo painel (recomendada na primeira vez)

1. Entre em <https://dashboard.render.com> → **New +** → **Web Service**.
2. Conecte o GitHub e escolha o repositório `Maria-Doarda/Aep-6s`.
3. Preencha:

   | Campo | Valor |
   |---|---|
   | Name | `aep6s` (a URL será `https://aep6s.onrender.com`) |
   | Language / Runtime | **Docker** |
   | Branch | **main** |
   | Root Directory | *(vazio)* |
   | Dockerfile Path | `./Dockerfile` |
   | Instance Type | **Free** |
   | Region | **Ohio** (a mais próxima do Brasil entre as gratuitas) |

4. Em **Environment Variables**, adicione:

   | Chave | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod,demo` |
   | `MONGO_URI` | a string do passo 1 |

5. Em **Advanced → Health Check Path**, coloque `/actuator/health`.
6. **Create Web Service** e acompanhe o log. O primeiro build leva de 5 a 8 minutos
   (compila o projeto e baixa as dependências do Maven).

### Opção B — pelo blueprint

O arquivo `render.yaml` na raiz descreve o mesmo serviço. Em **New +** → **Blueprint**,
selecione o repositório e o Render pede apenas o valor de `MONGO_URI`.

### Como saber que subiu

O log termina com algo assim:

```
Tomcat started on port 8081
Started AepApplication in 8.2 seconds
Seed demo: 2 doadores e 3 doacoes criados.
```

E o painel mostra o estado **Live** com o endereço `https://<nome>.onrender.com`.

---

## 3. Validar (5 min)

Abra `https://<nome>.onrender.com` no navegador. A tela deve mostrar o resumo com
**3 doações / 23 itens / 3 tipos** — dados criados pelo perfil `demo` na primeira subida.

Pelo terminal (PowerShell):

```powershell
$base = "https://aep6s.onrender.com"

Invoke-RestMethod "$base/actuator/health"        # {"status":"UP"}
Invoke-RestMethod "$base/api/usuarios" | ConvertTo-Json -Depth 4
Invoke-RestMethod "$base/api/doacoes/resumo"     # totalDoacoes / totalQuantidade / itensDistintos

# criar um doador
Invoke-RestMethod "$base/api/usuarios" -Method Post -ContentType "application/json" -Body (@{
  nome = "Teste Professor"; email = "teste@exemplo.com";
  enderecos = @(@{ rua = "Rua A, 10"; cidade = "Maringá"; estado = "PR" })
} | ConvertTo-Json)

# validar o 400
Invoke-RestMethod "$base/api/usuarios" -Method Post -ContentType "application/json" -Body '{"nome":"X","email":"sem-arroba"}'
```

Checklist do que deve funcionar:

- [ ] a tela abre em `/` com os dados de exemplo
- [ ] cadastrar doador pela tela → aparece na tabela e no seletor de doações
- [ ] registrar doação → os três números do resumo mudam
- [ ] e-mail inválido → mensagem no campo, não erro genérico
- [ ] excluir doador/doação → linha some e o resumo recalcula
- [ ] `/actuator/health` responde `UP`

---

## 4. Keep-alive (5 min, opcional)

No plano free o Render hiberna depois de ~15 minutos sem acesso; a próxima visita
demora de 30 a 60 segundos. Para reduzir isso:

1. Crie a conta em <https://uptimerobot.com> → **Add New Monitor**.
2. Tipo **HTTP(s)**, URL `https://<nome>.onrender.com/actuator/health`, intervalo **5 minutos**.
3. Salve. Os pings mantêm a aplicação acordada durante o período de avaliação.

> Um monitor a cada 5 minutos consome ~8.600 horas de instância por mês; o plano free
> permite 750 horas de **execução**. Se o painel do Render reclamar do limite, aumente
> o intervalo para 10 minutos ou desative o monitor fora do período de correção.

---

## 5. Limites do plano gratuito

| Item | Limite |
|---|---|
| Memória da aplicação | 512 MB (a JVM sobe com `-XX:MaxRAMPercentage=70`) |
| Hibernação | após ~15 min sem requisições |
| Build | sem cache entre deploys (5 a 8 min) |
| Atlas M0 | 512 MB de armazenamento, ~500 conexões |
| Domínio | subdomínio `*.onrender.com`; domínio próprio exige cartão |

Se em algum momento a avaliação exigir disponibilidade total, o plano Starter do Render
(US$ 7/mês) elimina a hibernação sem mudar nada no código.

---

## 6. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| Build falha em `dependency:go-offline` | instabilidade na rede do build | no Render, **Manual Deploy → Clear build cache & deploy** |
| **Application failed to respond** (502) | aplicação não escutou na porta do Render | conferir `server.port=${PORT:8081}` em `application-prod.properties` |
| Log repete erro de conexão com o Mongo | IP não liberado ou string errada | Atlas → Network Access `0.0.0.0/0`; conferir usuário, senha e `/poc_doacoes` no fim da URI |
| Health check falha e o deploy não ativa | `/actuator/health` responde `DOWN` (Mongo inacessível) | corrigir a conexão do Atlas; o Render só promove o deploy com health `UP` |
| Página abre, mas sem estilo | cache do navegador | recarregar com `Ctrl+F5` |
| Tela vazia e "API indisponível" | API hibernou ou caiu | esperar ~1 min e clicar em **Tentar novamente** |
| Dados de exemplo não aparecem | o banco já tinha registros (o seed só roda em banco vazio) | Atlas → **Browse Collections** → apagar o database `poc_doacoes` e reiniciar o serviço |
| Quero começar do zero | — | apagar o database no Atlas e usar **Manual Deploy → Deploy latest commit** |

---

## 7. Atualizar o que está no ar

Com `autoDeploy: true`, todo push na `main` gera um deploy novo. Para atualizar:

```bash
git checkout main
git pull origin main
# ... alterações ...
git push origin main
```

Acompanhe em **Render → Logs**. Se o build novo falhar, o Render mantém a versão anterior
no ar até você corrigir.

---

## 8. Rodar o container localmente (opcional)

Útil para testar o mesmo artefato que vai para o Render:

```bash
docker build -t aep6s .
docker run --rm -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=prod,demo \
  -e MONGO_URI="mongodb://host.docker.internal:27017/poc_doacoes" \
  aep6s
```

Depois abra `http://localhost:8081`. No Linux, troque `host.docker.internal` pelo IP
do host ou use `--network host`.
