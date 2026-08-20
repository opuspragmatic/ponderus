package io.pragmatic.ponderus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de création / mise à jour d'un projet.
 */
public record ProjectRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        Integer elimThreshold
) {
}
