package io.pragmatic.ponderus;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Vérifie le contrat OpenAPI (issue #8) : accessibilité sans authentification,
 * présence des métadonnées et du schéma de sécurité, documentation des DTOs
 * avec exemples, et structure exploitable pour générer un client TypeScript.
 *
 * <p>Le MockMvc est monté depuis le contexte web complet AVEC la chaîne de
 * filtres de sécurité réelle ({@code springSecurity()}), afin de prouver que
 * les routes de documentation sont bien accessibles sans authentification.
 */
class OpenApiContractIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void apiDocs_isPubliclyAccessible_asJson() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("Ponderus API"))
                .andExpect(jsonPath("$.info.version").value("0.1.0"));
    }

    @Test
    void swaggerUi_isReachableWithoutAuth() throws Exception {
        // /swagger-ui.html redirige vers la page servie par springdoc : le point
        // important est qu'elle n'est pas bloquée par la sécurité (ni 401 ni 403).
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void apiDocs_declaresFirebaseBearerSecurityScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearer-jwt.bearerFormat").value("JWT"));
    }

    @Test
    void apiDocs_documentsDtosWithExamples() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // ProjectRequest : exemple sur le nom.
                .andExpect(jsonPath("$.components.schemas.ProjectRequest.properties.name.example")
                        .value("Achat maison 2026"))
                // CriterionRequest : exemple + bornes 1..5 sur le poids.
                .andExpect(jsonPath("$.components.schemas.CriterionRequest.properties.weight.example")
                        .value(4))
                .andExpect(jsonPath("$.components.schemas.CriterionRequest.properties.weight.maximum")
                        .value(5))
                // ScoreRequest : exemple sur la note.
                .andExpect(jsonPath("$.components.schemas.ScoreRequest.properties.value.example")
                        .value(4.5));
    }

    @Test
    void apiDocs_isStructurallyGeneratable() throws Exception {
        // Conditions minimales pour qu'un générateur (openapi-generator) produise
        // un client TypeScript : des chemins et des schémas de composants nommés.
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/projects']").exists())
                .andExpect(jsonPath("$.paths['/api/projects/{projectId}/scores/{criterionId}/{optionId}']").exists())
                .andExpect(jsonPath("$.components.schemas.ProjectResponse").exists())
                .andExpect(jsonPath("$.components.schemas.ScoreResponse").exists());
    }
}
