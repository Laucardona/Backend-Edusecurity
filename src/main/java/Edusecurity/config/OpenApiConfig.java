package Edusecurity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        final String schemeName = "bearerAuth";

        return new OpenAPI()
                .info(
                        new Info()
                                .title("API Edusecurity")
                                .version("1.0")
                                .description("Backend del módulo Gestión de Contenidos Educativos")
                                .contact(
                                        new Contact()
                                                .name("Laura Cardona")
                                                .email("laura@email.com")
                                )
                )
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(
                        new Components().addSecuritySchemes(
                                schemeName,
                                new SecurityScheme()
                                        .name(schemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
}