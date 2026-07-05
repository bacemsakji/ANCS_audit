package tn.gov.ancs.audit.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Springdoc OpenAPI (Swagger UI).
 * Accessible en développement à l'URL : http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI ancsOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ANCS Audit API")
                .description("""
                    API REST de la plateforme d'audit de sécurité des systèmes d'information.
                    
                    **Référentiels applicables :**
                    - Décret-loi n° 2023-17 relatif à la cybersécurité
                    - Arrêté du 01/10/2019 fixant les normes d'audit
                    - Référentiel de contrôles ANCS
                    
                    **Authentification :** Bearer JWT (header `Authorization: Bearer <token>`)
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("Agence Nationale de Cybersécurité (ANCS)")
                    .email("contact@ancs.gov.tn")
                    .url("https://www.ancs.gov.tn"))
                .license(new License()
                    .name("Usage interne ANCS — Accès restreint")
                    .url("https://www.ancs.gov.tn"))
            )
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Insérez le token JWT obtenu via POST /api/auth/login")
                )
            );
    }
}
