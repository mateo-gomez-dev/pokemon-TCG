package com.pokemontcg.deck.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCardRepository extends JpaRepository<DeckCardEntity, Long> {
}
