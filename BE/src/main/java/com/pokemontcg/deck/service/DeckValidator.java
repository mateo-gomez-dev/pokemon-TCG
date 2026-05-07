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
import java.util.Set;

@Component
public class DeckValidator {

    private static final int REQUIRED_TOTAL_CARDS = 60;
    private static final int MAX_COPIES_BY_NAME = 4;
    private static final int MAX_ACE_SPEC_CARDS = 1;
    private static final String POKEMON_SUPERTYPE = "Pokémon";
    private static final String ENERGY_SUPERTYPE = "Energy";
    private static final String BASIC_SUBTYPE = "Basic";
    private static final Set<String> ACE_SPEC_SUBTYPES = Set.of("ACE SPEC", "AS TACTICO", "AS TÁCTICO");

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
                .filter(deckCard -> hasAnySubtype(deckCard.getCard(), ACE_SPEC_SUBTYPES))
                .mapToInt(DeckCardEntity::getQuantity)
                .sum();

        if (aceSpecCount > MAX_ACE_SPEC_CARDS) {
            errors.add("El mazo puede tener como maximo " + MAX_ACE_SPEC_CARDS + " carta de AS TACTICO.");
        }
    }

    private void validateBasicPokemon(List<DeckCardEntity> deckCards, List<String> errors) {
        boolean hasBasicPokemon = deckCards.stream()
                .anyMatch(deckCard -> POKEMON_SUPERTYPE.equalsIgnoreCase(deckCard.getCard().getSupertype()) && hasSubtype(deckCard.getCard(), BASIC_SUBTYPE));

        if (!hasBasicPokemon) {
            errors.add("El mazo debe tener al menos 1 Pokemon Basico.");
        }
    }

    private boolean isBasicEnergy(CardEntity card) {
        return ENERGY_SUPERTYPE.equalsIgnoreCase(card.getSupertype()) && hasSubtype(card, BASIC_SUBTYPE);
    }

    private boolean hasAnySubtype(CardEntity card, Set<String> subtypes) {
        if (card.getSubtypes() == null) {
            return false;
        }
        return card.getSubtypes().stream()
                .map(this::normalize)
                .anyMatch(subtypes::contains);
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
