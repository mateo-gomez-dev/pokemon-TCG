package com.pokemontcg.card.domain;

import com.pokemontcg.card.persistence.CardEntity;

import java.util.Locale;
import java.util.Set;

public final class CardRules {

    public static final String POKEMON_SUPERTYPE = "Pokémon";
    public static final String ENERGY_SUPERTYPE = "Energy";
    public static final String BASIC_SUBTYPE = "Basic";

    private CardRules() {
    }

    public static boolean isBasicPokemon(CardEntity card) {
        return POKEMON_SUPERTYPE.equalsIgnoreCase(card.getSupertype()) && hasSubtype(card, BASIC_SUBTYPE);
    }

    public static boolean isBasicEnergy(CardEntity card) {
        return ENERGY_SUPERTYPE.equalsIgnoreCase(card.getSupertype()) && hasSubtype(card, BASIC_SUBTYPE);
    }

    public static boolean hasAnySubtype(CardEntity card, Set<String> subtypes) {
        if (card.getSubtypes() == null) {
            return false;
        }
        return card.getSubtypes().stream()
                .map(CardRules::normalize)
                .anyMatch(subtypes::contains);
    }

    public static boolean hasSubtype(CardEntity card, String subtype) {
        if (card.getSubtypes() == null) {
            return false;
        }
        String normalizedSubtype = normalize(subtype);
        return card.getSubtypes().stream()
                .map(CardRules::normalize)
                .anyMatch(normalizedSubtype::equals);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
