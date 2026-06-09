package com.pokemontcg.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlayTrainerRequest(
        @NotNull Long playerId,
        @NotBlank String trainerCardId,
        String targetPokemonInstanceId,
        Long targetPlayerId
) {
}
