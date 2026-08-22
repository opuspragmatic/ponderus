package io.pragmatic.ponderus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de création / mise à jour d'un critère.
 * Le projet de rattachement vient du chemin, pas du corps.
 * {@code position} est optionnel : si absent, le critère est ajouté en fin de liste.
 * {@code elim} est optionnel : {@code false} par défaut.
 */
@Schema(description = "Données de création ou de mise à jour d'un critère de comparaison.")
public record CriterionRequest(
        @Schema(description = "Libellé du critère.", example = "Proximité transports", maxLength = 255)
        @NotBlank
        @Size(max = 255)
        String label,

        @Schema(description = "Poids / importance du critère, de 1 (faible) à 5 (fort).",
                example = "4", minimum = "1", maximum = "5")
        @NotNull
        @Min(1)
        @Max(5)
        Integer weight,

        @Schema(description = "Critère éliminatoire ? false par défaut.", example = "false", nullable = true)
        Boolean elim,

        @Schema(description = "Position d'affichage (0 = premier). Si absent, ajouté en fin de liste.",
                example = "0", nullable = true)
        Integer position
) {
}
