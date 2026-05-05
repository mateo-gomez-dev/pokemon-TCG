package com.pokemontcg.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttachEnergyRequest(
        @NotNull Long playerId,
        @NotBlank String energyCardId,
        @NotBlank String targetPokemonCardId
) {
}
