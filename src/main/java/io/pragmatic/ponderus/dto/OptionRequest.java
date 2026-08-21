package io.pragmatic.ponderus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de création / mise à jour d'une option.
 * Le projet de rattachement vient du chemin, pas du corps.
 * {@code position} est optionnel : si absent, l'option est ajoutée en fin de liste.
 */
public record OptionRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        Integer position
) {
}
