package com.pokemontcg.card.service;

import com.pokemontcg.card.client.PokemonTcgClient;
import com.pokemontcg.card.dto.CardImportResponse;
import com.pokemontcg.card.dto.PokemonTcgCardDto;
import com.pokemontcg.card.persistence.CardEntity;
import com.pokemontcg.card.persistence.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CardImportService {

    private static final String XY1_SET_ID = "xy1";

    private final PokemonTcgClient pokemonTcgClient;
    private final CardRepository cardRepository;

    public CardImportService(PokemonTcgClient pokemonTcgClient, CardRepository cardRepository) {
        this.pokemonTcgClient = pokemonTcgClient;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public CardImportResponse importXy1Cards() {
        List<PokemonTcgCardDto> cards = pokemonTcgClient.fetchXy1Cards();
        List<CardEntity> entities = cards.stream()
                .map(this::toEntity)
                .toList();

        cardRepository.saveAll(entities);
        return new CardImportResponse(XY1_SET_ID, entities.size());
    }

    private CardEntity toEntity(PokemonTcgCardDto card) {
        CardEntity entity = new CardEntity();
        entity.setId(card.getId());
        entity.setName(card.getName());
        entity.setSupertype(card.getSupertype());
        entity.setSubtypes(card.getSubtypes());
        entity.setHp(parseHp(card.getHp()));
        entity.setTypes(card.getTypes());
        entity.setEvolvesFrom(card.getEvolvesFrom());
        entity.setRules(card.getRules());
        entity.setAbilities(card.getAbilities());
        entity.setAttacks(card.getAttacks());
        entity.setWeaknesses(card.getWeaknesses());
        entity.setResistances(card.getResistances());
        entity.setRetreatCost(card.getRetreatCost());
        entity.setConvertedRetreatCost(card.getConvertedRetreatCost());
        entity.setNumber(card.getNumber());
        entity.setRarity(card.getRarity());
        entity.setImageSmallUrl(card.getImages() == null ? null : card.getImages().small());
        entity.setImageLargeUrl(card.getImages() == null ? null : card.getImages().large());
        entity.setRawJson(card.getRawJson());
        entity.setSetId(card.getSet() == null ? null : card.getSet().id());
        entity.setSetName(card.getSet() == null ? null : card.getSet().name());
        return entity;
    }

    private Integer parseHp(String hp) {
        if (hp == null || hp.isBlank()) {
            return null;
        }
        return Integer.parseInt(hp);
    }
}
