package io.pragmatic.ponderus.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * Payload d'un score (croisement critère × option).
 * {@code value} est volontairement nullable : {@code null} signifie
 * « pas encore noté », distinct d'un 0. La contrainte {@code @Digits}
 * reflète la colonne {@code NUMERIC(3,1)} pour éviter une erreur SQL.
 */
public record ScoreRequest(
        @Digits(integer = 2, fraction = 1)
        BigDecimal value,

        @Size(max = 10_000)
        String note
) {
}
