package io.pragmatic.ponderus.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * Payload d'un score (croisement critère × option).
 * {@code value} est volontairement nullable : {@code null} signifie
 * « pas encore noté », distinct d'un 0. La contrainte {@code @Digits}
 * reflète la colonne {@code NUMERIC(3,1)} pour éviter une erreur SQL.
 */
@Schema(description = "Note attribuée au croisement d'un critère et d'une option.")
public record ScoreRequest(
        @Schema(
                description = "Note, jusqu'à 2 chiffres avant la virgule et 1 après (NUMERIC(3,1)). "
                        + "null signifie « pas encore noté », distinct d'un 0.",
                example = "4.5", nullable = true)
        @Digits(integer = 2, fraction = 1)
        BigDecimal value,

        @Schema(description = "Commentaire libre optionnel.",
                example = "Quartier calme mais loin du travail", nullable = true, maxLength = 10000)
        @Size(max = 10_000)
        String note
) {
}
