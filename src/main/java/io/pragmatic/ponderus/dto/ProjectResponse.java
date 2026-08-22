package io.pragmatic.ponderus.dto;

import java.time.Instant;
import java.util.UUID;

import io.pragmatic.ponderus.domain.Project;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Vue exposée d'un projet. On n'expose jamais l'entité JPA directement
 * (pas de user, pas de collections lazy).
 */
@Schema(description = "Représentation d'un projet de comparaison.")
public record ProjectResponse(
        @Schema(description = "Identifiant du projet.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Nom du projet.", example = "Achat maison 2026")
        String name,

        @Schema(description = "Seuil éliminatoire, ou null si absent.", example = "50", nullable = true)
        Integer elimThreshold,

        @Schema(description = "Date de création (UTC).", example = "2026-08-21T10:15:30Z")
        Instant createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getElimThreshold(),
                project.getCreatedAt()
        );
    }
}
