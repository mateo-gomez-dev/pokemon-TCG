package com.pokemontcg.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record JoinGameRequest(
        @NotBlank String playerName,
        @NotNull Long deckId
) {
}
