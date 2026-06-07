package com.pokemontcg.game.dto;

import com.pokemontcg.game.persistence.GameStatus;
import com.pokemontcg.game.persistence.TurnPhase;

import java.time.LocalDateTime;
import java.util.List;

public record GameResponse(
        Long id,
        GameStatus status,
        TurnPhase turnPhase,
        Long currentPlayerId,
        Long winnerPlayerId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        List<GamePlayerResponse> players,
        List<GameLogResponse> logs
) {
}
