package io.pragmatic.ponderus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de création / mise à jour d'un projet.
 */
@Schema(description = "Données de création ou de mise à jour d'un projet de comparaison.")
public record ProjectRequest(
        @Schema(description = "Nom du projet.", example = "Achat maison 2026", maxLength = 255)
        @NotBlank
        @Size(max = 255)
        String name,

        @Schema(
                description = "Seuil éliminatoire optionnel : une option dont un score sur un critère "
                        + "éliminatoire passe sous ce seuil est disqualifiée. Null = pas de seuil.",
                example = "50", nullable = true)
        Integer elimThreshold
) {
}
