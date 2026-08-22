package io.pragmatic.ponderus.dto;

import java.math.BigDecimal;
import java.util.UUID;

import io.pragmatic.ponderus.domain.Score;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Vue d'un score. La liste des scores d'un projet, indexée côté frontend par
 * le couple (criterionId, optionId), reconstitue la matrice critère × option.
 */
@Schema(description = "Note d'un croisement critère × option. La liste des scores d'un projet, "
        + "indexée par (criterionId, optionId), reconstitue la matrice.")
public record ScoreResponse(
        @Schema(description = "Identifiant du score.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identifiant du critère.",
                example = "8d8e1a2b-1c3d-4e5f-9a0b-1c2d3e4f5a6b")
        UUID criterionId,

        @Schema(description = "Identifiant de l'option.",
                example = "6b1e9c7d-2f4a-4b8c-9d0e-1a2b3c4d5e6f")
        UUID optionId,

        @Schema(description = "Note (NUMERIC(3,1)), ou null si pas encore noté.",
                example = "4.5", nullable = true)
        BigDecimal value,

        @Schema(description = "Commentaire libre, ou null.",
                example = "Quartier calme mais loin du travail", nullable = true)
        String note
) {
    public static ScoreResponse from(Score score) {
        return new ScoreResponse(
                score.getId(),
                score.getCriterion().getId(),
                score.getOption().getId(),
                score.getValue(),
                score.getNote()
        );
    }
}
