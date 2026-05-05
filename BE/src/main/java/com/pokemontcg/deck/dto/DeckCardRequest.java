package com.pokemontcg.deck.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DeckCardRequest(
        @NotBlank String cardId,
        @Min(1) int quantity
) {
}
