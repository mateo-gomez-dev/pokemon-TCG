package com.pokemontcg.game.service;

import com.pokemontcg.card.persistence.CardEntity;
import com.pokemontcg.card.persistence.CardRepository;
import com.pokemontcg.deck.dto.DeckValidationResponse;
import com.pokemontcg.deck.persistence.DeckEntity;
import com.pokemontcg.deck.persistence.DeckRepository;
import com.pokemontcg.deck.service.DeckValidator;
import com.pokemontcg.game.dto.AttachEnergyRequest;
import com.pokemontcg.game.dto.AttackRequest;
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
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GameService {

    private static final Pattern DAMAGE_NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final int FIRST_PLAYER_ORDER = 1;
    private static final int SECOND_PLAYER_ORDER = 2;
    private static final int REQUIRED_PLAYER_COUNT = 2;
    private static final int MAX_BENCH_SIZE = 5;
    private static final int INITIAL_HAND_SIZE = 7;
    private static final int PRIZE_CARD_COUNT = 6;
    private static final int PRIZE_CARDS_START_INDEX = INITIAL_HAND_SIZE;
    private static final int REMAINING_DECK_START_INDEX = INITIAL_HAND_SIZE + PRIZE_CARD_COUNT;
    private static final String POKEMON_SUPERTYPE = "Pokémon";
    private static final String ENERGY_SUPERTYPE = "Energy";
    private static final String BASIC_SUBTYPE = "Basic";

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
        DeckEntity deck = findDeckOrBadRequest(request.deckId());

        GameEntity game = new GameEntity();
        game.setStatus(GameStatus.WAITING);
        game.addPlayer(createPlayer(request.playerName(), deck, FIRST_PLAYER_ORDER));

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
        assertWaitingGame(game, "Solo se puede unir a una partida en estado WAITING");
        if (game.getPlayers().size() >= REQUIRED_PLAYER_COUNT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partida ya tiene " + REQUIRED_PLAYER_COUNT + " jugadores");
        }

        DeckEntity deck = findDeckOrBadRequest(request.deckId());
        game.addPlayer(createPlayer(request.playerName(), deck, SECOND_PLAYER_ORDER));

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
        assertWaitingGame(game, "Solo se puede iniciar una partida en estado WAITING");
        if (game.getPlayers().size() != REQUIRED_PLAYER_COUNT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La partida debe tener exactamente " + REQUIRED_PLAYER_COUNT + " jugadores");
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
        GameEntity game = findActiveGameForCurrentPlayer(gameId, request.playerId());

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
        GameEntity game = findActiveGameForCurrentPlayer(gameId, request.playerId());

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
        GameEntity game = findActiveGameForCurrentPlayer(gameId, request.playerId());
        assertMainPhase(game);

        GamePlayerEntity player = findPlayer(game, request.playerId());
        assertCardInList(player.getHandCardIds(), request.cardId(), "La carta no esta en la mano del jugador");
        if (player.getBenchCardIds().size() >= MAX_BENCH_SIZE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La banca ya tiene " + MAX_BENCH_SIZE + " Pokemon");
        }

        CardEntity card = findCardOrBadRequest(request.cardId(), "Carta no encontrada");
        assertBasicPokemon(card);

        player.getHandCardIds().remove(request.cardId());
        player.getBenchCardIds().add(request.cardId());
        addLog(game, player.getId(), "PLAY_BASIC_POKEMON", "Jugador " + player.getPlayerName() + " jugo " + card.getName() + " en la banca.");

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse attachEnergy(Long gameId, AttachEnergyRequest request) {
        GameEntity game = findActiveGameForCurrentPlayer(gameId, request.playerId());
        assertMainPhase(game);

        GamePlayerEntity player = findPlayer(game, request.playerId());
        assertCardInList(player.getHandCardIds(), request.energyCardId(), "La energia no esta en la mano del jugador");
        assertCardInList(player.getBenchCardIds(), request.targetPokemonCardId(), "El Pokemon objetivo no esta en la banca del jugador");
        if (player.isEnergyAttachedThisTurn()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El jugador ya unio una energia este turno");
        }

        CardEntity energyCard = findCardOrBadRequest(request.energyCardId(), "Energia no encontrada");
        assertBasicEnergy(energyCard);

        player.getHandCardIds().remove(request.energyCardId());
        player.getAttachedEnergyCardIdsByPokemonCardId()
                .computeIfAbsent(request.targetPokemonCardId(), ignored -> new ArrayList<>())
                .add(request.energyCardId());
        player.setEnergyAttachedThisTurn(true);
        addLog(game, player.getId(), "ATTACH_ENERGY", "Jugador " + player.getPlayerName() + " unio " + energyCard.getName() + " a " + request.targetPokemonCardId() + ".");

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse attack(Long gameId, AttackRequest request) {
        GameEntity game = findActiveGameForCurrentPlayer(gameId, request.playerId());
        assertMainPhase(game);

        GamePlayerEntity attacker = findPlayer(game, request.playerId());
        GamePlayerEntity defender = findOtherPlayer(game, request.playerId());
        String attackerActiveCardId = activePokemonCardId(attacker, "El jugador actual no tiene Pokemon activo");
        String defenderActiveCardId = activePokemonCardId(defender, "El rival no tiene Pokemon activo");

        Map<String, CardEntity> cardsById = findCardsById(List.of(attackerActiveCardId, defenderActiveCardId));
        CardEntity attackerCard = findCard(cardsById, attackerActiveCardId, "Pokemon atacante no encontrado");
        CardEntity defenderCard = findCard(cardsById, defenderActiveCardId, "Pokemon defensor no encontrado");
        JsonNode attackNode = findAttack(attackerCard, request.attackName());
        int requiredEnergy = requiredEnergy(attackNode);
        int attachedEnergy = attacker.getAttachedEnergyCardIdsByPokemonCardId()
                .getOrDefault(attackerActiveCardId, List.of())
                .size();
        if (attachedEnergy < requiredEnergy) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Energia insuficiente para atacar");
        }

        int damage = attackDamage(attackNode);
        int accumulatedDamage = defender.getDamageByPokemonCardId().getOrDefault(defenderActiveCardId, 0) + damage;
        defender.getDamageByPokemonCardId().put(defenderActiveCardId, accumulatedDamage);

        String message = attacker.getPlayerName() + " ataco con " + attackerCard.getName()
                + " usando " + attackNode.path("name").asText(request.attackName())
                + " e hizo " + damage + " de dano.";

        if (accumulatedDamage >= (defenderCard.getHp() == null ? 0 : defenderCard.getHp())) {
            knockOutActivePokemon(attacker, defender, defenderActiveCardId);
            message += " " + defenderCard.getName() + " quedo KO.";
        }

        if (attacker.getPrizeCardIds().isEmpty()) {
            game.setStatus(GameStatus.FINISHED);
            game.setFinishedAt(LocalDateTime.now());
            game.setTurnPhase(null);
            message += " " + attacker.getPlayerName() + " gano la partida.";
        } else {
            game.setCurrentPlayerId(defender.getId());
            game.setTurnPhase(TurnPhase.DRAW);
            defender.setEnergyAttachedThisTurn(false);
        }

        addLog(game, attacker.getId(), "ATTACK", message);
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

    private DeckEntity findDeckOrBadRequest(Long deckId) {
        return deckRepository.findById(deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mazo no encontrado"));
    }

    private GamePlayerEntity createPlayer(String playerName, DeckEntity deck, int playerOrder) {
        GamePlayerEntity player = new GamePlayerEntity();
        player.setPlayerName(playerName);
        player.setDeck(deck);
        player.setPlayerOrder(playerOrder);
        return player;
    }

    private GameEntity findActiveGameForCurrentPlayer(Long gameId, Long playerId) {
        GameEntity game = findGame(gameId);
        assertActiveGame(game);
        assertCurrentPlayer(game, playerId);
        return game;
    }

    private void assertWaitingGame(GameEntity game, String errorMessage) {
        if (game.getStatus() != GameStatus.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
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

    private void assertCardInList(List<String> cardIds, String cardId, String errorMessage) {
        if (!cardIds.contains(cardId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
    }

    private void assertBasicPokemon(CardEntity card) {
        if (!POKEMON_SUPERTYPE.equalsIgnoreCase(card.getSupertype())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La carta no es un Pokemon");
        }
        if (!hasSubtype(card, BASIC_SUBTYPE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La carta no es un Pokemon Basico");
        }
    }

    private void assertBasicEnergy(CardEntity card) {
        if (!ENERGY_SUPERTYPE.equalsIgnoreCase(card.getSupertype())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La carta no es una Energia");
        }
        if (!hasSubtype(card, BASIC_SUBTYPE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La energia no es Basica");
        }
    }

    private CardEntity findCardOrBadRequest(String cardId, String errorMessage) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage));
    }

    private Map<String, CardEntity> findCardsById(List<String> cardIds) {
        return cardRepository.findAllById(cardIds).stream()
                .collect(Collectors.toMap(CardEntity::getId, Function.identity()));
    }

    private CardEntity findCard(Map<String, CardEntity> cardsById, String cardId, String errorMessage) {
        CardEntity card = cardsById.get(cardId);
        if (card == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
        return card;
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
        player.setHandCardIds(new ArrayList<>(deckCardIds.subList(0, INITIAL_HAND_SIZE)));
        player.setPrizeCardIds(new ArrayList<>(deckCardIds.subList(PRIZE_CARDS_START_INDEX, REMAINING_DECK_START_INDEX)));
        player.setBenchCardIds(new ArrayList<>());
        player.getAttachedEnergyCardIdsByPokemonCardId().clear();
        player.getDamageByPokemonCardId().clear();
        player.setEnergyAttachedThisTurn(false);
        player.setDeckCardIds(new ArrayList<>(deckCardIds.subList(REMAINING_DECK_START_INDEX, deckCardIds.size())));
        player.setDiscardCardIds(new ArrayList<>());
    }

    private List<String> expandDeck(DeckEntity deck) {
        return deck.getCards().stream()
                .flatMap(deckCard -> Collections.nCopies(deckCard.getQuantity(), deckCard.getCard().getId()).stream())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<GamePlayerEntity> orderedPlayers(GameEntity game) {
        return game.getPlayers().stream()
                .sorted(Comparator.comparingInt(GamePlayerEntity::getPlayerOrder))
                .toList();
    }

    private String activePokemonCardId(GamePlayerEntity player, String errorMessage) {
        if (player.getBenchCardIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
        return player.getBenchCardIds().getFirst();
    }

    private JsonNode findAttack(CardEntity card, String attackName) {
        JsonNode attacks = card.getAttacks();
        if (attacks == null || !attacks.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El Pokemon no tiene ataques");
        }
        for (JsonNode attack : attacks) {
            if (attackName.equalsIgnoreCase(attack.path("name").asText())) {
                return attack;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ataque no encontrado");
    }

    private int requiredEnergy(JsonNode attack) {
        if (attack.hasNonNull("convertedEnergyCost")) {
            return attack.path("convertedEnergyCost").asInt();
        }
        JsonNode cost = attack.path("cost");
        return cost.isArray() ? cost.size() : 0;
    }

    private int attackDamage(JsonNode attack) {
        String damage = attack.path("damage").asText("");
        Matcher matcher = DAMAGE_NUMBER_PATTERN.matcher(damage);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    private void knockOutActivePokemon(GamePlayerEntity attacker, GamePlayerEntity defender, String defenderActiveCardId) {
        defender.getBenchCardIds().remove(defenderActiveCardId);
        defender.getDiscardCardIds().add(defenderActiveCardId);
        List<String> attachedEnergy = defender.getAttachedEnergyCardIdsByPokemonCardId().remove(defenderActiveCardId);
        if (attachedEnergy != null) {
            defender.getDiscardCardIds().addAll(attachedEnergy);
        }
        defender.getDamageByPokemonCardId().remove(defenderActiveCardId);

        if (!attacker.getPrizeCardIds().isEmpty()) {
            attacker.getHandCardIds().add(attacker.getPrizeCardIds().removeFirst());
        }
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
                player.getDamageByPokemonCardId(),
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
