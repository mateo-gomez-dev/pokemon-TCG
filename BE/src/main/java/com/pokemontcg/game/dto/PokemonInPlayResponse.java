package com.pokemontcg.game.dto;

import java.util.List;

public record PokemonInPlayResponse(
        String instanceId,
        String cardId,
        String name,
        Integer hp,
        int damage,
        int remainingHp,
        List<String> attachedEnergyCardIds,
        int attachedEnergyCount,
        String attachedToolCardId
) {
}
