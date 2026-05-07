package com.pokemontcg.game.dto;

import java.util.List;
import java.util.Map;

public record GamePlayerResponse(
        Long id,
        String playerName,
        int playerOrder,
        Long deckId,
        String deckName,
        int deckRemaining,
        int handSize,
        int prizeCardsRemaining,
        int benchSize,
        int discardSize,
        boolean energyAttachedThisTurn,
        List<String> deckCardIds,
        List<String> handCardIds,
        List<String> prizeCardIds,
        List<String> benchCardIds,
        Map<String, List<String>> attachedEnergyCardIdsByPokemonCardId,
        Map<String, Integer> damageByPokemonCardId,
        List<String> discardCardIds
) {
}
