# Money Magnet API

Backend REST do Money Magnet, criado com Java e Spring Boot. A API cuida de autenticação, perfil, conexão bancária via Pluggy, contas, transações, categorias, regras automáticas por merchant, dashboard financeiro, recuperação de senha por e-mail e cache com Caffeine.

## Stack

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Security com JWT
- Spring Data JPA
- PostgreSQL
- Spring Mail
- Springdoc OpenAPI/Swagger
- Caffeine Cache
- Bucket4j para rate limit
- Maven Wrapper

## Pré-requisitos

- Java 17+
- PostgreSQL
- Conta/credenciais Pluggy
- SMTP para recuperação de senha

## Configuração

Arquivos principais:

```text
src/main/resources/application.properties
src/main/resources/application-dev.properties
src/main/resources/application-prod.properties
```

Em desenvolvimento, o perfil `dev` usa PostgreSQL local:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/moneyMagnet
spring.datasource.username=meuUser
spring.datasource.password=minhasenha123
```

Variáveis esperadas em ambiente:

```env
EMAIL_HOST=smtp.example.com
EMAIL_PORT=587
EMAIL_USERNAME=usuario@example.com
EMAIL_PASSWORD=senha
EMAIL_DEBUG=false

PLUGGY_BASE_URL=https://api.pluggy.ai
PLUGGY_CLIENT_ID=seu_client_id
PLUGGY_CLIENT_SECRET=seu_client_secret
PLUGGY_API_KEY=

DATABASE_URL=jdbc:postgresql://host:5432/moneyMagnet
DATABASE_USERNAME=usuario
DATABASE_PASSWORD=senha
JWT_SECRET=segredo_producao
BASE_URL_FRONT=http://localhost:3000
```

## Como executar

No Windows:

```bash
.\mvnw.cmd spring-boot:run
```

No Linux/macOS:

```bash
./mvnw spring-boot:run
```

API local:

```text
http://localhost:8080
```

Health check:

```text
GET /health
```

## Swagger

Com o perfil de desenvolvimento, a documentação interativa fica em:

```text
http://localhost:8080/swagger-ui.html
```

Documento OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Para testar rotas protegidas no Swagger:

1. Faça login em `POST /api/v1/auth/login`.
2. Copie o `token` retornado.
3. Clique em `Authorize`.
4. Informe `Bearer <token>`.

## Principais módulos

- `controller`: endpoints REST documentados com Swagger
- `service`: regras de negócio, sincronização, cache e integração externa
- `repository`: consultas JPA
- `domain`: entidades do domínio
- `dto`: contratos de entrada e saída da API
- `security`: JWT, filtros e autenticação
- `config`: segurança, OpenAPI e cache

## Endpoints principais

### Autenticação

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password/{token}`

### Perfil

- `GET /api/v1/profile/me`
- `PATCH /api/v1/profile/username/and/email`
- `PATCH /api/v1/profile/email`
- `PATCH /api/v1/profile/username`
- `PATCH /api/v1/profile/password`
- `PATCH /api/v1/profile/theme`
- `DELETE /api/v1/profile`

### Dashboard

- `GET /api/v1/dashboard`
- `GET /api/v1/dashboard/expenses-category`
- `GET /api/v1/dashboard/financial-history`

### Pluggy e Items

- `POST /api/v1/pluggy/connect-token`
- `POST /api/v1/items`

### Contas e instituições

- `GET /api/v1/accounts`
- `GET /api/v1/accounts/{accountId}`
- `GET /api/v1/accounts/item/{itemId}`
- `DELETE /api/v1/accounts/{accountId}`
- `GET /api/v1/institutions/{institutionId}`
- `GET /api/v1/institutions/{institutionId}/transactions`

### Transações

- `GET /transactions`
- `GET /transactions/{transactionId}`
- `GET /transactions/account/{accountId}`
- `PUT /transactions/{transactionId}`
- `DELETE /transactions/{transactionId}`

### Categorias e regras por merchant

- `GET /api/v1/categories`
- `POST /api/v1/categories`
- `PUT /api/v1/categories/{categoryId}`
- `DELETE /api/v1/categories/{categoryId}`
- `GET /api/v1/categories/merchant-rules`
- `POST /api/v1/categories/merchant-rules`
- `PUT /api/v1/categories/merchant-rules/{ruleId}`
- `DELETE /api/v1/categories/merchant-rules/{ruleId}`

## Cache

O projeto usa Caffeine para reduzir leituras repetidas em dados muito acessados:

- categorias por usuário
- regras de merchant
- mapeamento de categorias Pluggy
- contas por usuário, item e ID
- transações por página, conta e ID
- validação de item por usuário

Os caches têm limite de tamanho e expiração por acesso. Quando há sincronização, edição ou exclusão, os caches relacionados são invalidados.

## Build e testes

Compilar:

```bash
.\mvnw.cmd compile
```

Rodar testes:

```bash
.\mvnw.cmd test
```

Gerar pacote:

```bash
.\mvnw.cmd clean package
```

## Integração com o frontend

O frontend espera o backend em:

```text
http://localhost:8080
```

O backend usa:

```properties
app.frontend.base-url=http://localhost:3000
```

Essa URL é usada principalmente no link de recuperação de senha.
