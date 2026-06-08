package com.pokemontcg.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvolvePokemonRequest(
        @NotNull Long playerId,
        @NotBlank String evolutionCardId,
        @NotBlank String targetPokemonInstanceId
) {
}
