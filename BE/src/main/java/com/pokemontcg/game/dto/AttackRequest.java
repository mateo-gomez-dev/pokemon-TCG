package com.pokemontcg.game.dto;

import jakarta.validation.constraints.NotNull;

public record AttackRequest(
        @NotNull Long playerId,
        String attackName,
        Integer attackIndex
) {
    public AttackRequest(Long playerId, String attackName) {
        this(playerId, attackName, null);
    }
}
