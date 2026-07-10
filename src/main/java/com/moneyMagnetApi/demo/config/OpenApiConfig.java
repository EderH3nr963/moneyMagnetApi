package com.moneyMagnetApi.demo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Money Magnet API",
                version = "v1",
                description = "API REST para autenticacao, perfil, dashboard financeiro, contas, transacoes, categorias e integracao Pluggy.",
                contact = @Contact(
                        name = "Money Magnet",
                        email = "support@moneymagnet.local"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Ambiente local")
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@Configuration
public class OpenApiConfig {
}
