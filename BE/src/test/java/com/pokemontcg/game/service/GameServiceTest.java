package com.pokemontcg.game.service;

import com.pokemontcg.card.persistence.CardEntity;
import com.pokemontcg.card.persistence.CardRepository;
import com.pokemontcg.deck.dto.DeckValidationResponse;
import com.pokemontcg.deck.persistence.DeckCardEntity;
import com.pokemontcg.deck.persistence.DeckEntity;
import com.pokemontcg.deck.persistence.DeckRepository;
import com.pokemontcg.deck.service.DeckValidator;
import com.pokemontcg.game.dto.AttachEnergyRequest;
import com.pokemontcg.game.dto.AttackRequest;
import com.pokemontcg.game.dto.CreateGameRequest;
import com.pokemontcg.game.dto.GameActionRequest;
import com.pokemontcg.game.dto.GameResponse;
import com.pokemontcg.game.dto.JoinGameRequest;
import com.pokemontcg.game.dto.PlayBasicPokemonRequest;
import com.pokemontcg.game.persistence.GameEntity;
import com.pokemontcg.game.persistence.GameLogEntity;
import com.pokemontcg.game.persistence.GamePlayerEntity;
import com.pokemontcg.game.persistence.GameRepository;
import com.pokemontcg.game.persistence.GameStatus;
import com.pokemontcg.game.persistence.PokemonInPlay;
import com.pokemontcg.game.persistence.PokemonZone;
import com.pokemontcg.game.persistence.TurnPhase;
import com.pokemontcg.game.dto.PromoteActiveRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private GameRepository gameRepository;

    @Mock
    private DeckRepository deckRepository;

    @Mock
    private DeckValidator deckValidator;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private GameService gameService;

    @Test
    void createGameCreatesWaitingGameWithOnePlayer() {
        DeckEntity deck = deck(10L, "Mazo inicial");
        when(deckRepository.findById(10L)).thenReturn(Optional.of(deck));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity game = invocation.getArgument(0);
            game.setId(1L);
            game.getPlayers().getFirst().setId(100L);
            game.getLogs().getFirst().setId(1000L);
            return game;
        });

        GameResponse response = gameService.createGame(new CreateGameRequest("Ash", 10L));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(GameStatus.WAITING);
        assertThat(response.players()).hasSize(1);
        assertThat(response.players().getFirst().playerName()).isEqualTo("Ash");
        assertThat(response.players().getFirst().deckId()).isEqualTo(10L);
        assertThat(response.logs()).hasSize(1);
        assertThat(response.logs().getFirst().actionType()).isEqualTo("CREATE_GAME");

        ArgumentCaptor<GameEntity> gameCaptor = ArgumentCaptor.forClass(GameEntity.class);
        verify(gameRepository).save(gameCaptor.capture());
        assertThat(gameCaptor.getValue().getStatus()).isEqualTo(GameStatus.WAITING);
        assertThat(gameCaptor.getValue().getPlayers()).hasSize(1);
    }

    @Test
    void createGameFailsWhenDeckDoesNotExist() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.createGame(new CreateGameRequest("Misty", 99L)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Mazo no encontrado");
    }

    @Test
    void getGamesReturnsMappedGames() {
        GameEntity game = new GameEntity();
        game.setId(1L);
        game.setStatus(GameStatus.WAITING);
        when(gameRepository.findAll()).thenReturn(List.of(game));

        List<GameResponse> response = gameService.getGames();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(1L);
        assertThat(response.getFirst().status()).isEqualTo(GameStatus.WAITING);
    }

    @Test
    void joinGameAddsSecondPlayerAndJoinLog() {
        GameEntity game = waitingGameWithPlayer(1L, deck(10L, "Mazo 1"));
        DeckEntity secondDeck = deck(20L, "Mazo 2");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(deckRepository.findById(20L)).thenReturn(Optional.of(secondDeck));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity savedGame = invocation.getArgument(0);
            savedGame.getPlayers().get(1).setId(200L);
            savedGame.getLogs().get(1).setId(2000L);
            return savedGame;
        });

        GameResponse response = gameService.joinGame(1L, new JoinGameRequest("Misty", 20L));

        assertThat(response.status()).isEqualTo(GameStatus.WAITING);
        assertThat(response.players()).hasSize(2);
        assertThat(response.players().get(1).playerName()).isEqualTo("Misty");
        assertThat(response.players().get(1).playerOrder()).isEqualTo(2);
        assertThat(response.players().get(1).deckId()).isEqualTo(20L);
        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(1).actionType()).isEqualTo("JOIN_GAME");
    }

    @Test
    void joinGameFailsWhenGameDoesNotExist() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.joinGame(99L, new JoinGameRequest("Misty", 20L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void joinGameFailsWhenGameIsNotWaiting() {
        GameEntity game = waitingGameWithPlayer(1L, deck(10L, "Mazo 1"));
        game.setStatus(GameStatus.ACTIVE);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.joinGame(1L, new JoinGameRequest("Misty", 20L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void joinGameFailsWhenGameAlreadyHasTwoPlayers() {
        GameEntity game = waitingGameWithPlayer(1L, deck(10L, "Mazo 1"));
        game.addPlayer(player(200L, "Brock", 2, deck(20L, "Mazo 2")));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.joinGame(1L, new JoinGameRequest("Misty", 30L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void joinGameFailsWhenDeckDoesNotExist() {
        GameEntity game = waitingGameWithPlayer(1L, deck(10L, "Mazo 1"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.joinGame(1L, new JoinGameRequest("Misty", 99L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void startGameActivatesGameAndDealsInitialCards() {
        DeckEntity firstDeck = validDeck(10L, "Mazo 1");
        DeckEntity secondDeck = validDeck(20L, "Mazo 2");
        GameEntity game = waitingGameWithPlayer(1L, firstDeck);
        game.addPlayer(player(200L, "Misty", 2, secondDeck));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(deckValidator.validate(firstDeck.getCards())).thenReturn(new DeckValidationResponse(true, 60, List.of()));
        when(deckValidator.validate(secondDeck.getCards())).thenReturn(new DeckValidationResponse(true, 60, List.of()));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity savedGame = invocation.getArgument(0);
            savedGame.getLogs().get(1).setId(2000L);
            return savedGame;
        });

        GameResponse response = gameService.startGame(1L);

        assertThat(response.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(response.currentPlayerId()).isEqualTo(100L);
        assertThat(response.turnPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(response.startedAt()).isNotNull();
        assertThat(response.players()).hasSize(2);
        assertThat(response.players()).allSatisfy(player -> {
            assertThat(player.handSize()).isEqualTo(7);
            assertThat(player.prizeCardsRemaining()).isEqualTo(6);
            assertThat(player.deckRemaining()).isEqualTo(47);
            assertThat(player.discardSize()).isZero();
            assertThat(player.activePokemonCardId()).isNull();
            assertThat(player.benchCardIds()).isEmpty();
        });
        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(1).actionType()).isEqualTo("START_GAME");
    }

    @Test
    void startGameFailsWhenGameDoesNotExist() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.startGame(99L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void startGameFailsWhenGameIsNotWaiting() {
        GameEntity game = waitingGameWithPlayer(1L, validDeck(10L, "Mazo 1"));
        game.setStatus(GameStatus.ACTIVE);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.startGame(1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void startGameFailsWhenGameDoesNotHaveTwoPlayers() {
        GameEntity game = waitingGameWithPlayer(1L, validDeck(10L, "Mazo 1"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.startGame(1L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void startGameFailsWhenAnyDeckIsInvalid() {
        DeckEntity firstDeck = validDeck(10L, "Mazo 1");
        DeckEntity secondDeck = validDeck(20L, "Mazo 2");
        GameEntity game = waitingGameWithPlayer(1L, firstDeck);
        game.addPlayer(player(200L, "Misty", 2, secondDeck));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(deckValidator.validate(firstDeck.getCards())).thenReturn(new DeckValidationResponse(true, 60, List.of()));
        when(deckValidator.validate(secondDeck.getCards())).thenReturn(new DeckValidationResponse(false, 2, List.of("El mazo debe tener exactamente 60 cartas.")));

        assertThatThrownBy(() -> gameService.startGame(1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El mazo de Misty no es valido")
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void drawCardMovesOneCardFromDeckToHandAndSetsMainPhase() {
        GameEntity game = activeGame();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity savedGame = invocation.getArgument(0);
            savedGame.getLogs().get(1).setId(2000L);
            return savedGame;
        });

        GameResponse response = gameService.drawCard(1L, new GameActionRequest(100L));

        assertThat(response.turnPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(response.currentPlayerId()).isEqualTo(100L);
        assertThat(response.players().getFirst().handSize()).isEqualTo(8);
        assertThat(response.players().getFirst().deckRemaining()).isEqualTo(46);
        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(1).playerId()).isEqualTo(100L);
        assertThat(response.logs().get(1).actionType()).isEqualTo("DRAW_CARD");
    }

    @Test
    void drawCardFailsWhenGameDoesNotExist() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.drawCard(99L, new GameActionRequest(100L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void drawCardFailsWhenGameIsNotActive() {
        GameEntity game = activeGame();
        game.setStatus(GameStatus.WAITING);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.drawCard(1L, new GameActionRequest(100L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void drawCardFailsWhenPlayerIsNotCurrentPlayer() {
        GameEntity game = activeGame();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.drawCard(1L, new GameActionRequest(200L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void drawCardWithEmptyDeckFinishesGameAndOpponentWins() {
        GameEntity game = activeGame();
        game.getPlayers().getFirst().setDeckCardIds(List.of());
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.drawCard(1L, new GameActionRequest(100L));

        assertThat(response.status()).isEqualTo(GameStatus.FINISHED);
        assertThat(response.winnerPlayerId()).isEqualTo(200L);
        assertThat(response.finishedAt()).isNotNull();
        assertThat(response.turnPhase()).isNull();
        assertThat(response.logs()).extracting("actionType").contains("VICTORY_DECK_OUT");
    }

    @Test
    void endTurnChangesCurrentPlayerAndSetsDrawPhase() {
        GameEntity game = activeGame();
        game.setTurnPhase(TurnPhase.MAIN);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity savedGame = invocation.getArgument(0);
            savedGame.getLogs().get(1).setId(2000L);
            return savedGame;
        });

        GameResponse response = gameService.endTurn(1L, new GameActionRequest(100L));

        assertThat(response.currentPlayerId()).isEqualTo(200L);
        assertThat(response.turnPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(1).playerId()).isEqualTo(100L);
        assertThat(response.logs().get(1).actionType()).isEqualTo("END_TURN");
    }

    @Test
    void endTurnFailsWhenGameDoesNotExist() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.endTurn(99L, new GameActionRequest(100L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void endTurnFailsWhenGameIsNotActive() {
        GameEntity game = activeGame();
        game.setStatus(GameStatus.WAITING);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.endTurn(1L, new GameActionRequest(100L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void endTurnFailsWhenPlayerIsNotCurrentPlayer() {
        GameEntity game = activeGame();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.endTurn(1L, new GameActionRequest(200L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void playBasicPokemonMovesCardFromHandToActiveWhenPlayerHasNoActivePokemon() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        CardEntity basicPokemon = card("xy1-1", "Pikachu", "Pokémon", List.of("Basic"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(basicPokemon));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity savedGame = invocation.getArgument(0);
            savedGame.getLogs().get(1).setId(2000L);
            return savedGame;
        });

        GameResponse response = gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1"));

        assertThat(response.players().getFirst().handSize()).isEqualTo(7);
        assertThat(response.players().getFirst().activePokemonCardId()).isEqualTo("xy1-1");
        assertThat(response.players().getFirst().benchSize()).isZero();
        assertThat(response.players().getFirst().benchCardIds()).isEmpty();
        assertThat(response.players().getFirst().handCardIds()).doesNotContain("xy1-1");
        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(1).actionType()).isEqualTo("PLAY_BASIC_POKEMON");
        assertThat(response.logs().get(1).message()).contains("Pokemon activo");
    }

    @Test
    void playBasicPokemonMovesCardFromHandToBenchWhenPlayerAlreadyHasActivePokemon() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        game.getPlayers().getFirst().setActivePokemonCardId("xy1-active");
        CardEntity basicPokemon = card("xy1-1", "Pikachu", "Pokémon", List.of("Basic"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(basicPokemon));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity savedGame = invocation.getArgument(0);
            savedGame.getLogs().get(1).setId(2000L);
            return savedGame;
        });

        GameResponse response = gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1"));

        assertThat(response.players().getFirst().activePokemonCardId()).isEqualTo("xy1-active");
        assertThat(response.players().getFirst().benchCardIds()).containsExactly("xy1-1");
        assertThat(response.players().getFirst().benchCardIds()).doesNotContain("xy1-active");
        assertThat(response.players().getFirst().handCardIds()).doesNotContain("xy1-1");
        assertThat(response.logs().get(1).message()).contains("banca");
    }

    @Test
    void playBasicPokemonCreatesDifferentInstancesForRepeatedCard() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        game.getPlayers().getFirst().getHandCardIds().add("xy1-1");
        CardEntity basicPokemon = card("xy1-1", "Pikachu", "Pokémon", List.of("Basic"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(basicPokemon));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse firstResponse = gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1"));
        GameResponse secondResponse = gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1"));

        String activeInstanceId = secondResponse.players().getFirst().activePokemonInstanceId();
        String benchInstanceId = secondResponse.players().getFirst().benchPokemon().getFirst().instanceId();
        assertThat(firstResponse.players().getFirst().activePokemonCardId()).isEqualTo("xy1-1");
        assertThat(secondResponse.players().getFirst().activePokemonCardId()).isEqualTo("xy1-1");
        assertThat(secondResponse.players().getFirst().benchPokemon().getFirst().cardId()).isEqualTo("xy1-1");
        assertThat(activeInstanceId).isNotBlank();
        assertThat(benchInstanceId).isNotBlank();
        assertThat(activeInstanceId).isNotEqualTo(benchInstanceId);
    }

    @Test
    void playBasicPokemonFailsWhenGameDoesNotExist() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.playBasicPokemon(99L, new PlayBasicPokemonRequest(100L, "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void playBasicPokemonFailsWhenGameIsNotActive() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        game.setStatus(GameStatus.WAITING);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void playBasicPokemonFailsWhenPlayerIsNotCurrentPlayer() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(200L, "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void playBasicPokemonFailsWhenPhaseIsNotMain() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        game.setTurnPhase(TurnPhase.DRAW);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void playBasicPokemonFailsWhenCardIsNotInHand() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-99")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void playBasicPokemonFailsWhenCardDoesNotExist() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void playBasicPokemonFailsWhenCardIsNotPokemon() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        CardEntity trainer = card("xy1-1", "Potion", "Trainer", List.of("Item"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(trainer));

        assertThatThrownBy(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void playBasicPokemonFailsWhenPokemonIsNotBasic() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        CardEntity stageOnePokemon = card("xy1-1", "Raichu", "Pokémon", List.of("Stage 1"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(stageOnePokemon));

        assertThatThrownBy(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void playBasicPokemonFailsWhenBenchIsFull() {
        GameEntity game = activeGameInMainPhaseWithBasicPokemonInHand();
        game.getPlayers().getFirst().setActivePokemonCardId("xy1-active");
        game.getPlayers().getFirst().setBenchCardIds(new java.util.ArrayList<>(List.of("b1", "b2", "b3", "b4", "b5")));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attachEnergyMovesBasicEnergyFromHandToActivePokemon() {
        GameEntity game = activeGameWithActiveAndEnergyInHand();
        CardEntity energy = card("xy1-132", "Lightning Energy", "Energy", List.of("Basic"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-132")).thenReturn(Optional.of(energy));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> {
            GameEntity savedGame = invocation.getArgument(0);
            savedGame.getLogs().get(1).setId(2000L);
            return savedGame;
        });

        GameResponse response = gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1"));

        assertThat(response.players().getFirst().handCardIds()).doesNotContain("xy1-132");
        assertThat(response.players().getFirst().handSize()).isEqualTo(7);
        assertThat(response.players().getFirst().energyAttachedThisTurn()).isTrue();
        assertThat(response.players().getFirst().attachedEnergyCardIdsByPokemonCardId())
                .containsEntry("xy1-1", List.of("xy1-132"));
        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(1).actionType()).isEqualTo("ATTACH_ENERGY");
    }

    @Test
    void attachEnergyMovesBasicEnergyFromHandToBenchPokemon() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        CardEntity energy = card("xy1-132", "Lightning Energy", "Energy", List.of("Basic"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-132")).thenReturn(Optional.of(energy));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-3"));

        assertThat(response.players().getFirst().attachedEnergyCardIdsByPokemonCardId())
                .containsEntry("xy1-3", List.of("xy1-132"));
    }

    @Test
    void attachEnergyUsesPokemonInstanceIdWhenRepeatedCardsAreInPlay() {
        GameEntity game = activeGameWithRepeatedPokemonInstancesAndEnergyInHand();
        CardEntity energy = card("xy1-132", "Lightning Energy", "Energy", List.of("Basic"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-132")).thenReturn(Optional.of(energy));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", null, "bench-copy"));

        assertThat(response.players().getFirst().attachedEnergyCardIdsByPokemonInstanceId())
                .containsEntry("bench-copy", List.of("xy1-132"));
        assertThat(response.players().getFirst().attachedEnergyCardIdsByPokemonInstanceId())
                .containsEntry("active-copy", List.of());
    }

    @Test
    void attachEnergyRejectsAmbiguousCardIdWhenRepeatedCardsAreInPlay() {
        GameEntity game = activeGameWithRepeatedPokemonInstancesAndEnergyInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pokemonInstanceId")
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void attachEnergyFailsWhenGameDoesNotExist() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.attachEnergy(99L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void attachEnergyFailsWhenGameIsNotActive() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        game.setStatus(GameStatus.WAITING);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attachEnergyFailsWhenPlayerIsNotCurrentPlayer() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(200L, "xy1-132", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attachEnergyFailsWhenPhaseIsNotMain() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        game.setTurnPhase(TurnPhase.DRAW);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attachEnergyFailsWhenEnergyIsNotInHand() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-133", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attachEnergyFailsWhenEnergyDoesNotExist() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-132")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void attachEnergyFailsWhenCardIsNotEnergy() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        CardEntity pokemon = card("xy1-132", "Pikachu", "Pokémon", List.of("Basic"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-132")).thenReturn(Optional.of(pokemon));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void attachEnergyFailsWhenEnergyIsNotBasic() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        CardEntity specialEnergy = card("xy1-132", "Special Energy", "Energy", List.of("Special"));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findById("xy1-132")).thenReturn(Optional.of(specialEnergy));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void attachEnergyFailsWhenTargetPokemonIsNotInPlay() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-99")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attachEnergyFailsWhenPokemonInstanceIsNotInPlay() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", null, "missing-instance")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attachEnergyFailsWhenEnergyWasAlreadyAttachedThisTurn() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        game.getPlayers().getFirst().setEnergyAttachedThisTurn(true);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attackAppliesDamageAndEndsTurn() {
        GameEntity game = activeGameReadyForAttack();
        CardEntity attacker = pokemonWithAttack("xy1-1", "Pikachu", 60, "Gnaw", 1, "30");
        CardEntity defender = card("xy1-2", "Bulbasaur", "Pokémon", List.of("Basic"));
        defender.setHp(70);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("xy1-132")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Gnaw"));

        assertThat(response.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(response.turnPhase()).isEqualTo(TurnPhase.DRAW);
        assertThat(response.currentPlayerId()).isEqualTo(200L);
        assertThat(response.players().get(1).activePokemonCardId()).isEqualTo("xy1-2");
        assertThat(response.players().get(1).activePokemon().remainingHp()).isEqualTo(40);
        assertThat(response.players().get(1).damageByPokemonCardId()).containsEntry("xy1-2", 30);
        assertThat(response.logs().get(1).actionType()).isEqualTo("ATTACK");
    }

    @Test
    void attackAppliesDamageOnlyToDefenderActiveInstance() {
        GameEntity game = activeGameWithRepeatedDefenderInstancesReadyForAttack();
        CardEntity attacker = pokemonWithAttack("xy1-1", "Pikachu", 60, "Gnaw", 1, "30");
        CardEntity defender = card("xy1-2", "Bulbasaur", "Pokémon", List.of("Basic"));
        defender.setHp(70);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("xy1-132")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Gnaw"));

        assertThat(response.players().get(1).damageByPokemonInstanceId()).containsEntry("defender-active", 30);
        assertThat(response.players().get(1).damageByPokemonInstanceId()).containsEntry("defender-bench", 0);
    }

    @Test
    void attackWithoutCostWorksWithoutEnergy() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Free Hit", List.of(), "10");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Free Hit"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(10);
        assertThat(response.currentPlayerId()).isEqualTo(200L);
    }

    @Test
    void attackWithGrassCostFailsWithoutEnergy() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Leaf Hit", List.of("Grass"), "30");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Leaf Hit")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No hay suficiente energia")
                .hasMessageContaining("Requiere: Grass")
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertEnergyFailureDidNotChangeCombatState(game);
    }

    @Test
    void attackWithGrassCostWorksWithGrassEnergy() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of("grass-1"));
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Leaf Hit", List.of("Grass"), "30");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("grass-1")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Leaf Hit"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(30);
    }

    @Test
    void attackWithColorlessCostWorksWithAnyEnergy() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of("fire-1"));
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Tackle", List.of("Colorless"), "20");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, energy("fire-1", "Fire Energy", "Fire")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Tackle"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(20);
    }

    @Test
    void attackWithGrassAndColorlessCostWorksWithGrassAndAnyEnergy() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of("grass-1", "fire-1"));
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Vine Hit", List.of("Grass", "Colorless"), "40");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("grass-1"), energy("fire-1", "Fire Energy", "Fire")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Vine Hit"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(40);
    }

    @Test
    void attackWithGrassAndColorlessCostFailsWithOnlyGrassEnergy() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of("grass-1"));
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Vine Hit", List.of("Grass", "Colorless"), "40");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("grass-1")));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Vine Hit")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No hay suficiente energia")
                .hasMessageContaining("Requiere: Grass, Colorless");
        assertEnergyFailureDidNotChangeCombatState(game);
    }

    @Test
    void attackWithTwoGrassCostFailsWithOnlyOneGrassEnergy() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of("grass-1"));
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Double Leaf", List.of("Grass", "Grass"), "50");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("grass-1")));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Double Leaf")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No hay suficiente energia")
                .hasMessageContaining("Requiere: Grass, Grass");
        assertEnergyFailureDidNotChangeCombatState(game);
    }

    @Test
    void attackWithTwoGrassCostWorksWithTwoGrassEnergy() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of("grass-1", "grass-2"));
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Double Leaf", List.of("Grass", "Grass"), "50");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("grass-1"), grassEnergy("grass-2")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Double Leaf"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(50);
    }

    @Test
    void colorlessEnergyDoesNotPaySpecificGrassCost() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of("colorless-1"));
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Leaf Hit", List.of("Grass"), "30");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, energy("colorless-1", "Double Colorless Energy", "Colorless")));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Leaf Hit")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No hay suficiente energia")
                .hasMessageContaining("Energias unidas: Colorless");
        assertEnergyFailureDidNotChangeCombatState(game);
    }

    @Test
    void attackOnlyUsesEnergyAttachedToAttackerInstance() {
        GameEntity game = activeGameWithRepeatedAttackerInstancesAndBenchEnergy();
        CardEntity attacker = pokemonWithAttackCost("xy1-1", "Pikachu", 60, "Leaf Hit", List.of("Grass"), "30");
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("grass-1")));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Leaf Hit")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No hay suficiente energia");
        assertEnergyFailureDidNotChangeCombatState(game);
    }

    @Test
    void attackWithoutNameOrIndexUsesFirstAttack() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = pokemonWithTwoAttacks("xy1-1", "Pikachu", 60);
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, null, null));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(10);
        assertThat(response.logs()).anySatisfy(log -> assertThat(log.message()).contains("Quick Hit"));
    }

    @Test
    void attackIndexSelectsCorrectAttack() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = pokemonWithTwoAttacks("xy1-1", "Pikachu", 60);
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, null, 1));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(40);
        assertThat(response.logs()).anySatisfy(log -> assertThat(log.message()).contains("Heavy Hit"));
    }

    @Test
    void attackNameSelectsCorrectAttack() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = pokemonWithTwoAttacks("xy1-1", "Pikachu", 60);
        CardEntity defender = pokemonWithAttackCost("xy1-2", "Bulbasaur", 70, "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Heavy Hit"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(40);
        assertThat(response.logs()).anySatisfy(log -> assertThat(log.message()).contains("Heavy Hit"));
    }

    @Test
    void attackWithoutWeaknessOrResistanceAppliesBaseDamage() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "20");
        CardEntity defender = typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(20);
    }

    @Test
    void attackAppliesTimesTwoWeakness() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "20");
        CardEntity defender = withWeakness(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Lightning", "×2");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(40);
    }

    @Test
    void attackAppliesPlusTwentyWeakness() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "20");
        CardEntity defender = withWeakness(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Lightning", "+20");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(40);
    }

    @Test
    void attackAppliesResistance() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "40");
        CardEntity defender = withResistance(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Lightning", "-20");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(20);
    }

    @Test
    void resistanceDoesNotAllowNegativeDamage() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "10");
        CardEntity defender = withResistance(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Lightning", "-20");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isZero();
    }

    @Test
    void weaknessDoesNotApplyWhenAttackerTypeDoesNotMatch() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "20");
        CardEntity defender = withWeakness(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Fire", "×2");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(20);
    }

    @Test
    void resistanceDoesNotApplyWhenAttackerTypeDoesNotMatch() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "40");
        CardEntity defender = withResistance(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Fire", "-20");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(40);
    }

    @Test
    void attackAppliesWeaknessBeforeResistance() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "20");
        CardEntity defender = withResistance(
                withWeakness(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Lightning", "×2"),
                "Lightning",
                "-10"
        );
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(30);
    }

    @Test
    void attackParsesPlusDamageAsBaseNumber() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "30+");
        CardEntity defender = typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(30);
    }

    @Test
    void attackParsesMultiplyDamageAsBaseNumber() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "10×");
        CardEntity defender = typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(10);
    }

    @Test
    void attackWithEmptyDamageDealsZeroDamageAndDoesNotApplyWeakness() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "");
        CardEntity defender = withWeakness(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Lightning", "+20");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isZero();
    }

    @Test
    void attackWithNullDamageDealsZeroDamage() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), null);
        CardEntity defender = typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon().damage()).isZero();
    }

    @Test
    void finalDamageCanCauseKnockOut() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "20");
        CardEntity defender = withWeakness(typedPokemonWithAttack("xy1-2", "Bulbasaur", 30, "Grass", "Growl", List.of(), "0"), "Lightning", "×2");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon()).isNull();
        assertThat(response.players().get(1).discardCardIds()).contains("xy1-2");
        assertThat(response.logs()).anySatisfy(log -> assertThat(log.actionType()).isEqualTo("KNOCK_OUT"));
    }

    @Test
    void resistanceCanPreventKnockOut() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "40");
        CardEntity defender = withResistance(typedPokemonWithAttack("xy1-2", "Bulbasaur", 30, "Grass", "Growl", List.of(), "0"), "Lightning", "-20");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.players().get(1).activePokemon()).isNotNull();
        assertThat(response.players().get(1).activePokemon().damage()).isEqualTo(20);
        assertThat(response.logs()).noneSatisfy(log -> assertThat(log.actionType()).isEqualTo("KNOCK_OUT"));
    }

    @Test
    void attackLogIncludesBaseAndFinalDamage() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of(), "20");
        CardEntity defender = withWeakness(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Lightning", "×2");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Spark"));

        assertThat(response.logs()).anySatisfy(log -> {
            assertThat(log.actionType()).isEqualTo("ATTACK");
            assertThat(log.message()).contains("Dano base: 20");
            assertThat(log.message()).contains("Debilidad: x2");
            assertThat(log.message()).contains("Dano final: 40");
        });
    }

    @Test
    void energyValidationStillRunsBeforeWeaknessAndResistance() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        CardEntity attacker = typedPokemonWithAttack("xy1-1", "Pikachu", 60, "Lightning", "Spark", List.of("Lightning"), "20");
        CardEntity defender = withWeakness(typedPokemonWithAttack("xy1-2", "Bulbasaur", 70, "Grass", "Growl", List.of(), "0"), "Lightning", "×2");
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Spark")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No hay suficiente energia");
        assertEnergyFailureDidNotChangeCombatState(game);
    }

    @Test
    void attackFailsWhenGameDoesNotExist() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.attack(99L, new AttackRequest(100L, "Gnaw")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void attackFailsWhenGameIsNotActive() {
        GameEntity game = activeGameReadyForAttack();
        game.setStatus(GameStatus.WAITING);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Gnaw")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attackFailsWhenPlayerIsNotCurrentPlayer() {
        GameEntity game = activeGameReadyForAttack();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(200L, "Gnaw")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attackFailsWhenPhaseIsNotMain() {
        GameEntity game = activeGameReadyForAttack();
        game.setTurnPhase(TurnPhase.DRAW);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Gnaw")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attackFailsWhenPlayerHasNoActivePokemon() {
        GameEntity game = activeGameReadyForAttack();
        game.getPlayers().getFirst().setActivePokemonCardId(null);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Gnaw")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Pokemon activo")
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attackFailsWhenOpponentHasNoActivePokemon() {
        GameEntity game = activeGameReadyForAttack();
        game.getPlayers().get(1).setActivePokemonCardId(null);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Gnaw")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("rival")
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attackFailsWhenAttackDoesNotExist() {
        GameEntity game = activeGameReadyForAttack();
        CardEntity attacker = pokemonWithAttack("xy1-1", "Pikachu", 60, "Gnaw", 1, "30");
        CardEntity defender = card("xy1-2", "Bulbasaur", "Pokémon", List.of("Basic"));
        defender.setHp(70);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("xy1-132")));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Thunderbolt")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("El Pokemon activo no tiene ese ataque")
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void attackFailsWhenEnergyIsInsufficient() {
        GameEntity game = activeGameReadyForAttack();
        game.getPlayers().getFirst().getAttachedEnergyCardIdsByPokemonCardId().clear();
        CardEntity attacker = pokemonWithAttack("xy1-1", "Pikachu", 60, "Gnaw", 1, "30");
        CardEntity defender = card("xy1-2", "Bulbasaur", "Pokémon", List.of("Basic"));
        defender.setHp(70);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("xy1-132")));

        assertThatThrownBy(() -> gameService.attack(1L, new AttackRequest(100L, "Gnaw")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No hay suficiente energia")
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attackKnocksOutOpponentActivePokemonAndTakesPrize() {
        GameEntity game = activeGameReadyForAttack();
        game.getPlayers().get(1).getAttachedEnergyCardIdsByPokemonCardId().put("xy1-2", new java.util.ArrayList<>(List.of("xy1-132")));
        CardEntity attacker = pokemonWithAttack("xy1-1", "Pikachu", 60, "Gnaw", 1, "60");
        CardEntity defender = card("xy1-2", "Bulbasaur", "Pokémon", List.of("Basic"));
        defender.setHp(50);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("xy1-132")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Gnaw"));

        assertThat(response.players().getFirst().prizeCardsRemaining()).isEqualTo(5);
        assertThat(response.players().get(1).activePokemonCardId()).isNull();
        assertThat(response.players().get(1).benchCardIds()).contains("xy1-4");
        assertThat(response.players().get(1).benchCardIds()).doesNotContain("xy1-2");
        assertThat(response.players().get(1).discardCardIds()).contains("xy1-2", "xy1-132");
        assertThat(response.players().get(1).damageByPokemonCardId()).doesNotContainKey("xy1-2");
        assertThat(response.players().get(1).attachedEnergyCardIdsByPokemonInstanceId().values())
                .allSatisfy(energyIds -> assertThat(energyIds).doesNotContain("xy1-132"));
        assertThat(response.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(response.winnerPlayerId()).isNull();
        assertThat(response.currentPlayerId()).isEqualTo(200L);
        assertThat(response.logs()).anySatisfy(log -> assertThat(log.actionType()).isEqualTo("KNOCK_OUT"));
        assertThat(response.logs()).anySatisfy(log -> assertThat(log.actionType()).isEqualTo("TAKE_PRIZE"));
    }

    @Test
    void attackTakesTwoPrizesWhenKnockedOutPokemonIsEx() {
        GameEntity game = activeGameReadyForAttack();
        CardEntity attacker = pokemonWithAttack("xy1-1", "Pikachu", 60, "Gnaw", 1, "60");
        CardEntity defender = card("xy1-2", "Bulbasaur-EX", "Pokémon", List.of("Basic", "EX"));
        defender.setHp(50);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("xy1-132")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Gnaw"));

        assertThat(response.players().getFirst().prizeCardsRemaining()).isEqualTo(4);
        assertThat(response.players().getFirst().handCardIds()).contains("p1", "p2");
        assertThat(response.logs()).anySatisfy(log -> {
            assertThat(log.actionType()).isEqualTo("TAKE_PRIZE");
            assertThat(log.message()).contains("2 premios");
        });
    }

    @Test
    void attackFinishesGameWhenAttackerHasNoPrizesAfterKo() {
        GameEntity game = activeGameReadyForAttack();
        game.getPlayers().getFirst().setPrizeCardIds(new java.util.ArrayList<>(List.of("last-prize")));
        CardEntity attacker = pokemonWithAttack("xy1-1", "Pikachu", 60, "Gnaw", 1, "60");
        CardEntity defender = card("xy1-2", "Bulbasaur", "Pokémon", List.of("Basic"));
        defender.setHp(50);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("xy1-132")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Gnaw"));

        assertThat(response.status()).isEqualTo(GameStatus.FINISHED);
        assertThat(response.winnerPlayerId()).isEqualTo(100L);
        assertThat(response.finishedAt()).isNotNull();
        assertThat(response.turnPhase()).isNull();
        assertThat(response.players().getFirst().prizeCardsRemaining()).isZero();
        assertThat(response.players().getFirst().handCardIds()).contains("last-prize");
        assertThat(response.logs()).anySatisfy(log -> assertThat(log.actionType()).isEqualTo("VICTORY_PRIZES"));
    }

    @Test
    void attackFinishesGameWhenDefenderHasNoPokemonAfterKo() {
        GameEntity game = activeGameReadyForAttack();
        game.getPlayers().get(1).setBenchCardIds(new java.util.ArrayList<>());
        CardEntity attacker = pokemonWithAttack("xy1-1", "Pikachu", 60, "Gnaw", 1, "60");
        CardEntity defender = card("xy1-2", "Bulbasaur", "Pokémon", List.of("Basic"));
        defender.setHp(50);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("xy1-132")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.attack(1L, new AttackRequest(100L, "Gnaw"));

        assertThat(response.status()).isEqualTo(GameStatus.FINISHED);
        assertThat(response.winnerPlayerId()).isEqualTo(100L);
        assertThat(response.players().get(1).activePokemon()).isNull();
        assertThat(response.players().get(1).benchPokemon()).isEmpty();
        assertThat(response.logs()).anySatisfy(log -> assertThat(log.actionType()).isEqualTo("VICTORY_NO_POKEMON"));
    }

    @Test
    void promoteActiveWorksAfterActivePokemonWasKnockedOut() {
        GameEntity game = activeGameReadyForAttack();
        CardEntity attacker = pokemonWithAttack("xy1-1", "Pikachu", 60, "Gnaw", 1, "60");
        CardEntity defender = card("xy1-2", "Bulbasaur", "Pokémon", List.of("Basic"));
        defender.setHp(50);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(cardRepository.findAllById(any())).thenReturn(List.of(attacker, defender, grassEnergy("xy1-132")));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        gameService.attack(1L, new AttackRequest(100L, "Gnaw"));
        String benchInstanceId = game.getPlayers().get(1).getPokemonInPlay().stream()
                .filter(pokemon -> pokemon.getZone() == PokemonZone.BENCH)
                .findFirst()
                .orElseThrow()
                .getInstanceId();

        GameResponse response = gameService.promoteActive(1L, new PromoteActiveRequest(200L, benchInstanceId));

        assertThat(response.status()).isEqualTo(GameStatus.ACTIVE);
        assertThat(response.players().get(1).activePokemonInstanceId()).isEqualTo(benchInstanceId);
        assertThat(response.players().get(1).activePokemon().cardId()).isEqualTo("xy1-4");
        assertThat(response.logs()).anySatisfy(log -> assertThat(log.actionType()).isEqualTo("PROMOTE_ACTIVE"));
    }

    @Test
    void promoteActiveMovesBenchPokemonToActiveWhenPlayerHasNoActivePokemon() {
        GameEntity game = activeGame();
        game.getPlayers().getFirst().getPokemonInPlay().add(pokemonInPlay("bench-copy", "xy1-1", PokemonZone.BENCH));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(gameRepository.save(any(GameEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GameResponse response = gameService.promoteActive(1L, new PromoteActiveRequest(100L, "bench-copy"));

        assertThat(response.players().getFirst().activePokemonInstanceId()).isEqualTo("bench-copy");
        assertThat(response.players().getFirst().activePokemon().cardId()).isEqualTo("xy1-1");
        assertThat(response.players().getFirst().benchPokemon()).isEmpty();
        assertThat(response.logs().get(1).actionType()).isEqualTo("PROMOTE_ACTIVE");
    }

    @Test
    void promoteActiveFailsWhenPlayerAlreadyHasActivePokemon() {
        GameEntity game = activeGame();
        game.getPlayers().getFirst().getPokemonInPlay().add(pokemonInPlay("active-copy", "xy1-1", PokemonZone.ACTIVE));
        game.getPlayers().getFirst().setActivePokemonInstanceId("active-copy");
        game.getPlayers().getFirst().getPokemonInPlay().add(pokemonInPlay("bench-copy", "xy1-2", PokemonZone.BENCH));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.promoteActive(1L, new PromoteActiveRequest(100L, "bench-copy")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void promoteActiveFailsWhenInstanceDoesNotBelongToPlayer() {
        GameEntity game = activeGame();
        game.getPlayers().get(1).getPokemonInPlay().add(pokemonInPlay("other-player-copy", "xy1-2", PokemonZone.BENCH));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.promoteActive(1L, new PromoteActiveRequest(100L, "other-player-copy")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attackFailsWhenGameIsFinished() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(finishedGame()));

        assertFinishedAction(() -> gameService.attack(1L, new AttackRequest(100L, "Gnaw")));
    }

    @Test
    void drawCardFailsWhenGameIsFinished() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(finishedGame()));

        assertFinishedAction(() -> gameService.drawCard(1L, new GameActionRequest(100L)));
    }

    @Test
    void attachEnergyFailsWhenGameIsFinished() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(finishedGame()));

        assertFinishedAction(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-1")));
    }

    @Test
    void playBasicPokemonFailsWhenGameIsFinished() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(finishedGame()));

        assertFinishedAction(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1")));
    }

    @Test
    void endTurnFailsWhenGameIsFinished() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(finishedGame()));

        assertFinishedAction(() -> gameService.endTurn(1L, new GameActionRequest(100L)));
    }

    @Test
    void promoteActiveFailsWhenGameIsFinished() {
        when(gameRepository.findById(1L)).thenReturn(Optional.of(finishedGame()));

        assertFinishedAction(() -> gameService.promoteActive(1L, new PromoteActiveRequest(100L, "bench-copy")));
    }

    private DeckEntity deck(Long id, String name) {
        DeckEntity deck = new DeckEntity();
        deck.setId(id);
        deck.setName(name);
        return deck;
    }

    private DeckEntity validDeck(Long id, String name) {
        DeckEntity deck = deck(id, name);
        deck.addCard(deckCard(card("pokemon-" + id, "Pikachu " + id, "Pokémon", List.of("Basic")), 4));
        deck.addCard(deckCard(card("energy-" + id, "Lightning Energy", "Energy", List.of("Basic")), 56));
        return deck;
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

    private CardEntity pokemonWithAttack(String id, String name, int hp, String attackName, int energyCost, String damage) {
        CardEntity card = card(id, name, "Pokémon", List.of("Basic"));
        card.setHp(hp);
        card.setAttacks(objectMapper.valueToTree(List.of(Map.of(
                "name", attackName,
                "cost", java.util.Collections.nCopies(energyCost, "Colorless"),
                "convertedEnergyCost", energyCost,
                "damage", damage
        ))));
        return card;
    }

    private CardEntity pokemonWithAttackCost(String id, String name, int hp, String attackName, List<String> cost, String damage) {
        CardEntity card = card(id, name, "Pokémon", List.of("Basic"));
        card.setHp(hp);
        card.setAttacks(objectMapper.valueToTree(List.of(attack(attackName, cost, damage))));
        return card;
    }

    private CardEntity typedPokemonWithAttack(String id, String name, int hp, String type, String attackName, List<String> cost, String damage) {
        CardEntity card = pokemonWithAttackCost(id, name, hp, attackName, cost, damage);
        card.setTypes(List.of(type));
        return card;
    }

    private CardEntity withWeakness(CardEntity card, String type, String value) {
        card.setWeaknesses(objectMapper.valueToTree(List.of(Map.of("type", type, "value", value))));
        return card;
    }

    private CardEntity withResistance(CardEntity card, String type, String value) {
        card.setResistances(objectMapper.valueToTree(List.of(Map.of("type", type, "value", value))));
        return card;
    }

    private CardEntity pokemonWithTwoAttacks(String id, String name, int hp) {
        CardEntity card = card(id, name, "Pokémon", List.of("Basic"));
        card.setHp(hp);
        card.setAttacks(objectMapper.valueToTree(List.of(
                attack("Quick Hit", List.of(), "10"),
                attack("Heavy Hit", List.of(), "40")
        )));
        return card;
    }

    private Map<String, Object> attack(String name, List<String> cost, String damage) {
        Map<String, Object> attack = new java.util.LinkedHashMap<>();
        attack.put("name", name);
        attack.put("cost", cost);
        attack.put("convertedEnergyCost", cost.size());
        attack.put("damage", damage);
        return attack;
    }

    private CardEntity grassEnergy(String id) {
        return energy(id, "Grass Energy", "Grass");
    }

    private CardEntity energy(String id, String name, String type) {
        CardEntity card = card(id, name, "Energy", List.of("Basic"));
        card.setTypes(List.of(type));
        return card;
    }

    private GameEntity waitingGameWithPlayer(Long id, DeckEntity deck) {
        GameEntity game = new GameEntity();
        game.setId(id);
        game.setStatus(GameStatus.WAITING);
        game.addPlayer(player(100L, "Ash", 1, deck));
        GameLogEntity log = new GameLogEntity();
        log.setId(1000L);
        log.setActionType("CREATE_GAME");
        log.setMessage("Partida creada por Ash.");
        game.addLog(log);
        return game;
    }

    private GameEntity activeGame() {
        GameEntity game = new GameEntity();
        game.setId(1L);
        game.setStatus(GameStatus.ACTIVE);
        game.setTurnPhase(TurnPhase.DRAW);
        game.setCurrentPlayerId(100L);
        GamePlayerEntity firstPlayer = player(100L, "Ash", 1, deck(10L, "Mazo 1"));
        firstPlayer.setHandCardIds(new java.util.ArrayList<>(List.of("h1", "h2", "h3", "h4", "h5", "h6", "h7")));
        firstPlayer.setPrizeCardIds(new java.util.ArrayList<>(List.of("p1", "p2", "p3", "p4", "p5", "p6")));
        firstPlayer.setDeckCardIds(new java.util.ArrayList<>(numberedCards("d", 47)));
        firstPlayer.setBenchCardIds(new java.util.ArrayList<>());
        firstPlayer.setDiscardCardIds(new java.util.ArrayList<>());
        GamePlayerEntity secondPlayer = player(200L, "Misty", 2, deck(20L, "Mazo 2"));
        secondPlayer.setHandCardIds(new java.util.ArrayList<>(List.of("mh1", "mh2", "mh3", "mh4", "mh5", "mh6", "mh7")));
        secondPlayer.setPrizeCardIds(new java.util.ArrayList<>(List.of("mp1", "mp2", "mp3", "mp4", "mp5", "mp6")));
        secondPlayer.setDeckCardIds(new java.util.ArrayList<>(numberedCards("md", 47)));
        secondPlayer.setBenchCardIds(new java.util.ArrayList<>());
        secondPlayer.setDiscardCardIds(new java.util.ArrayList<>());
        game.addPlayer(firstPlayer);
        game.addPlayer(secondPlayer);
        GameLogEntity log = new GameLogEntity();
        log.setId(1000L);
        log.setActionType("START_GAME");
        log.setMessage("Partida iniciada.");
        game.addLog(log);
        return game;
    }

    private GameEntity activeGameInMainPhaseWithBasicPokemonInHand() {
        GameEntity game = activeGame();
        game.setTurnPhase(TurnPhase.MAIN);
        game.getPlayers().getFirst().getHandCardIds().add("xy1-1");
        return game;
    }

    private GameEntity activeGameWithActiveAndEnergyInHand() {
        GameEntity game = activeGame();
        game.setTurnPhase(TurnPhase.MAIN);
        game.getPlayers().getFirst().getHandCardIds().add("xy1-132");
        game.getPlayers().getFirst().setActivePokemonCardId("xy1-1");
        return game;
    }

    private GameEntity activeGameWithBenchAndEnergyInHand() {
        GameEntity game = activeGameWithActiveAndEnergyInHand();
        game.getPlayers().getFirst().getBenchCardIds().add("xy1-3");
        return game;
    }

    private GameEntity activeGameWithRepeatedPokemonInstancesAndEnergyInHand() {
        GameEntity game = activeGame();
        game.setTurnPhase(TurnPhase.MAIN);
        GamePlayerEntity player = game.getPlayers().getFirst();
        player.getHandCardIds().add("xy1-132");
        player.getPokemonInPlay().add(pokemonInPlay("active-copy", "xy1-1", PokemonZone.ACTIVE));
        player.getPokemonInPlay().add(pokemonInPlay("bench-copy", "xy1-1", PokemonZone.BENCH));
        player.setActivePokemonInstanceId("active-copy");
        return game;
    }

    private GameEntity activeGameReadyForAttack() {
        GameEntity game = activeGame();
        game.setTurnPhase(TurnPhase.MAIN);
        game.getPlayers().getFirst().setActivePokemonCardId("xy1-1");
        game.getPlayers().getFirst().getBenchCardIds().add("xy1-3");
        game.getPlayers().getFirst().getAttachedEnergyCardIdsByPokemonCardId()
                .put("xy1-1", new java.util.ArrayList<>(List.of("xy1-132")));
        game.getPlayers().get(1).setActivePokemonCardId("xy1-2");
        game.getPlayers().get(1).getBenchCardIds().add("xy1-4");
        return game;
    }

    private GameEntity activeGameWithRepeatedDefenderInstancesReadyForAttack() {
        GameEntity game = activeGame();
        game.setTurnPhase(TurnPhase.MAIN);
        GamePlayerEntity attacker = game.getPlayers().getFirst();
        PokemonInPlay attackerActive = pokemonInPlay("attacker-active", "xy1-1", PokemonZone.ACTIVE);
        attackerActive.getAttachedEnergyCardIds().add("xy1-132");
        attacker.getPokemonInPlay().add(attackerActive);
        attacker.setActivePokemonInstanceId("attacker-active");

        GamePlayerEntity defender = game.getPlayers().get(1);
        defender.getPokemonInPlay().add(pokemonInPlay("defender-active", "xy1-2", PokemonZone.ACTIVE));
        defender.getPokemonInPlay().add(pokemonInPlay("defender-bench", "xy1-2", PokemonZone.BENCH));
        defender.setActivePokemonInstanceId("defender-active");
        return game;
    }

    private GameEntity activeGameReadyForAttackWithAttachedEnergies(List<String> energyCardIds) {
        GameEntity game = activeGame();
        game.setTurnPhase(TurnPhase.MAIN);

        GamePlayerEntity attacker = game.getPlayers().getFirst();
        PokemonInPlay attackerActive = pokemonInPlay("attacker-active", "xy1-1", PokemonZone.ACTIVE);
        attackerActive.getAttachedEnergyCardIds().addAll(energyCardIds);
        attacker.getPokemonInPlay().add(attackerActive);
        attacker.setActivePokemonInstanceId("attacker-active");

        GamePlayerEntity defender = game.getPlayers().get(1);
        defender.getPokemonInPlay().add(pokemonInPlay("defender-active", "xy1-2", PokemonZone.ACTIVE));
        defender.setActivePokemonInstanceId("defender-active");
        return game;
    }

    private GameEntity activeGameWithRepeatedAttackerInstancesAndBenchEnergy() {
        GameEntity game = activeGameReadyForAttackWithAttachedEnergies(List.of());
        PokemonInPlay benchCopy = pokemonInPlay("attacker-bench", "xy1-1", PokemonZone.BENCH);
        benchCopy.getAttachedEnergyCardIds().add("grass-1");
        game.getPlayers().getFirst().getPokemonInPlay().add(benchCopy);
        return game;
    }

    private void assertEnergyFailureDidNotChangeCombatState(GameEntity game) {
        GamePlayerEntity attacker = game.getPlayers().getFirst();
        GamePlayerEntity defender = game.getPlayers().get(1);
        PokemonInPlay defenderActive = defender.getPokemonInPlay().stream()
                .filter(pokemon -> pokemon.getZone() == PokemonZone.ACTIVE)
                .findFirst()
                .orElseThrow();
        assertThat(defenderActive.getDamage()).isZero();
        assertThat(defender.getActivePokemonInstanceId()).isNotBlank();
        assertThat(defender.getDiscardCardIds()).isEmpty();
        assertThat(attacker.getPrizeCardIds()).hasSize(6);
        assertThat(game.getCurrentPlayerId()).isEqualTo(100L);
        assertThat(game.getTurnPhase()).isEqualTo(TurnPhase.MAIN);
        assertThat(game.getLogs()).extracting(GameLogEntity::getActionType)
                .doesNotContain("ATTACK", "KNOCK_OUT", "TAKE_PRIZE");
    }

    private GameEntity finishedGame() {
        GameEntity game = activeGame();
        game.setStatus(GameStatus.FINISHED);
        game.setWinnerPlayerId(100L);
        game.setTurnPhase(null);
        game.getPlayers().getFirst().getPokemonInPlay().add(pokemonInPlay("bench-copy", "xy1-1", PokemonZone.BENCH));
        return game;
    }

    private void assertFinishedAction(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("La partida ya finalizó.")
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    private PokemonInPlay pokemonInPlay(String instanceId, String cardId, PokemonZone zone) {
        return new PokemonInPlay(instanceId, cardId, zone);
    }

    private List<String> numberedCards(String prefix, int quantity) {
        java.util.ArrayList<String> cards = new java.util.ArrayList<>();
        for (int index = 1; index <= quantity; index++) {
            cards.add(prefix + index);
        }
        return cards;
    }

    private GamePlayerEntity player(Long id, String playerName, int playerOrder, DeckEntity deck) {
        GamePlayerEntity player = new GamePlayerEntity();
        player.setId(id);
        player.setPlayerName(playerName);
        player.setPlayerOrder(playerOrder);
        player.setDeck(deck);
        return player;
    }
}
