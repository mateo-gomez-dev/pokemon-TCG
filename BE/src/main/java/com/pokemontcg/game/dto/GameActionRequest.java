package com.pokemontcg.game.dto;

import jakarta.validation.constraints.NotNull;

public record GameActionRequest(@NotNull Long playerId) {
}
