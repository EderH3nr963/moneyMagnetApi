**Money Magnet API**

Breve API back-end para gerenciamento financeiro pessoal (transações, categorias, usuários, autenticação JWT e recuperação de senha por e-mail). Implementada em Java com Spring Boot e Maven.

**Visão Geral**
- **Projeto:** API REST para controle de transações e categorias com autenticação.
- **Stack:** Java, Spring Boot, Maven.
- **Funcionalidades:** registro/login, CRUD de categorias e transações, dashboard resumo, import/export Excel, recuperação de senha via e-mail, rate limiting e JWT.

**Pré-requisitos**
- **Java:** JDK 11 ou superior instalado.
- **Maven:** use o wrapper `./mvnw` (Linux/macOS) ou `mvnw.cmd` (Windows) incluído no projeto.
- **Banco de dados:** MySQL/Postgres ou outro suportado pelo Spring Data (configure em `src/main/resources/application-*.properties`).
- **SMTP:** para envio de e-mails (recuperação de senha).

**Instalação e execução**
- Clone o repositório.
- Build: `./mvnw clean package` (ou `mvnw.cmd clean package` no Windows).
- Executar em desenvolvimento: `./mvnw spring-boot:run`.
- Executar jar: `java -jar target/<artifact>.jar`.
- Perfis: use `spring.profiles.active=dev` ou `prod` conforme necessário e ajuste `src/main/resources/application-dev.properties` e `application-prod.properties`.

**Configuração importante**
- Arquivo: [src/main/resources/application.properties](src/main/resources/application.properties)
- Propriedades chave (exemplos):
  - `spring.datasource.url` — URL do banco de dados
  - `spring.datasource.username` — usuário DB
  - `spring.datasource.password` — senha DB
  - `spring.profiles.active` — perfil ativo (dev|prod)
  - `jwt.secret` — segredo para geração de tokens JWT
  - `spring.mail.host`, `spring.mail.username`, `spring.mail.password` — para envio de e-mails

**Principais endpoints (resumo)**
- **Autenticação:** `/api/auth` — registro, login, esqueci-senha, reset-senha.
- **Usuários:** `/api/usuarios` — endpoints para perfil e atualização.
- **Categorias:** `/api/categories` — CRUD de categorias (tipos: receita/despesa).
- **Transações:** `/api/transactions` — CRUD de transações, atualização parcial (valor, data, descrição), import/export Excel.
- **Dashboard:** `/api/dashboard` — dados agregados (saldo, total por categoria, etc.).

Os controllers relevantes estão em `src/main/java/com/moneyMagnetApi/demo/controller`.

**Testes**
- Executar testes: `./mvnw test`.

**Arquivo de referência**
- Configurações e filtros de segurança: [src/main/java/com/moneyMagnetApi/demo/config/SecurityConfig.java](src/main/java/com/moneyMagnetApi/demo/config/SecurityConfig.java)
- Serviço de tokens: [src/main/java/com/moneyMagnetApi/demo/security/TokenService.java](src/main/java/com/moneyMagnetApi/demo/security/TokenService.java)
- Serviço de e-mail: [src/main/java/com/moneyMagnetApi/demo/service/EmailService.java](src/main/java/com/moneyMagnetApi/demo/service/EmailService.java)