package com.pokemontcg.game.service;

import com.pokemontcg.card.persistence.CardEntity;
import com.pokemontcg.card.persistence.CardRepository;
import com.pokemontcg.deck.dto.DeckValidationResponse;
import com.pokemontcg.deck.persistence.DeckCardEntity;
import com.pokemontcg.deck.persistence.DeckEntity;
import com.pokemontcg.deck.persistence.DeckRepository;
import com.pokemontcg.deck.service.DeckValidator;
import com.pokemontcg.game.dto.AttachEnergyRequest;
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
import com.pokemontcg.game.persistence.TurnPhase;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

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
    void drawCardFailsWhenDeckIsEmpty() {
        GameEntity game = activeGame();
        game.getPlayers().getFirst().setDeckCardIds(List.of());
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.drawCard(1L, new GameActionRequest(100L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
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
    void playBasicPokemonMovesCardFromHandToBench() {
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
        assertThat(response.players().getFirst().benchSize()).isEqualTo(1);
        assertThat(response.players().getFirst().benchCardIds()).containsExactly("xy1-1");
        assertThat(response.players().getFirst().handCardIds()).doesNotContain("xy1-1");
        assertThat(response.logs()).hasSize(2);
        assertThat(response.logs().get(1).actionType()).isEqualTo("PLAY_BASIC_POKEMON");
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
        game.getPlayers().getFirst().setBenchCardIds(new java.util.ArrayList<>(List.of("b1", "b2", "b3", "b4", "b5")));
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.playBasicPokemon(1L, new PlayBasicPokemonRequest(100L, "xy1-1")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void attachEnergyMovesBasicEnergyFromHandToTargetPokemon() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
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
    void attachEnergyFailsWhenTargetPokemonIsNotOnBench() {
        GameEntity game = activeGameWithBenchAndEnergyInHand();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        assertThatThrownBy(() -> gameService.attachEnergy(1L, new AttachEnergyRequest(100L, "xy1-132", "xy1-99")))
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

    private GameEntity activeGameWithBenchAndEnergyInHand() {
        GameEntity game = activeGame();
        game.setTurnPhase(TurnPhase.MAIN);
        game.getPlayers().getFirst().getHandCardIds().add("xy1-132");
        game.getPlayers().getFirst().getBenchCardIds().add("xy1-1");
        return game;
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
