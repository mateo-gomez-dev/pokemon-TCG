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
import com.pokemontcg.game.dto.GameLogResponse;
import com.pokemontcg.game.dto.GamePlayerResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class GameService {

    private final GameRepository gameRepository;
    private final DeckRepository deckRepository;
    private final DeckValidator deckValidator;
    private final CardRepository cardRepository;

    public GameService(GameRepository gameRepository, DeckRepository deckRepository, DeckValidator deckValidator, CardRepository cardRepository) {
        this.gameRepository = gameRepository;
        this.deckRepository = deckRepository;
        this.deckValidator = deckValidator;
        this.cardRepository = cardRepository;
    }

    @Transactional
    public GameResponse createGame(CreateGameRequest request) {
        DeckEntity deck = deckRepository.findById(request.deckId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mazo no encontrado"));

        GameEntity game = new GameEntity();
        game.setStatus(GameStatus.WAITING);

        GamePlayerEntity player = new GamePlayerEntity();
        player.setPlayerName(request.playerName());
        player.setDeck(deck);
        player.setPlayerOrder(1);
        game.addPlayer(player);

        GameLogEntity log = new GameLogEntity();
        log.setPlayerId(null);
        log.setActionType("CREATE_GAME");
        log.setMessage("Partida creada por " + request.playerName() + ".");
        game.addLog(log);

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse joinGame(Long gameId, JoinGameRequest request) {
        GameEntity game = findGame(gameId);
        if (game.getStatus() != GameStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede unir a una partida en estado WAITING");
        }
        if (game.getPlayers().size() >= 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partida ya tiene 2 jugadores");
        }

        DeckEntity deck = deckRepository.findById(request.deckId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mazo no encontrado"));

        GamePlayerEntity player = new GamePlayerEntity();
        player.setPlayerName(request.playerName());
        player.setDeck(deck);
        player.setPlayerOrder(2);
        game.addPlayer(player);

        GameLogEntity log = new GameLogEntity();
        log.setPlayerId(null);
        log.setActionType("JOIN_GAME");
        log.setMessage("Jugador " + request.playerName() + " se unio a la partida.");
        game.addLog(log);

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse startGame(Long gameId) {
        GameEntity game = findGame(gameId);
        if (game.getStatus() != GameStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede iniciar una partida en estado WAITING");
        }
        if (game.getPlayers().size() != 2) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partida debe tener exactamente 2 jugadores");
        }

        List<GamePlayerEntity> players = orderedPlayers(game);
        for (GamePlayerEntity player : players) {
            validateDeck(player);
            setupPlayer(player);
        }

        game.setStatus(GameStatus.ACTIVE);
        game.setCurrentPlayerId(players.getFirst().getId());
        game.setTurnPhase(TurnPhase.DRAW);
        game.setStartedAt(LocalDateTime.now());

        GameLogEntity log = new GameLogEntity();
        log.setPlayerId(null);
        log.setActionType("START_GAME");
        log.setMessage("Partida iniciada.");
        game.addLog(log);

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse drawCard(Long gameId, GameActionRequest request) {
        GameEntity game = findGame(gameId);
        assertActiveGame(game);
        assertCurrentPlayer(game, request.playerId());

        GamePlayerEntity player = findPlayer(game, request.playerId());
        if (player.getDeckCardIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El jugador no tiene cartas en el deck restante");
        }

        String drawnCardId = player.getDeckCardIds().removeFirst();
        player.getHandCardIds().add(drawnCardId);
        game.setTurnPhase(TurnPhase.MAIN);
        addLog(game, player.getId(), "DRAW_CARD", "Jugador " + player.getPlayerName() + " robo una carta.");

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse endTurn(Long gameId, GameActionRequest request) {
        GameEntity game = findGame(gameId);
        assertActiveGame(game);
        assertCurrentPlayer(game, request.playerId());

        GamePlayerEntity player = findPlayer(game, request.playerId());
        GamePlayerEntity nextPlayer = findOtherPlayer(game, request.playerId());
        game.setCurrentPlayerId(nextPlayer.getId());
        game.setTurnPhase(TurnPhase.DRAW);
        nextPlayer.setEnergyAttachedThisTurn(false);
        addLog(game, player.getId(), "END_TURN", "Jugador " + player.getPlayerName() + " finalizo su turno.");

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse playBasicPokemon(Long gameId, PlayBasicPokemonRequest request) {
        GameEntity game = findGame(gameId);
        assertActiveGame(game);
        assertCurrentPlayer(game, request.playerId());
        assertMainPhase(game);

        GamePlayerEntity player = findPlayer(game, request.playerId());
        if (!player.getHandCardIds().contains(request.cardId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La carta no esta en la mano del jugador");
        }
        if (player.getBenchCardIds().size() >= 5) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La banca ya tiene 5 Pokemon");
        }

        CardEntity card = cardRepository.findById(request.cardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carta no encontrada"));
        if (!"Pokémon".equalsIgnoreCase(card.getSupertype())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La carta no es un Pokemon");
        }
        if (!hasSubtype(card, "Basic")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La carta no es un Pokemon Basico");
        }

        player.getHandCardIds().remove(request.cardId());
        player.getBenchCardIds().add(request.cardId());
        addLog(game, player.getId(), "PLAY_BASIC_POKEMON", "Jugador " + player.getPlayerName() + " jugo " + card.getName() + " en la banca.");

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse attachEnergy(Long gameId, AttachEnergyRequest request) {
        GameEntity game = findGame(gameId);
        assertActiveGame(game);
        assertCurrentPlayer(game, request.playerId());
        assertMainPhase(game);

        GamePlayerEntity player = findPlayer(game, request.playerId());
        if (!player.getHandCardIds().contains(request.energyCardId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La energia no esta en la mano del jugador");
        }
        if (!player.getBenchCardIds().contains(request.targetPokemonCardId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El Pokemon objetivo no esta en la banca del jugador");
        }
        if (player.isEnergyAttachedThisTurn()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El jugador ya unio una energia este turno");
        }

        CardEntity energyCard = cardRepository.findById(request.energyCardId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Energia no encontrada"));
        if (!"Energy".equalsIgnoreCase(energyCard.getSupertype())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La carta no es una Energia");
        }
        if (!hasSubtype(energyCard, "Basic")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La energia no es Basica");
        }

        player.getHandCardIds().remove(request.energyCardId());
        player.getAttachedEnergyCardIdsByPokemonCardId()
                .computeIfAbsent(request.targetPokemonCardId(), ignored -> new ArrayList<>())
                .add(request.energyCardId());
        player.setEnergyAttachedThisTurn(true);
        addLog(game, player.getId(), "ATTACH_ENERGY", "Jugador " + player.getPlayerName() + " unio " + energyCard.getName() + " a " + request.targetPokemonCardId() + ".");

        return toResponse(gameRepository.save(game));
    }

    @Transactional(readOnly = true)
    public List<GameResponse> getGames() {
        return gameRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GameResponse getGame(Long id) {
        return toResponse(findGame(id));
    }

    private GameEntity findGame(Long id) {
        return gameRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partida no encontrada"));
    }

    private void assertActiveGame(GameEntity game) {
        if (game.getStatus() != GameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partida debe estar ACTIVE");
        }
    }

    private void assertCurrentPlayer(GameEntity game, Long playerId) {
        if (!playerId.equals(game.getCurrentPlayerId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo puede actuar el jugador actual");
        }
    }

    private void assertMainPhase(GameEntity game) {
        if (game.getTurnPhase() != TurnPhase.MAIN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La accion solo se puede realizar en fase MAIN");
        }
    }

    private GamePlayerEntity findPlayer(GameEntity game, Long playerId) {
        return game.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El jugador no pertenece a la partida"));
    }

    private GamePlayerEntity findOtherPlayer(GameEntity game, Long playerId) {
        return game.getPlayers().stream()
                .filter(player -> !player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No hay otro jugador en la partida"));
    }

    private void addLog(GameEntity game, Long playerId, String actionType, String message) {
        GameLogEntity log = new GameLogEntity();
        log.setPlayerId(playerId);
        log.setActionType(actionType);
        log.setMessage(message);
        game.addLog(log);
    }

    private boolean hasSubtype(CardEntity card, String subtype) {
        if (card.getSubtypes() == null) {
            return false;
        }
        String normalizedSubtype = normalize(subtype);
        return card.getSubtypes().stream()
                .map(this::normalize)
                .anyMatch(normalizedSubtype::equals);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private void validateDeck(GamePlayerEntity player) {
        DeckValidationResponse validation = deckValidator.validate(player.getDeck().getCards());
        if (!validation.valid()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El mazo de " + player.getPlayerName() + " no es valido: " + String.join(" ", validation.errors())
            );
        }
    }

    private void setupPlayer(GamePlayerEntity player) {
        List<String> deckCardIds = expandDeck(player.getDeck());
        Collections.shuffle(deckCardIds);
        player.setHandCardIds(new ArrayList<>(deckCardIds.subList(0, 7)));
        player.setPrizeCardIds(new ArrayList<>(deckCardIds.subList(7, 13)));
        player.setBenchCardIds(new ArrayList<>());
        player.getAttachedEnergyCardIdsByPokemonCardId().clear();
        player.setEnergyAttachedThisTurn(false);
        player.setDeckCardIds(new ArrayList<>(deckCardIds.subList(13, deckCardIds.size())));
        player.setDiscardCardIds(new ArrayList<>());
    }

    private List<String> expandDeck(DeckEntity deck) {
        List<String> cardIds = new ArrayList<>();
        for (DeckCardEntity deckCard : deck.getCards()) {
            for (int index = 0; index < deckCard.getQuantity(); index++) {
                cardIds.add(deckCard.getCard().getId());
            }
        }
        return cardIds;
    }

    private List<GamePlayerEntity> orderedPlayers(GameEntity game) {
        return game.getPlayers().stream()
                .sorted(Comparator.comparingInt(GamePlayerEntity::getPlayerOrder))
                .toList();
    }

    private GameResponse toResponse(GameEntity game) {
        return new GameResponse(
                game.getId(),
                game.getStatus(),
                game.getTurnPhase(),
                game.getCurrentPlayerId(),
                game.getCreatedAt(),
                game.getUpdatedAt(),
                game.getStartedAt(),
                game.getFinishedAt(),
                orderedPlayers(game).stream()
                        .map(this::toPlayerResponse)
                        .toList(),
                game.getLogs().stream()
                        .sorted(Comparator.comparing(GameLogEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toLogResponse)
                        .toList()
        );
    }

    private GamePlayerResponse toPlayerResponse(GamePlayerEntity player) {
        DeckEntity deck = player.getDeck();
        return new GamePlayerResponse(
                player.getId(),
                player.getPlayerName(),
                player.getPlayerOrder(),
                deck.getId(),
                deck.getName(),
                player.getDeckCardIds().size(),
                player.getHandCardIds().size(),
                player.getPrizeCardIds().size(),
                player.getBenchCardIds().size(),
                player.getDiscardCardIds().size(),
                player.isEnergyAttachedThisTurn(),
                player.getDeckCardIds(),
                player.getHandCardIds(),
                player.getPrizeCardIds(),
                player.getBenchCardIds(),
                player.getAttachedEnergyCardIdsByPokemonCardId(),
                player.getDiscardCardIds()
        );
    }

    private GameLogResponse toLogResponse(GameLogEntity log) {
        return new GameLogResponse(
                log.getId(),
                log.getPlayerId(),
                log.getActionType(),
                log.getMessage(),
                log.getCreatedAt()
        );
    }
}
