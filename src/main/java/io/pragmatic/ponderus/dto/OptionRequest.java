package io.pragmatic.ponderus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de création / mise à jour d'une option.
 * Le projet de rattachement vient du chemin, pas du corps.
 * {@code position} est optionnel : si absent, l'option est ajoutée en fin de liste.
 */
@Schema(description = "Données de création ou de mise à jour d'une option comparée.")
public record OptionRequest(
        @Schema(description = "Libellé de l'option.", example = "Maison Bd Voltaire", maxLength = 255)
        @NotBlank
        @Size(max = 255)
        String name,

        @Schema(
                description = "Position d'affichage (0 = première). Si absent, l'option est ajoutée "
                        + "en fin de liste.",
                example = "0", nullable = true)
        Integer position
) {
}
