package com.pokemontcg.card.dto;

import java.util.List;

public record PokemonTcgApiResponse(List<PokemonTcgCardDto> data) {
}
