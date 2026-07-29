# 📋 Task Manager API

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
- - **Spring Security** (autenticação e autorização)
- **JWT (JJWT)** (tokens de autenticação stateless)

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
