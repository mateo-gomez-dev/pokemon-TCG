package com.pokemontcg.deck.service;

import com.pokemontcg.card.persistence.CardEntity;
import com.pokemontcg.deck.dto.DeckValidationResponse;
import com.pokemontcg.deck.persistence.DeckCardEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class DeckValidator {

    private static final int REQUIRED_TOTAL_CARDS = 60;
    private static final int MAX_COPIES_BY_NAME = 4;

    public DeckValidationResponse validate(List<DeckCardEntity> deckCards) {
        int totalCards = deckCards.stream()
                .mapToInt(DeckCardEntity::getQuantity)
                .sum();

        List<String> errors = new ArrayList<>();
        if (totalCards != REQUIRED_TOTAL_CARDS) {
            errors.add("El mazo debe tener exactamente 60 cartas. Total actual: " + totalCards + ".");
        }

        validateCopiesByName(deckCards, errors);
        validateAceSpec(deckCards, errors);
        validateBasicPokemon(deckCards, errors);

        return new DeckValidationResponse(errors.isEmpty(), totalCards, errors);
    }

    private void validateCopiesByName(List<DeckCardEntity> deckCards, List<String> errors) {
        Map<String, Integer> copiesByName = new HashMap<>();
        for (DeckCardEntity deckCard : deckCards) {
            CardEntity card = deckCard.getCard();
            if (isBasicEnergy(card)) {
                continue;
            }
            copiesByName.merge(card.getName(), deckCard.getQuantity(), Integer::sum);
        }

        copiesByName.entrySet().stream()
                .filter(entry -> entry.getValue() > MAX_COPIES_BY_NAME)
                .forEach(entry -> errors.add("La carta " + entry.getKey() + " supera el maximo de 4 copias."));
    }

    private void validateAceSpec(List<DeckCardEntity> deckCards, List<String> errors) {
        int aceSpecCount = deckCards.stream()
                .filter(deckCard -> hasSubtype(deckCard.getCard(), "ACE SPEC") || hasSubtype(deckCard.getCard(), "AS TACTICO") || hasSubtype(deckCard.getCard(), "AS TÁCTICO"))
                .mapToInt(DeckCardEntity::getQuantity)
                .sum();

        if (aceSpecCount > 1) {
            errors.add("El mazo puede tener como maximo 1 carta de AS TACTICO.");
        }
    }

    private void validateBasicPokemon(List<DeckCardEntity> deckCards, List<String> errors) {
        boolean hasBasicPokemon = deckCards.stream()
                .anyMatch(deckCard -> "Pokémon".equalsIgnoreCase(deckCard.getCard().getSupertype()) && hasSubtype(deckCard.getCard(), "Basic"));

        if (!hasBasicPokemon) {
            errors.add("El mazo debe tener al menos 1 Pokemon Basico.");
        }
    }

    private boolean isBasicEnergy(CardEntity card) {
        return "Energy".equalsIgnoreCase(card.getSupertype()) && hasSubtype(card, "Basic");
    }

    private boolean hasSubtype(CardEntity card, String subtype) {
        if (card.getSubtypes() == null) {
            return false;
        }
        String normalizedSubtype = normalize(subtype);
        return card.getSubtypes().stream()
                .map(this::normalize)
                .anyMatch(normalizedSubtype::equals);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
