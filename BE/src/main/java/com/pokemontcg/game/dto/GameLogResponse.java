package com.pokemontcg.game.dto;

import java.time.LocalDateTime;

public record GameLogResponse(
        Long id,
        Long playerId,
        String actionType,
        String message,
        LocalDateTime createdAt
) {
}
