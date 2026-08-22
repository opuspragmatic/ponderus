package io.pragmatic.ponderus.dto;

import java.util.UUID;

import io.pragmatic.ponderus.domain.Criterion;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représentation d'un critère de comparaison d'un projet.")
public record CriterionResponse(
        @Schema(description = "Identifiant du critère.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identifiant du projet de rattachement.",
                example = "8d8e1a2b-1c3d-4e5f-9a0b-1c2d3e4f5a6b")
        UUID projectId,

        @Schema(description = "Libellé du critère.", example = "Proximité transports")
        String label,

        @Schema(description = "Poids du critère (1 à 5).", example = "4")
        Integer weight,

        @Schema(description = "Critère éliminatoire ?", example = "false")
        boolean elim,

        @Schema(description = "Position d'affichage (0 = premier).", example = "0")
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
