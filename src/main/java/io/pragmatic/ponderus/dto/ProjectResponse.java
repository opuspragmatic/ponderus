package io.pragmatic.ponderus.dto;

import java.time.Instant;
import java.util.UUID;

import io.pragmatic.ponderus.domain.Project;

/**
 * Vue exposée d'un projet. On n'expose jamais l'entité JPA directement
 * (pas de user, pas de collections lazy).
 */
public record ProjectResponse(
        UUID id,
        String name,
        Integer elimThreshold,
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
