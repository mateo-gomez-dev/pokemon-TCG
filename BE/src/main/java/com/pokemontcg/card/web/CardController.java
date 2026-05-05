package com.pokemontcg.card.web;

import com.pokemontcg.card.dto.CardImportResponse;
import com.pokemontcg.card.persistence.CardEntity;
import com.pokemontcg.card.persistence.CardRepository;
import com.pokemontcg.card.service.CardImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "Cache local de cartas Pokemon TCG")
public class CardController {

    private final CardImportService cardImportService;
    private final CardRepository cardRepository;

    public CardController(CardImportService cardImportService, CardRepository cardRepository) {
        this.cardImportService = cardImportService;
        this.cardRepository = cardRepository;
    }

    @PostMapping("/import/xy1")
    @Operation(summary = "Importa o actualiza el set XY1 desde pokemontcg.io")
    public CardImportResponse importXy1Cards() {
        return cardImportService.importXy1Cards();
    }

    @GetMapping
    @Operation(summary = "Lista las cartas cacheadas localmente")
    public List<CardEntity> getCards() {
        return cardRepository.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una carta cacheada por id")
    public ResponseEntity<CardEntity> getCardById(@PathVariable String id) {
        return cardRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
