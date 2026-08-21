package io.pragmatic.ponderus.dto;

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
public record CriterionRequest(
        @NotBlank
        @Size(max = 255)
        String label,

        @NotNull
        @Min(1)
        @Max(5)
        Integer weight,

        Boolean elim,

        Integer position
) {
}
