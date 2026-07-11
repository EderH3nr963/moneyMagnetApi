# Money Magnet — API

API REST do Money Magnet. O backend gerencia autenticação e sessões, perfil do usuário, integração bancária com a Pluggy, contas, transações, categorias, regras automáticas por merchant, dashboard financeiro, recuperação de senha, auditoria, cache e rate limit.

## Tecnologias

- Java 17
- Spring Boot 4
- Spring Web MVC e Spring Security
- Spring Data JPA e PostgreSQL
- Auth0 Java JWT
- Spring Mail
- Springdoc OpenAPI/Swagger
- Caffeine Cache
- Bucket4j
- Maven Wrapper

## Pré-requisitos

- Java 17 ou superior
- PostgreSQL
- Credenciais da Pluggy
- Servidor SMTP para recuperação de senha

## Perfis e configuração

Os arquivos principais ficam em:

```text
src/main/resources/application.properties
src/main/resources/application-dev.properties
src/main/resources/application-prod.properties
```

Para executar localmente com o perfil de desenvolvimento:

```env
SPRING_PROFILES_ACTIVE=dev
```

Variáveis utilizadas em produção:

```env
SPRING_PROFILES_ACTIVE=prod

DATABASE_URL=jdbc:postgresql://host:5432/moneyMagnet
DATABASE_USERNAME=usuario
DATABASE_PASSWORD=senha

JWT_SECRET=um_segredo_aleatorio_e_forte
REFRESH_TOKEN_EXPIRATION_DAYS=30
BASE_URL_FRONT=https://app.exemplo.com

EMAIL_HOST=smtp.example.com
EMAIL_PORT=587
EMAIL_USERNAME=usuario@example.com
EMAIL_PASSWORD=senha

PLUGGY_BASE_URL=https://api.pluggy.ai
PLUGGY_CLIENT_ID=seu_client_id
PLUGGY_CLIENT_SECRET=seu_client_secret
PLUGGY_API_KEY=
```

Em produção, `JWT_SECRET` deve ser fornecido externamente e nunca reutilizado entre ambientes.

## Banco de dados

O projeto utiliza PostgreSQL. O arquivo `../script.sql` contém a estrutura de referência.

Além dos dados financeiros, o banco mantém:

- `token_version` no usuário para invalidar access tokens antigos;
- hashes SHA-256 dos refresh tokens em `refresh_tokens`;
- hashes SHA-256 dos tokens de recuperação em `password_reset_tokens`;
- registros de auditoria em `audit_logs`.

O valor original do refresh token existe apenas no cookie do navegador. O banco armazena somente seu hash.

O perfil `dev` usa `spring.jpa.hibernate.ddl-auto=update`. O perfil `prod` usa `validate`, portanto alterações de schema precisam ser aplicadas antes do deploy.

## Execução

Windows:

```bash
.\mvnw.cmd spring-boot:run
```

Linux ou macOS:

```bash
./mvnw spring-boot:run
```

API local: `http://localhost:8080`

Health check:

```http
GET /health
```

## Autenticação e sessões

O backend permanece stateless no Spring Security e não utiliza `HttpSession`.

### Login e cadastro

Ao autenticar, a API gera:

- um access token JWT válido por 1 hora, retornado no JSON;
- um refresh token aleatório válido por 30 dias, enviado em cookie `HttpOnly`.

O cookie utiliza:

- `HttpOnly`;
- `SameSite=Strict`;
- `Secure` em produção;
- `Path=/api/v1/auth`.

### Validação do access token

O JWT contém o UUID do usuário e `tokenVersion`. O filtro valida assinatura, emissor, expiração, existência do usuário e compatibilidade da versão antes de autenticar a requisição.

### Renovação

`POST /api/v1/auth/refresh` recebe o refresh token pelo cookie, calcula seu SHA-256, consulta o registro com bloqueio pessimista e rotaciona o token. O registro antigo é removido e um novo hash é persistido.

### Revogação

- Logout remove o refresh token atual e apaga o cookie.
- Troca ou redefinição de senha incrementa `tokenVersion` e remove todos os refresh tokens do usuário.
- JWTs emitidos antes da troca de senha deixam de ser aceitos.

### Recuperação de senha

O token aleatório é enviado por e-mail e somente seu SHA-256 é persistido. Na redefinição, o backend calcula o hash do token recebido no corpo da requisição antes de consultar o banco.

## Endpoints principais

### Autenticação

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

### Perfil

- `GET /api/v1/profile/me`
- `PATCH /api/v1/profile/username/and/email`
- `PATCH /api/v1/profile/password`
- `PATCH /api/v1/profile/theme`
- `DELETE /api/v1/profile`

### Dashboard

- `GET /api/v1/dashboard`
- `GET /api/v1/dashboard/expenses-category`
- `GET /api/v1/dashboard/financial-history`

### Pluggy, itens e bancos

- `POST /api/v1/pluggy/connect-token`
- `POST /api/v1/items`
- `GET /api/v1/banks`
- `GET /api/v1/banks/{itemId}`
- `GET /api/v1/banks/{itemId}/transactions`

### Transações

- `GET /transactions`
- `PUT /transactions/{transactionId}`

### Categorias e regras por merchant

- `GET /api/v1/categories`
- `POST /api/v1/categories`
- `PUT /api/v1/categories/{categoryId}`
- `DELETE /api/v1/categories/{categoryId}`
- `GET /api/v1/categories/merchant-rules`
- `POST /api/v1/categories/merchant-rules`
- `PUT /api/v1/categories/merchant-rules/{ruleId}`
- `DELETE /api/v1/categories/merchant-rules/{ruleId}`

## Segurança

- Senhas protegidas com BCrypt.
- Access tokens assinados com HMAC SHA-256.
- Refresh tokens e tokens de recuperação armazenados somente como SHA-256.
- Controle de propriedade nas consultas de itens, contas, transações e categorias.
- Rate limit para autenticação e demais requisições.
- CORS restrito à URL configurada do frontend.
- Respostas de erro sanitizadas; detalhes internos ficam apenas nos logs do servidor.
- Auditoria de método, rota, status, duração, usuário e recurso.

## Cache

O Caffeine reduz consultas repetidas para:

- categorias e regras por usuário;
- mapeamentos de categorias Pluggy;
- contas por usuário, item e ID;
- transações por página, conta e ID;
- validação de item por usuário.

Os caches possuem limite e expiração por acesso e são invalidados após operações que modificam os dados relacionados.

## Swagger

No perfil `dev`:

- UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

O Swagger é desativado no perfil `prod`.

Para testar uma rota protegida, faça login, copie o campo `token` da resposta e use `Bearer <token>` na autorização.

## Build e testes

Windows:

```bash
.\mvnw.cmd compile
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Linux ou macOS:

```bash
./mvnw compile
./mvnw test
./mvnw clean package
```

## Integração com o frontend

Em desenvolvimento, o backend espera o frontend em `http://localhost:3000`. A URL configurada em `app.frontend.base-url` é utilizada pelo CORS e pelo link enviado na recuperação de senha.

Como a autenticação usa cookie para renovação, o frontend precisa enviar as requisições com credenciais incluídas. O cookie seguro exige HTTPS em produção.
