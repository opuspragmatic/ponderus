package io.pragmatic.ponderus.dto;

import java.math.BigDecimal;
import java.util.UUID;

import io.pragmatic.ponderus.domain.Score;

/**
 * Vue d'un score. La liste des scores d'un projet, indexée côté frontend par
 * le couple (criterionId, optionId), reconstitue la matrice critère × option.
 */
public record ScoreResponse(
        UUID id,
        UUID criterionId,
        UUID optionId,
        BigDecimal value,
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
