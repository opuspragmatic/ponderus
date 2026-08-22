package io.pragmatic.ponderus.dto;

import java.util.UUID;

import io.pragmatic.ponderus.domain.Option;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représentation d'une option comparée dans un projet.")
public record OptionResponse(
        @Schema(description = "Identifiant de l'option.",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Identifiant du projet de rattachement.",
                example = "8d8e1a2b-1c3d-4e5f-9a0b-1c2d3e4f5a6b")
        UUID projectId,

        @Schema(description = "Libellé de l'option.", example = "Maison Bd Voltaire")
        String name,

        @Schema(description = "Position d'affichage (0 = première).", example = "0")
        Integer position
) {
    public static OptionResponse from(Option option){
        return new OptionResponse(
            option.getId(),
            option.getProject().getId(),
            option.getName(),
            option.getPosition()
        );
    }
}
