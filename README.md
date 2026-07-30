# 📋 Task Manager API
![CI](https://github.com/ssergionp/task-manager-api/actions/workflows/ci.yml/badge.svg)

API REST desenvolvida em **Java + Spring Boot** para gerenciamento de tarefas, com CRUD completo, autenticação de dados via DTOs, tratamento global de exceções, testes automatizados e deploy em produção.

🔗 **API em produção:** [https://task-manager-api-vcu3.onrender.com](https://task-manager-api-vcu3.onrender.com)
📖 **Documentação interativa (Swagger):** [https://task-manager-api-vcu3.onrender.com/swagger-ui.html](https://task-manager-api-vcu3.onrender.com/swagger-ui.html)

> ⚠️ O serviço roda no plano gratuito do Render, que "dorme" após 15 minutos de inatividade. A primeira requisição após esse período pode levar de 30 a 60 segundos para responder — é esperado, não é um bug.

---

## 🚀 Tecnologias utilizadas

- **Java 25**
- **Spring Boot 4.1** (Spring Web, Spring Data JPA, Validation, DevTools)
- **PostgreSQL** (banco relacional em produção, via [Neon](https://neon.tech))
- **H2 Database** (banco em memória, usado nos testes automatizados)
- **Hibernate / JPA** (mapeamento objeto-relacional)
- **Lombok** (redução de boilerplate)
- **JUnit 5 + Mockito** (testes unitários)
- **Spring Test / MockMvc** (testes de integração)
- **Springdoc OpenAPI** (documentação Swagger)
- **Docker & Docker Compose** (containerização da aplicação e do banco)
- **Maven** (gerenciamento de dependências e build)
- **Render** (hospedagem da aplicação)
- **Spring Security** (autenticação e autorização)
- **JWT (JJWT)** (tokens de autenticação stateless)
- **Flyway** (migrations versionadas de banco de dados)
- **Spring Boot Actuator** (observabilidade e health-check)
- **GitHub Actions** (integração contínua)
- **Refresh Token** (renovação de sessão sem novo login)
- **Spring Security OAuth2 Client** (login social com Google)

---

## 🏗️ Arquitetura

O projeto segue uma arquitetura em camadas, separando claramente responsabilidades:

```
com.ssergionp.taskmanagerapi
├── controller     → Endpoints REST (camada de entrada HTTP)
├── service        → Regras de negócio
├── repository     → Acesso a dados (Spring Data JPA)
├── model          → Entidades JPA (Task, TaskStatus)
├── dto            → Objetos de transferência (Request/Response)
└── exception      → Tratamento global de exceções
```

**Por que essa separação importa:** a API nunca expõe as entidades JPA diretamente — todo dado que entra ou sai passa por DTOs (`TaskRequestDTO` / `TaskResponseDTO`), o que evita acoplamento entre o modelo de banco de dados e o contrato da API.

---

## ✅ Funcionalidades

- CRUD completo de tarefas (criar, listar, buscar por ID, atualizar, remover)
- Filtro de tarefas por status (`TODO`, `IN_PROGRESS`, `DONE`)
- Validação de dados de entrada (título obrigatório, datas não podem estar no passado, etc.)
- Tratamento global de exceções com respostas padronizadas (`status`, `message`, `timestamp`)
- Testes automatizados (unitários e de integração)
- Documentação interativa via Swagger/OpenAPI
- Containerização completa via Docker (aplicação + banco)
- Deploy em produção com banco de dados PostgreSQL gerenciado
- Autenticação via JWT (registro e login de usuários)
- Senhas armazenadas com hash BCrypt

---

## 🔐 Autenticação (JWT)

A API utiliza autenticação via **JSON Web Token (JWT)**, com **Spring Security**. A maioria dos endpoints exige um token válido no cabeçalho `Authorization`.

### Fluxo de autenticação

1. **Registrar um usuário** (uma única vez):
   ```
   POST /auth/register
   Content-Type: application/json

   {
     "username": "sergio",
     "password": "senha123"
   }
   ```
   Retorna `201 Created` com um token JWT já pronto para uso.

2. **Fazer login** (nas próximas vezes):
   ```
   POST /auth/login
   Content-Type: application/json

   {
     "username": "sergio",
     "password": "senha123"
   }
   ```
   Retorna `200 OK` com um token JWT:
   ```json
   {
     "token": "eyJhbGciOiJIUzUxMiJ9..."
   }
   ```

3. **Usar o token** em todas as requisições aos endpoints protegidos, no cabeçalho `Authorization`:
   ```
   Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
   ```

### Rotas públicas (não exigem token)
- `POST /auth/register`
- `POST /auth/login`
- `/swagger-ui/**` e `/v3/api-docs/**` (documentação)

### Rotas protegidas (exigem token)
- Todos os endpoints de `/tasks/**`

### Testando pelo Swagger

Acesse `/swagger-ui.html`, clique no botão **"Authorize"** (canto superior direito), cole o token obtido no login (sem o prefixo `Bearer`) e todos os endpoints protegidos ficam disponíveis para teste diretamente pela interface.

### Detalhes técnicos

- Senhas são armazenadas com hash **BCrypt**, nunca em texto puro.
- Tokens JWT expiram em 24 horas (configurável via `jwt.expiration` no `application.properties`).
- A API é **stateless** — nenhuma sessão é mantida no servidor; cada requisição se autentica de forma independente via token.

## 🔄 Refresh Token

Para evitar que o usuário precise fazer login com usuário/senha com frequência, a API implementa o padrão **access token + refresh token**:

- **Access token (JWT):** validade curta (**15 minutos**), usado no cabeçalho `Authorization` de toda requisição.
- **Refresh token:** validade longa (**7 dias**), armazenado no banco de dados, usado exclusivamente para obter um novo access token.

### Fluxo completo

1. **Login/registro** retornam os dois tokens:
   ```json
   {
     "token": "eyJhbGciOiJIUzUxMiJ9...",
     "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```

2. Quando o **access token expira**, em vez de fazer login novamente, o cliente troca o refresh token por um access token novo:
   ```
   POST /auth/refresh
   Content-Type: application/json

   {
     "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```
   Retorna um novo `token`, mantendo o mesmo `refreshToken`.

3. **Logout** revoga o refresh token, impedindo que ele seja usado novamente:
   ```
   POST /auth/logout
   Content-Type: application/json

   {
     "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```

### Por que essa abordagem

- **Access token curto** → limita o impacto caso um token vazado seja interceptado.
- **Refresh token revogável** → permite encerrar uma sessão remotamente (logout), algo impossível de fazer com um JWT puro sem essa camada extra.
- **Um refresh token ativo por usuário** → a cada novo login, o refresh token anterior é descartado, evitando acúmulo de sessões esquecidas.


## 🔗 Login com Google (OAuth2)

Além do login tradicional (usuário/senha), a API suporta autenticação via **Google (OAuth2 / OpenID Connect)**.

### Fluxo

1. O cliente redireciona o usuário para:
   ```
   GET /oauth2/authorization/google
   ```
2. O usuário faz login e autoriza o acesso na tela do Google.
3. O Google redireciona de volta para a aplicação, que troca o código de autorização pelos dados do perfil (e-mail).
4. A API então:
    - Localiza um usuário existente com esse e-mail, **ou** cria um novo automaticamente (marcado como `AuthProvider.GOOGLE`, sem senha própria).
    - Gera os **mesmos tokens** (access token JWT + refresh token) usados no login tradicional.
5. A resposta final é idêntica à do `/auth/login` comum:
   ```json
   {
     "token": "eyJhbGciOiJIUzUxMiJ9...",
     "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
   }
   ```

### Por que essa abordagem

- **Um único sistema de tokens** para toda a API — não importa se o usuário entrou com senha ou com Google, o restante do sistema (autorização, refresh, expiração) funciona de forma idêntica.
- **Contas reaproveitadas por e-mail** — se um usuário já existia localmente e depois usa "Entrar com Google" com o mesmo e-mail, a conta é a mesma, sem duplicar cadastros.
- **Nenhuma senha armazenada para contas sociais** — o campo `password` fica nulo para usuários com `AuthProvider.GOOGLE`, já que a autenticação é delegada inteiramente ao Google.

### Nota sobre gov.br

A arquitetura foi desenhada de forma genérica o suficiente para, em tese, suportar qualquer provedor OpenID Connect adicional — incluindo o Login Único do gov.br, que usa o mesmo protocolo. Na prática, o acesso de desenvolvedor ao gov.br para aplicações privadas não é self-service: exige processo comercial via Loja Serpro/Dataprev (para empresas) ou solicitação institucional via SGD (para órgãos públicos), o que inviabiliza a integração num projeto pessoal de portfólio. Por esse motivo, o Google foi escolhido como provedor de demonstração, mantendo a mesma base técnica (OAuth2/OIDC) que seria usada numa eventual integração institucional com o gov.br.

## 🛡️ Recursos avançados

Além do CRUD básico com autenticação, o projeto conta com recursos que aproximam a aplicação de um cenário real de produção:

### Controle de acesso
- **Cada usuário só acessa suas próprias tarefas** — o relacionamento `Task` → `User` garante isolamento total entre usuários.
- **Autorização por papel (role):** usuários `ADMIN` têm acesso a um endpoint especial (`GET /tasks/admin/all`) que lista as tarefas de **todos** os usuários — útil para um painel administrativo.
- **Respostas HTTP semanticamente corretas:**
   - `401 Unauthorized` → token ausente ou inválido
   - `403 Forbidden` → autenticado, mas sem permissão suficiente
   - Ambos retornam um JSON padronizado (`status`, `message`, `timestamp`)

### Paginação
Os endpoints de listagem (`GET /tasks` e `GET /tasks/status/{status}`) suportam paginação via query params:
```
GET /tasks?page=0&size=10
```
Resposta no formato:
```json
{
  "content": [ ... ],
  "pageNumber": 0,
  "pageSize": 10,
  "totalElements": 42,
  "totalPages": 5,
  "last": false
}
```

### Migrations versionadas (Flyway)
O schema do banco de dados é controlado por migrations SQL versionadas, localizadas em `src/main/resources/db/migration`, em vez de deixar o Hibernate alterar tabelas automaticamente. Isso garante:
- Histórico auditável de cada mudança na estrutura do banco
- Consistência entre ambientes (local, CI, produção)
- Segurança contra alterações destrutivas acidentais

> Em produção, o Flyway foi configurado com baseline (`spring.flyway.baseline-on-migrate`), já que o banco já possuía tabelas criadas anteriormente pelo Hibernate antes da adoção do Flyway.

### Observabilidade (Spring Boot Actuator)
A API expõe endpoints de monitoramento:
- `GET /actuator/health` — público, usado pelo Render para health-check automático
- `GET /actuator/info` — informações da aplicação (somente ADMIN)
- `GET /actuator/metrics` — métricas técnicas: JVM, requisições HTTP, pool de conexões, etc. (somente ADMIN)

### Integração Contínua (CI)
Todo push na branch `main` (e Pull Requests) dispara automaticamente uma pipeline no **GitHub Actions** (`.github/workflows/ci.yml`) que:
1. Configura o ambiente com JDK 25
2. Roda os 14 testes automatizados (unitários + integração)
3. Compila e empacota a aplicação, validando que o build está íntegro

O status do build (✅ ou ❌) fica visível diretamente no histórico de commits do repositório.

## 📍 Endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/tasks` | Cria uma nova tarefa |
| `GET` | `/tasks` | Lista todas as tarefas |
| `GET` | `/tasks/{id}` | Busca uma tarefa por ID |
| `PUT` | `/tasks/{id}` | Atualiza uma tarefa existente |
| `DELETE` | `/tasks/{id}` | Remove uma tarefa |
| `GET` | `/tasks/status/{status}` | Lista tarefas filtradas por status |

Exemplo de payload para criação (`POST /tasks`):
```json
{
  "title": "Estudar Spring Boot",
  "description": "Terminar o CRUD de tarefas",
  "dueDate": "2026-08-15"
}
```

---

## ▶️ Como rodar localmente

### Pré-requisitos
- [JDK 21+](https://adoptium.net)
- [Docker Desktop](https://www.docker.com/products/docker-desktop)
- Maven (ou usar o wrapper `./mvnw` incluso no projeto)

### Opção 1 — Rodando tudo via Docker (recomendado)

Sobe a aplicação **e** o banco PostgreSQL juntos, sem precisar instalar nada além do Docker:

```bash
git clone https://github.com/ssergionp/task-manager-api.git
cd task-manager-api
docker compose up --build -d
```

A API estará disponível em `http://localhost:8080`, e o Swagger em `http://localhost:8080/swagger-ui.html`.

### Opção 2 — Rodando a aplicação localmente (fora do Docker) com banco em Docker

Sobe só o banco Postgres via Docker, e roda a aplicação Java direto na sua máquina (útil durante o desenvolvimento, para aproveitar hot-reload da IDE):

```bash
docker compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

### Opção 3 — Rodando com banco em memória (H2), sem Docker

Mais rápido para testes pontuais, sem persistência de dados:

```bash
./mvnw spring-boot:run
```

---

## 🧪 Rodando os testes

```bash
./mvnw test
```

O projeto conta com:
- **Testes unitários** (`TaskServiceTest`) — testam a lógica de negócio isoladamente, usando Mockito para simular o repositório.
- **Testes de integração** (`TaskControllerTest`) — testam os endpoints via requisições HTTP simuladas (`MockMvc`), com banco H2 em memória.

---

## 🐳 Docker

O projeto usa um **build multi-stage** no `Dockerfile`:
1. Uma etapa com Maven + JDK compila o projeto e gera o `.jar`.
2. A imagem final usa apenas o Java Runtime (JRE), copiando somente o `.jar` compilado — resultando em uma imagem final mais leve.

O `docker-compose.yml` orquestra dois serviços:
- `postgres` — banco de dados, com verificação de saúde (`healthcheck`) antes de liberar a aplicação.
- `app` — a aplicação Spring Boot, que só inicia depois que o banco estiver pronto.

> **Nota:** localmente, o Postgres é exposto na porta `5433` (em vez da padrão `5432`), para evitar conflito com instalações nativas do Postgres que já possam existir na máquina.

---

## ☁️ Deploy

- **Aplicação:** hospedada no [Render](https://render.com), com deploy automático a cada push na branch `main` (Docker runtime, detectado a partir do `Dockerfile` do projeto).
- **Banco de dados:** PostgreSQL gerenciado pelo [Neon](https://neon.tech), com tier gratuito permanente (diferente de bancos gratuitos que expiram após alguns dias).

As credenciais de conexão com o banco são passadas via variáveis de ambiente na plataforma de hospedagem, nunca commitadas no repositório.

---

## 📌 Próximos passos (ideias de evolução)

- [ ] Autenticação e autorização (Spring Security + JWT)
- [ ] Paginação nos endpoints de listagem
- [ ] Migrations versionadas com Flyway (em vez de `ddl-auto=update`)
- [ ] Pipeline de CI/CD com GitHub Actions
- [ ] Testes de carga

---

## 👤 Autor

**Sérgio do Nascimento Pereira**
Projeto desenvolvido como parte de estudos em Java + Spring Boot.
[LinkedIn](https://www.linkedin.com/in/sergio-do-nascimento-pereira-7a174a112/) • [GitHub](https://github.com/ssergionp)
