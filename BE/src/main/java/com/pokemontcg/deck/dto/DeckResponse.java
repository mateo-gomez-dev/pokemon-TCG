package com.pokemontcg.deck.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DeckResponse(
        Long id,
        String name,
        boolean valid,
        int totalCards,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DeckCardResponse> cards
) {
}
