package com.pokemontcg.game.web;

import com.pokemontcg.game.dto.AttachEnergyRequest;
import com.pokemontcg.game.dto.AttackRequest;
import com.pokemontcg.game.dto.CreateGameRequest;
import com.pokemontcg.game.dto.EvolvePokemonRequest;
import com.pokemontcg.game.dto.GameActionRequest;
import com.pokemontcg.game.dto.GameResponse;
import com.pokemontcg.game.dto.JoinGameRequest;
import com.pokemontcg.game.dto.PlayBasicPokemonRequest;
import com.pokemontcg.game.dto.PlayTrainerRequest;
import com.pokemontcg.game.dto.PromoteActiveRequest;
import com.pokemontcg.game.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@Tag(name = "Games", description = "Persistencia basica de partidas")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea una partida en estado WAITING")
    public GameResponse createGame(@Valid @RequestBody CreateGameRequest request) {
        return gameService.createGame(request);
    }

    @PostMapping("/{id}/join")
    @Operation(summary = "Une un segundo jugador a una partida WAITING")
    public GameResponse joinGame(@PathVariable Long id, @Valid @RequestBody JoinGameRequest request) {
        return gameService.joinGame(id, request);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Inicia una partida WAITING con 2 jugadores")
    public GameResponse startGame(@PathVariable Long id) {
        return gameService.startGame(id);
    }

    @PostMapping("/{id}/actions/draw")
    @Operation(summary = "Roba una carta del jugador actual")
    public GameResponse drawCard(@PathVariable Long id, @Valid @RequestBody GameActionRequest request) {
        return gameService.drawCard(id, request);
    }

    @PostMapping("/{id}/actions/end-turn")
    @Operation(summary = "Finaliza el turno del jugador actual")
    public GameResponse endTurn(@PathVariable Long id, @Valid @RequestBody GameActionRequest request) {
        return gameService.endTurn(id, request);
    }

    @PostMapping("/{id}/actions/play-basic-pokemon")
    @Operation(summary = "Juega un Pokemon Basico desde la mano al activo o la banca")
    public GameResponse playBasicPokemon(@PathVariable Long id, @Valid @RequestBody PlayBasicPokemonRequest request) {
        return gameService.playBasicPokemon(id, request);
    }

    @PostMapping("/{id}/actions/play-trainer")
    @Operation(summary = "Juega una carta Trainer desde la mano")
    public GameResponse playTrainer(@PathVariable Long id, @Valid @RequestBody PlayTrainerRequest request) {
        return gameService.playTrainer(id, request);
    }

    @PostMapping("/{id}/actions/attach-energy")
    @Operation(summary = "Une una Energia Basica desde la mano a un Pokemon en juego")
    public GameResponse attachEnergy(@PathVariable Long id, @Valid @RequestBody AttachEnergyRequest request) {
        return gameService.attachEnergy(id, request);
    }

    @PostMapping("/{id}/actions/promote-active")
    @Operation(summary = "Promueve un Pokemon de banca al puesto activo")
    public GameResponse promoteActive(@PathVariable Long id, @Valid @RequestBody PromoteActiveRequest request) {
        return gameService.promoteActive(id, request);
    }

    @PostMapping("/{id}/actions/evolve-pokemon")
    @Operation(summary = "Evoluciona un Pokemon en juego usando una carta de la mano")
    public GameResponse evolvePokemon(@PathVariable Long id, @Valid @RequestBody EvolvePokemonRequest request) {
        return gameService.evolvePokemon(id, request);
    }

    @PostMapping("/{id}/actions/attack")
    @Operation(summary = "Ataca con el Pokemon activo del jugador actual")
    public GameResponse attack(@PathVariable Long id, @Valid @RequestBody AttackRequest request) {
        return gameService.attack(id, request);
    }

    @GetMapping
    @Operation(summary = "Lista las partidas")
    public List<GameResponse> getGames() {
        return gameService.getGames();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una partida por id")
    public GameResponse getGame(@PathVariable Long id) {
        return gameService.getGame(id);
    }
}
