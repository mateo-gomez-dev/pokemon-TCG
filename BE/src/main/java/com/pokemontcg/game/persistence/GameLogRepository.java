package com.pokemontcg.game.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameLogRepository extends JpaRepository<GameLogEntity, Long> {
}
