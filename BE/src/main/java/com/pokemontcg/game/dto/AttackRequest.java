package com.pokemontcg.game.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AttackRequest(
        @NotNull Long playerId,
        @NotBlank String attackName
) {
}
