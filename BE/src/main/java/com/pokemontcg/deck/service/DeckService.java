package com.pokemontcg.deck.service;

import com.pokemontcg.card.persistence.CardEntity;
import com.pokemontcg.card.persistence.CardRepository;
import com.pokemontcg.deck.dto.DeckCardRequest;
import com.pokemontcg.deck.dto.DeckCardResponse;
import com.pokemontcg.deck.dto.DeckRequest;
import com.pokemontcg.deck.dto.DeckResponse;
import com.pokemontcg.deck.dto.DeckValidationResponse;
import com.pokemontcg.deck.persistence.DeckCardEntity;
import com.pokemontcg.deck.persistence.DeckEntity;
import com.pokemontcg.deck.persistence.DeckRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final DeckValidator deckValidator;

    public DeckService(DeckRepository deckRepository, CardRepository cardRepository, DeckValidator deckValidator) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.deckValidator = deckValidator;
    }

    @Transactional
    public DeckResponse createDeck(DeckRequest request) {
        DeckEntity deck = new DeckEntity();
        deck.setName(request.name());
        deck.replaceCards(toDeckCards(request.cards()));
        deck.setValid(deckValidator.validate(deck.getCards()).valid());
        return toResponse(deckRepository.save(deck));
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> getDecks() {
        return deckRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckResponse getDeck(Long id) {
        return toResponse(findDeck(id));
    }

    @Transactional
    public DeckResponse updateDeck(Long id, DeckRequest request) {
        DeckEntity deck = findDeck(id);
        deck.setName(request.name());
        deck.replaceCards(toDeckCards(request.cards()));
        deck.setValid(deckValidator.validate(deck.getCards()).valid());
        return toResponse(deckRepository.save(deck));
    }

    @Transactional
    public void deleteDeck(Long id) {
        DeckEntity deck = findDeck(id);
        deckRepository.delete(deck);
    }

    @Transactional
    public DeckValidationResponse validateDeck(Long id) {
        DeckEntity deck = findDeck(id);
        DeckValidationResponse response = deckValidator.validate(deck.getCards());
        deck.setValid(response.valid());
        return response;
    }

    private DeckEntity findDeck(Long id) {
        return deckRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mazo no encontrado"));
    }

    private List<DeckCardEntity> toDeckCards(List<DeckCardRequest> cardRequests) {
        Map<String, Integer> quantitiesByCardId = new LinkedHashMap<>();
        for (DeckCardRequest cardRequest : cardRequests) {
            quantitiesByCardId.merge(cardRequest.cardId(), cardRequest.quantity(), Integer::sum);
        }

        Map<String, CardEntity> cardsById = cardRepository.findAllById(quantitiesByCardId.keySet()).stream()
                .collect(Collectors.toMap(CardEntity::getId, Function.identity()));

        return quantitiesByCardId.entrySet().stream()
                .map(entry -> toDeckCard(findCard(cardsById, entry.getKey()), entry.getValue()))
                .toList();
    }

    private CardEntity findCard(Map<String, CardEntity> cardsById, String cardId) {
        CardEntity card = cardsById.get(cardId);
        if (card == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La carta " + cardId + " no existe en el cache local");
        }
        return card;
    }

    private DeckCardEntity toDeckCard(CardEntity card, int quantity) {
        DeckCardEntity deckCard = new DeckCardEntity();
        deckCard.setCard(card);
        deckCard.setQuantity(quantity);
        return deckCard;
    }

    private DeckResponse toResponse(DeckEntity deck) {
        List<DeckCardResponse> cards = deck.getCards().stream()
                .map(this::toCardResponse)
                .toList();

        int totalCards = deck.getCards().stream()
                .mapToInt(DeckCardEntity::getQuantity)
                .sum();

        return new DeckResponse(
                deck.getId(),
                deck.getName(),
                deck.isValid(),
                totalCards,
                deck.getCreatedAt(),
                deck.getUpdatedAt(),
                cards
        );
    }

    private DeckCardResponse toCardResponse(DeckCardEntity deckCard) {
        CardEntity card = deckCard.getCard();
        return new DeckCardResponse(
                card.getId(),
                card.getName(),
                card.getSupertype(),
                card.getSubtypes(),
                deckCard.getQuantity()
        );
    }
}
