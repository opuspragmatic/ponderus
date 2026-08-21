package io.pragmatic.ponderus.dto;

import java.util.UUID;

import io.pragmatic.ponderus.domain.Criterion;

public record CriterionResponse(
        UUID id,
        UUID projectId,
        String label,
        Integer weight,
        boolean elim,
        Integer position
) {
    public static CriterionResponse from(Criterion criterion) {
        return new CriterionResponse(
                criterion.getId(),
                criterion.getProject().getId(),
                criterion.getLabel(),
                criterion.getWeight(),
                criterion.isElim(),
                criterion.getPosition()
        );
    }
}
