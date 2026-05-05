package com.pokemontcg.deck.dto;

import java.util.List;

public record DeckValidationResponse(
        boolean valid,
        int totalCards,
        List<String> errors
) {
}
