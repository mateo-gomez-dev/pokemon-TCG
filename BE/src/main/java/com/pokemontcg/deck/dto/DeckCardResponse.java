package com.pokemontcg.deck.dto;

import java.util.List;

public record DeckCardResponse(
        String cardId,
        String name,
        String supertype,
        List<String> subtypes,
        int quantity
) {
}
