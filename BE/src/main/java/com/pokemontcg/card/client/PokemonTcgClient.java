package com.pokemontcg.card.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pokemontcg.card.dto.PokemonTcgCardDto;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class PokemonTcgClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public PokemonTcgClient(RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.baseUrl("https://api.pokemontcg.io/v2").build();
        this.objectMapper = objectMapper;
    }

    public List<PokemonTcgCardDto> fetchXy1Cards() {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards")
                        .queryParam("q", "set.id:xy1")
                        .queryParam("pageSize", 250)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("data") || !response.get("data").isArray()) {
            return List.of();
        }

        List<PokemonTcgCardDto> cards = new ArrayList<>();
        for (JsonNode cardNode : response.get("data")) {
            cards.add(toCardDto(cardNode));
        }
        return cards;
    }

    private PokemonTcgCardDto toCardDto(JsonNode cardNode) {
        try {
            PokemonTcgCardDto card = objectMapper.treeToValue(cardNode, PokemonTcgCardDto.class);
            card.setRawJson(cardNode);
            return card;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo mapear una carta de pokemontcg.io", exception);
        }
    }
}
