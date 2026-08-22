package io.pragmatic.ponderus.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Métadonnées du contrat OpenAPI exposé par springdoc.
 *
 * <p>Déclare le schéma de sécurité « bearer-jwt » (le token d'accès Firebase,
 * transmis en {@code Authorization: Bearer <token>}) et l'applique globalement,
 * pour que le client TypeScript généré sache attacher l'en-tête d'authentification
 * sur les endpoints protégés.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Ponderus API",
                version = "0.1.0",
                description = "API du comparateur pondéré : projets, options, critères et scores."),
        servers = @Server(url = "/", description = "Serveur courant"),
        security = @SecurityRequirement(name = "bearer-jwt")
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token d'accès Firebase (vérifié côté serveur).")
public class OpenApiConfig {
}
