package com.pokemontcg.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttachEnergyRequest(
        @NotNull Long playerId,
        @NotBlank String energyCardId,
        String targetPokemonCardId,
        String pokemonInstanceId
) {
    public AttachEnergyRequest(Long playerId, String energyCardId, String targetPokemonCardId) {
        this(playerId, energyCardId, targetPokemonCardId, null);
    }
}
