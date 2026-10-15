package edusecurity.common.docs;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Documentación OpenAPI estándar (esquema bearer JWT incluido) para todos los
 * servicios que usen springdoc. Se configura con:
 *
 * <pre>
 * app.openapi.title=Nombre del servicio
 * app.openapi.description=Descripción
 * </pre>
 */
@AutoConfiguration
@ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
@ConditionalOnProperty(prefix = "app.openapi", name = "title")
public class OpenApiAutoConfiguration {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openApi(
            @org.springframework.beans.factory.annotation.Value("${app.openapi.title}") String title,
            @org.springframework.beans.factory.annotation.Value("${app.openapi.version:1.0.0}") String version,
            @org.springframework.beans.factory.annotation.Value("${app.openapi.description:}") String description) {

        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version(version)
                        .description(description))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(
                        BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
