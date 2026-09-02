# AEP6S — ODS 2 | CRUD de Doação de Alimentos

> Spring Boot 4.1.0 + Java 17 + MongoDB (`poc_doacoes`) — API REST para cadastrar doadores (`Usuario`) e doações de alimentos (`Doacao`).

Repositório: **https://github.com/Maria-Doarda/Aep-6s** — branch deste README: **`Gabriel`**

---

## O que o projeto faz

- **Usuários (doadores):** `POST /api/usuarios` → `201 + Location`, `GET /api/usuarios`, `GET /api/usuarios/{id}`, `PUT /api/usuarios/{id}` → `200` ou `404`, `DELETE /api/usuarios/{id}` → `204` (idempotente)
- **Doações:** `POST /api/doacoes`, `GET /api/doacoes`, `GET /api/doacoes/{id}`, `GET /api/doacoes/usuario/{usuarioId}`, `PUT /api/doacoes/{id}`, `DELETE /api/doacoes/{id}`
- Validação com `@Valid` → `400` se inválido, `404` se não encontrado (via `GlobalExceptionHandler`)
- Sem autenticação (`config/SecurityConfig.java` → `permitAll`)
- `Doacao.dataDoacao` (Java) é salvo como `data_doacao` no Mongo (`@Field`)

Módulo Maven: `aep/aep/` | Porta da API: `http://localhost:8080` | Mongo: `localhost:27017` | Database: `poc_doacoes`

---

## Pré-requisitos

| Ferramenta | Versão | Como verificar |
|---|---|---|
| **Java JDK 17+** | 17 obrigatório (`pom.xml:30`) | `java -version` deve mostrar `17` ou `21` |
| **Git** | qualquer | `git --version` |
| **Docker Desktop** | com `docker compose` v2 | `docker --version` e `docker compose version` |
| **Navegador ou curl/Postman** | para testar | `curl --version` |

> Não precisa instalar Maven — o projeto usa **Maven Wrapper** (`aep/aep/mvnw` e `aep/aep/mvnw.cmd` que baixa Maven 3.9.6 sozinho).

---

## Passo a passo em outra máquina (do zero)

### Passo 0 — Instalar Java 17

**Windows:**
1. Acesse https://adoptium.net → **Temurin 17** → baixe o `.msi` → instale com padrão.
2. Abra **novo** PowerShell e teste:
   ```powershell
   java -version
   # deve mostrar openjdk 17...
   ```
3. Se der erro, adicione ao PATH: `Configurações > Sistema > Sobre > Configurações avançadas > Variáveis de Ambiente` → `Path` → `C:\Program Files\Eclipse Adoptium\jdk-17...\bin`.

**Mac (Homebrew):**
```bash
brew install openjdk@17
echo 'export JAVA_HOME=$(/usr/libexec/java_home -v17)' >> ~/.zshrc
source ~/.zshrc
java -version
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update && sudo apt install openjdk-17-jdk -y
java -version
```

### Passo 1 — Instalar Docker Desktop

1. https://www.docker.com/products/docker-desktop → **Download** → instale → **reinicie** o PC.
2. Abra o **Docker Desktop** e aguarde ficar `Running` (ícone verde embaixo).
3. Teste no terminal:
   ```bash
   docker --version
   docker compose version
   # deve mostrar Docker version ... e Docker Compose version v2...
   ```
> No Windows, habilite **WSL 2** quando o instalador pedir. No Mac, aceite a permissão.

### Passo 2 — Clonar o repositório (branch Gabriel)

```bash
git clone https://github.com/Maria-Doarda/Aep-6s.git
cd Aep-6s
git checkout Gabriel
cd aep/aep
# no Windows com espaço na pasta "Projetos Java", use aspas:
# cd "C:\Users\SeuNome\Desktop\Projetos Java\AEP6S\aep\aep"
```

Verifique que existe `compose.yaml` e `pom.xml`:
```bash
dir        # Windows
ls -la     # Mac/Linux
```

### Passo 3 — Subir o MongoDB

Na pasta `aep/aep` (onde está `compose.yaml`):

```bash
docker compose up -d
```

Verifique:
```bash
docker ps --filter name=aepmongojava2026_mongo
# STATUS deve ser Up (porta 27017:27017)

docker compose logs mongo
# deve mostrar "Waiting for connections" sem erro
```

> Detalhe: `compose.yaml` usa `mongo:7.0`, container `aepmongojava2026_mongo`, volume `mongo-data` e cria o database `poc_doacoes`. O `application.properties` já aponta para `localhost:27017/poc_doacoes` sem senha.

Se a porta `27017` estiver ocupada:
```powershell
# Windows
netstat -ano | findstr 27017
# Mac/Linux
lsof -i :27017
```
Pare outro Mongo ou use `docker compose down`.

### Passo 4 — Build (compilar)

**Windows PowerShell:**
```powershell
.\mvnw.cmd compile -DskipTests
```

**Mac/Linux:**
```bash
chmod +x mvnw
./mvnw compile -DskipTests
```

Esperado: `BUILD SUCCESS`.

> Primeira vez demora (baixa dependências). Próximas vezes é rápido.

### Passo 5 — Rodar a aplicação

**Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```

**Mac/Linux:**
```bash
./mvnw spring-boot:run
```

Esperado no log:
```
Tomcat started on port 8080
Started AepApplication in ... seconds
```

> O Spring Boot tenta subir o `compose.yaml` sozinho (`spring.docker.compose.enabled=true` em `application.properties:5`). Se você já fez `docker compose up -d` no Passo 3, ele só conecta. Se esqueceu, ele sobe automaticamente se o Docker estiver rodando.

Mantenha este terminal aberto. A API está em `http://localhost:8080`.

### Passo 6 — Testar a API

Abra **outro terminal** e teste:

**1. Criar usuário (doador):**
```bash
curl -X POST http://localhost:8080/api/usuarios \
  -H "Content-Type: application/json" \
  -d "{\"nome\":\"Ana Silva\",\"email\":\"ana@teste.com\",\"enderecos\":[{\"rua\":\"Rua A, 123\",\"cidade\":\"Maringa\",\"estado\":\"PR\"}]}"
```
Resposta: `201 Created` + header `Location: /api/usuarios/{id}` + JSON com `id`. **Copie o `id`**.

**2. Listar usuários:**
```bash
curl http://localhost:8080/api/usuarios
```

**3. Buscar por ID:**
```bash
curl http://localhost:8080/api/usuarios/SEU_ID_AQUI
```

**4. Criar doação:**
```bash
curl -X POST http://localhost:8080/api/doacoes \
  -H "Content-Type: application/json" \
  -d "{\"usuarioId\":\"SEU_ID_AQUI\",\"item\":\"Arroz\",\"quantidade\":10,\"dataDoacao\":\"2026-09-02\"}"
```

**5. Listar doações:**
```bash
curl http://localhost:8080/api/doacoes
curl http://localhost:8080/api/doacoes/usuario/SEU_ID_AQUI
```

**Testar no navegador/Postman:**
- `GET http://localhost:8080/api/usuarios`
- `GET http://localhost:8080/api/doacoes`
- Sem autenticação (Security `permitAll`).

Erros esperados:
- `400` → JSON inválido ou campo faltando (ex: `email` sem `@`, `quantidade: 0`)
- `404` → ID não existe

### Passo 7 — Parar

No terminal do `spring-boot:run`: `Ctrl + C`

Parar Mongo:
```bash
docker compose down        # para o container
docker compose down -v     # apaga os dados (opcional, limpa poc_doacoes)
```

---

## Tabela de endpoints

| Método | URL | Status | Descrição |
|---|---|---|---|
| `POST` | `/api/usuarios` | `201 + Location` | Cria usuário |
| `GET` | `/api/usuarios` | `200` | Lista todos |
| `GET` | `/api/usuarios/{id}` | `200` / `404` | Busca por ID |
| `PUT` | `/api/usuarios/{id}` | `200` / `404` | Atualiza |
| `DELETE` | `/api/usuarios/{id}` | `204` | Remove (idempotente) |
| `POST` | `/api/doacoes` | `201 + Location` | Cria doação |
| `GET` | `/api/doacoes` | `200` | Lista todas |
| `GET` | `/api/doacoes/{id}` | `200` / `404` | Busca por ID |
| `GET` | `/api/doacoes/usuario/{usuarioId}` | `200` | Lista por usuário |
| `PUT` | `/api/doacoes/{id}` | `200` / `404` | Atualiza |
| `DELETE` | `/api/doacoes/{id}` | `204` | Remove |

---

## Solução de problemas

| Erro | Causa | Solução |
|---|---|---|
| `MongoTimeoutException: localhost:27017` | Docker/Mongo não subiu | `docker compose up -d` → `docker ps` deve mostrar `Up` |
| `port 27017 already in use` | Outro Mongo rodando | `docker compose down` ou mate o processo na porta |
| `JAVA_HOME not defined` / `java -version` mostra 8 ou 11 | JDK 17 não instalado | Reinstale Temurin 17 e abra novo terminal |
| `.\mvnw` não funciona no Windows | Usou comando Linux | No Windows use `.\mvnw.cmd` |
| `BUILD FAILURE` na primeira vez | Sem internet | Precisa internet só na 1ª vez para baixar dependências |
| `404` em `GET /api/usuarios/xxx` | ID não existe | Liste `GET /api/usuarios` e copie ID válido |
| `400` ao criar | JSON inválido | Verifique `email` com `@`, `nome` não vazio, `quantidade > 0`, `estado` com 2 letras |

---

## Estrutura do projeto

```
aep/aep/
├── pom.xml                              # Spring Boot 4.1.0, Java 17, spring-boot-starter-webmvc
├── compose.yaml                         # mongo:7.0 em 27017 → database poc_doacoes
├── mvnw / mvnw.cmd                      # Maven Wrapper
├── src/main/resources/application.properties  # host/port/database + docker.compose.enabled
└── src/main/java/fz/exemple/aep/
    ├── AepApplication.java               # main
    ├── config/SecurityConfig.java        # permitAll, csrf disabled
    ├── controllers/                      # UsuarioController, DoacaoController
    ├── services/                         # UsuarioService, DoacaoService
    ├── repositories/                     # UsuarioRepository, DoacaoRepository (MongoRepository)
    ├── models/                           # Usuario, Doacao (@Document), Endereco (embedded)
    ├── dto/                              # Create/Update Request + Response
    ├── mapper/                           # UsuarioMapper, DoacaoMapper
    └── exception/GlobalExceptionHandler.java  # 400 e 404
```

## Comandos rápidos

```bash
# do zero (Windows)
git clone https://github.com/Maria-Doarda/Aep-6s.git && cd Aep-6s && git checkout Gabriel && cd aep/aep
docker compose up -d && .\mvnw.cmd compile -DskipTests && .\mvnw.cmd spring-boot:run

# do zero (Mac/Linux)
git clone https://github.com/Maria-Doarda/Aep-6s.git && cd Aep-6s && git checkout Gabriel && cd aep/aep
docker compose up -d && ./mvnw compile -DskipTests && ./mvnw spring-boot:run
```
