package com.pokemontcg.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlayBasicPokemonRequest(
        @NotNull Long playerId,
        @NotBlank String cardId
) {
}
