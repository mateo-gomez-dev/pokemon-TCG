package com.pokemontcg.deck.web;

import com.pokemontcg.deck.dto.DeckRequest;
import com.pokemontcg.deck.dto.DeckResponse;
import com.pokemontcg.deck.dto.DeckValidationResponse;
import com.pokemontcg.deck.service.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
@Tag(name = "Decks", description = "Deck Builder backend")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un mazo")
    public DeckResponse createDeck(@Valid @RequestBody DeckRequest request) {
        return deckService.createDeck(request);
    }

    @GetMapping
    @Operation(summary = "Lista los mazos")
    public List<DeckResponse> getDecks() {
        return deckService.getDecks();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un mazo por id")
    public DeckResponse getDeck(@PathVariable Long id) {
        return deckService.getDeck(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un mazo")
    public DeckResponse updateDeck(@PathVariable Long id, @Valid @RequestBody DeckRequest request) {
        return deckService.updateDeck(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un mazo")
    public void deleteDeck(@PathVariable Long id) {
        deckService.deleteDeck(id);
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Valida un mazo")
    public DeckValidationResponse validateDeck(@PathVariable Long id) {
        return deckService.validateDeck(id);
    }
}
