package com.pokemontcg.deck.service;

import com.pokemontcg.card.persistence.CardEntity;
import com.pokemontcg.deck.dto.DeckValidationResponse;
import com.pokemontcg.deck.persistence.DeckCardEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeckValidatorTest {

    private final DeckValidator validator = new DeckValidator();

    @Test
    void validDeckHasSixtyCardsAndBasicPokemon() {
        DeckValidationResponse response = validator.validate(List.of(
                deckCard(card("xy1-1", "Venusaur-EX", "Pokémon", List.of("Basic", "EX")), 4),
                deckCard(card("xy1-2", "Professor Sycamore", "Trainer", List.of("Supporter")), 4),
                deckCard(card("xy1-3", "Potion", "Trainer", List.of("Item")), 4),
                deckCard(card("xy1-4", "Fire Energy", "Energy", List.of("Basic")), 48)
        ));

        assertThat(response.valid()).isTrue();
        assertThat(response.totalCards()).isEqualTo(60);
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void invalidDeckWhenTotalIsNotSixtyCards() {
        DeckValidationResponse response = validator.validate(List.of(
                deckCard(card("xy1-1", "Venusaur-EX", "Pokémon", List.of("Basic", "EX")), 1),
                deckCard(card("xy1-2", "Fire Energy", "Energy", List.of("Basic")), 1)
        ));

        assertThat(response.valid()).isFalse();
        assertThat(response.totalCards()).isEqualTo(2);
        assertThat(response.errors()).anyMatch(error -> error.contains("exactamente 60 cartas"));
    }

    @Test
    void invalidDeckWhenNonBasicEnergyExceedsFourCopiesByName() {
        DeckValidationResponse response = validator.validate(List.of(
                deckCard(card("xy1-1", "Venusaur-EX", "Pokémon", List.of("Basic", "EX")), 5),
                deckCard(card("xy1-2", "Fire Energy", "Energy", List.of("Basic")), 55)
        ));

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> error.contains("Venusaur-EX") && error.contains("4 copias"));
    }

    @Test
    void basicEnergyCanExceedFourCopies() {
        DeckValidationResponse response = validator.validate(List.of(
                deckCard(card("xy1-1", "Pikachu", "Pokémon", List.of("Basic")), 4),
                deckCard(card("xy1-2", "Lightning Energy", "Energy", List.of("Basic")), 56)
        ));

        assertThat(response.valid()).isTrue();
        assertThat(response.errors()).isEmpty();
    }

    @Test
    void xy1ModeDoesNotValidateAceSpecOrAsTactico() {
        DeckValidationResponse response = validator.validate(List.of(
                deckCard(card("xy1-1", "Pikachu", "Pokémon", List.of("Basic")), 4),
                deckCard(card("xy1-2", "Computer Search", "Trainer", List.of("Item", "ACE SPEC")), 2),
                deckCard(card("xy1-3", "Busqueda Computarizada", "Trainer", List.of("Item", "AS TÁCTICO")), 2),
                deckCard(card("xy1-4", "Lightning Energy", "Energy", List.of("Basic")), 52)
        ));

        assertThat(response.valid()).isTrue();
        assertThat(response.totalCards()).isEqualTo(60);
        assertThat(response.errors()).noneMatch(error -> error.contains("AS TACTICO") || error.contains("ACE SPEC"));
    }

    @Test
    void invalidDeckWhenItHasNoBasicPokemon() {
        DeckValidationResponse response = validator.validate(List.of(
                deckCard(card("xy1-1", "Ivysaur", "Pokémon", List.of("Stage 1")), 4),
                deckCard(card("xy1-2", "Fire Energy", "Energy", List.of("Basic")), 56)
        ));

        assertThat(response.valid()).isFalse();
        assertThat(response.errors()).anyMatch(error -> error.contains("Pokemon Basico"));
    }

    private DeckCardEntity deckCard(CardEntity card, int quantity) {
        DeckCardEntity deckCard = new DeckCardEntity();
        deckCard.setCard(card);
        deckCard.setQuantity(quantity);
        return deckCard;
    }

    private CardEntity card(String id, String name, String supertype, List<String> subtypes) {
        CardEntity card = new CardEntity();
        card.setId(id);
        card.setName(name);
        card.setSupertype(supertype);
        card.setSubtypes(subtypes);
        return card;
    }
}
