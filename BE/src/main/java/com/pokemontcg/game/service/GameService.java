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
import com.pokemontcg.game.dto.PokemonInPlayResponse;
import com.pokemontcg.game.dto.PlayBasicPokemonRequest;
import com.pokemontcg.game.dto.PromoteActiveRequest;
import com.pokemontcg.game.persistence.GameEntity;
import com.pokemontcg.game.persistence.GameLogEntity;
import com.pokemontcg.game.persistence.GamePlayerEntity;
import com.pokemontcg.game.persistence.GameRepository;
import com.pokemontcg.game.persistence.GameStatus;
import com.pokemontcg.game.persistence.PokemonInPlay;
import com.pokemontcg.game.persistence.PokemonZone;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.pokemontcg.card.domain.CardRules.BASIC_SUBTYPE;
import static com.pokemontcg.card.domain.CardRules.ENERGY_SUPERTYPE;
import static com.pokemontcg.card.domain.CardRules.POKEMON_SUPERTYPE;
import static com.pokemontcg.card.domain.CardRules.hasSubtype;

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
        PokemonZone targetZone = playTargetZone(player, request.targetZone());
        if (targetZone == PokemonZone.BENCH && benchPokemon(player).size() >= MAX_BENCH_SIZE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La banca ya tiene " + MAX_BENCH_SIZE + " Pokemon");
        }
        if (targetZone == PokemonZone.ACTIVE && activePokemon(player) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El jugador ya tiene Pokemon activo");
        }

        CardEntity card = findCardOrBadRequest(request.cardId(), "Carta no encontrada");
        assertBasicPokemon(card);

        player.getHandCardIds().remove(request.cardId());
        PokemonInPlay pokemon = new PokemonInPlay(UUID.randomUUID().toString(), request.cardId(), targetZone);
        player.getPokemonInPlay().add(pokemon);
        if (targetZone == PokemonZone.ACTIVE) {
            player.setActivePokemonInstanceId(pokemon.getInstanceId());
            addLog(game, player.getId(), "PLAY_BASIC_POKEMON", "Jugador " + player.getPlayerName() + " coloco " + card.getName() + " como Pokemon activo.");
        } else {
            addLog(game, player.getId(), "PLAY_BASIC_POKEMON", "Jugador " + player.getPlayerName() + " jugo " + card.getName() + " en la banca.");
        }
        syncDerivedPokemonFields(player);

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse attachEnergy(Long gameId, AttachEnergyRequest request) {
        GameEntity game = findActiveGameForCurrentPlayer(gameId, request.playerId());
        assertMainPhase(game);

        GamePlayerEntity player = findPlayer(game, request.playerId());
        assertCardInList(player.getHandCardIds(), request.energyCardId(), "La energia no esta en la mano del jugador");
        PokemonInPlay targetPokemon = resolvePokemonTarget(player, request.pokemonInstanceId(), request.targetPokemonCardId());
        if (player.isEnergyAttachedThisTurn()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El jugador ya unio una energia este turno");
        }

        CardEntity energyCard = findCardOrBadRequest(request.energyCardId(), "Energia no encontrada");
        assertBasicEnergy(energyCard);

        player.getHandCardIds().remove(request.energyCardId());
        targetPokemon.getAttachedEnergyCardIds().add(request.energyCardId());
        player.setEnergyAttachedThisTurn(true);
        syncDerivedPokemonFields(player);
        addLog(game, player.getId(), "ATTACH_ENERGY", "Jugador " + player.getPlayerName() + " unio " + energyCard.getName() + " a " + targetPokemon.getInstanceId() + ".");

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse promoteActive(Long gameId, PromoteActiveRequest request) {
        GameEntity game = findGame(gameId);
        assertActiveGame(game);

        GamePlayerEntity player = findPlayer(game, request.playerId());
        if (activePokemon(player) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El jugador ya tiene Pokemon activo");
        }

        PokemonInPlay pokemon = findPokemonByInstanceId(player, request.pokemonInstanceId(), "El Pokemon no pertenece al jugador");
        if (pokemon.getZone() != PokemonZone.BENCH) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El Pokemon no esta en banca");
        }

        pokemon.setZone(PokemonZone.ACTIVE);
        player.setActivePokemonInstanceId(pokemon.getInstanceId());
        syncDerivedPokemonFields(player);
        addLog(game, player.getId(), "PROMOTE_ACTIVE", "Jugador " + player.getPlayerName() + " promovio " + pokemon.getCardId() + " como Pokemon activo.");

        return toResponse(gameRepository.save(game));
    }

    @Transactional
    public GameResponse attack(Long gameId, AttackRequest request) {
        GameEntity game = findActiveGameForCurrentPlayer(gameId, request.playerId());
        assertMainPhase(game);

        GamePlayerEntity attacker = findPlayer(game, request.playerId());
        GamePlayerEntity defender = findOtherPlayer(game, request.playerId());
        PokemonInPlay attackerActive = requireActivePokemon(attacker, "El jugador actual no tiene Pokemon activo");
        PokemonInPlay defenderActive = requireActivePokemon(defender, "El rival no tiene Pokemon activo");
        String attackerActiveCardId = attackerActive.getCardId();
        String defenderActiveCardId = defenderActive.getCardId();

        Map<String, CardEntity> cardsById = findCardsById(List.of(attackerActiveCardId, defenderActiveCardId));
        CardEntity attackerCard = findCard(cardsById, attackerActiveCardId, "Pokemon atacante no encontrado");
        CardEntity defenderCard = findCard(cardsById, defenderActiveCardId, "Pokemon defensor no encontrado");
        JsonNode attackNode = findAttack(attackerCard, request.attackName());
        int requiredEnergy = requiredEnergy(attackNode);
        int attachedEnergy = attackerActive.getAttachedEnergyCardIds().size();
        if (attachedEnergy < requiredEnergy) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Energia insuficiente para atacar");
        }

        int damage = attackDamage(attackNode);
        int accumulatedDamage = defenderActive.getDamage() + damage;
        defenderActive.setDamage(accumulatedDamage);

        String message = attacker.getPlayerName() + " ataco con " + attackerCard.getName()
                + " usando " + attackNode.path("name").asText(request.attackName())
                + " e hizo " + damage + " de dano.";

        if (accumulatedDamage >= (defenderCard.getHp() == null ? 0 : defenderCard.getHp())) {
            knockOutActivePokemon(attacker, defender, defenderActive);
            message += " " + defenderCard.getName() + " quedo KO.";
        }

        syncDerivedPokemonFields(attacker);
        syncDerivedPokemonFields(defender);

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
        GameEntity game = gameRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partida no encontrada"));
        ensurePokemonState(game);
        return game;
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
        Iterable<CardEntity> cards = cardRepository.findAllById(cardIds.stream().distinct().toList());
        if (cards == null) {
            return Map.of();
        }
        return StreamSupport.stream(cards.spliterator(), false)
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
        player.setActivePokemonCardId(null);
        player.setActivePokemonInstanceId(null);
        player.setPokemonInPlay(new ArrayList<>());
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

    private PokemonInPlay requireActivePokemon(GamePlayerEntity player, String errorMessage) {
        PokemonInPlay pokemon = activePokemon(player);
        if (pokemon == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
        }
        return pokemon;
    }

    private PokemonInPlay activePokemon(GamePlayerEntity player) {
        if (player.getActivePokemonInstanceId() == null || player.getActivePokemonInstanceId().isBlank()) {
            return null;
        }
        return player.getPokemonInPlay().stream()
                .filter(pokemon -> player.getActivePokemonInstanceId().equals(pokemon.getInstanceId()))
                .filter(pokemon -> pokemon.getZone() == PokemonZone.ACTIVE)
                .findFirst()
                .orElse(null);
    }

    private List<PokemonInPlay> benchPokemon(GamePlayerEntity player) {
        return player.getPokemonInPlay().stream()
                .filter(pokemon -> pokemon.getZone() == PokemonZone.BENCH)
                .toList();
    }

    private PokemonZone playTargetZone(GamePlayerEntity player, String requestedTargetZone) {
        if (requestedTargetZone == null || requestedTargetZone.isBlank()) {
            return activePokemon(player) == null ? PokemonZone.ACTIVE : PokemonZone.BENCH;
        }
        try {
            return PokemonZone.valueOf(requestedTargetZone.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetZone debe ser ACTIVE o BENCH");
        }
    }

    private PokemonInPlay resolvePokemonTarget(GamePlayerEntity player, String pokemonInstanceId, String targetPokemonCardId) {
        if (pokemonInstanceId != null && !pokemonInstanceId.isBlank()) {
            return findPokemonByInstanceId(player, pokemonInstanceId, "El Pokemon objetivo no esta en juego del jugador");
        }
        if (targetPokemonCardId == null || targetPokemonCardId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se debe enviar pokemonInstanceId");
        }

        List<PokemonInPlay> matches = player.getPokemonInPlay().stream()
                .filter(pokemon -> targetPokemonCardId.equals(pokemon.getCardId()))
                .toList();
        if (matches.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El Pokemon objetivo no esta en juego del jugador");
        }
        if (matches.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hay mas de una instancia de esa carta en juego; usa pokemonInstanceId");
        }
        return matches.getFirst();
    }

    private PokemonInPlay findPokemonByInstanceId(GamePlayerEntity player, String pokemonInstanceId, String errorMessage) {
        return player.getPokemonInPlay().stream()
                .filter(pokemon -> pokemonInstanceId.equals(pokemon.getInstanceId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, errorMessage));
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

    private void knockOutActivePokemon(GamePlayerEntity attacker, GamePlayerEntity defender, PokemonInPlay defenderActive) {
        if (defenderActive.getInstanceId().equals(defender.getActivePokemonInstanceId())) {
            defender.setActivePokemonInstanceId(null);
        }
        defender.getPokemonInPlay().removeIf(pokemon -> defenderActive.getInstanceId().equals(pokemon.getInstanceId()));
        defender.getDiscardCardIds().add(defenderActive.getCardId());
        defender.getDiscardCardIds().addAll(defenderActive.getAttachedEnergyCardIds());

        if (!attacker.getPrizeCardIds().isEmpty()) {
            attacker.getHandCardIds().add(attacker.getPrizeCardIds().removeFirst());
        }
    }

    private void ensurePokemonState(GameEntity game) {
        game.getPlayers().forEach(this::ensurePokemonState);
    }

    private void ensurePokemonState(GamePlayerEntity player) {
        if (player.getPokemonInPlay() == null) {
            player.setPokemonInPlay(new ArrayList<>());
        }
        if (player.getAttachedEnergyCardIdsByPokemonCardId() == null) {
            player.setAttachedEnergyCardIdsByPokemonCardId(new LinkedHashMap<>());
        }
        if (player.getDamageByPokemonCardId() == null) {
            player.setDamageByPokemonCardId(new LinkedHashMap<>());
        }
        for (PokemonInPlay pokemon : player.getPokemonInPlay()) {
            if (pokemon.getAttachedEnergyCardIds() == null) {
                pokemon.setAttachedEnergyCardIds(new ArrayList<>());
            }
        }
        if (player.getPokemonInPlay().isEmpty()) {
            migrateLegacyPokemonState(player);
        }
        if (activePokemon(player) == null) {
            player.getPokemonInPlay().stream()
                    .filter(pokemon -> pokemon.getZone() == PokemonZone.ACTIVE)
                    .findFirst()
                    .ifPresent(pokemon -> player.setActivePokemonInstanceId(pokemon.getInstanceId()));
        }
        syncDerivedPokemonFields(player);
    }

    private void migrateLegacyPokemonState(GamePlayerEntity player) {
        if (player.getActivePokemonCardId() != null && !player.getActivePokemonCardId().isBlank()) {
            PokemonInPlay active = legacyPokemon(player, player.getActivePokemonCardId(), PokemonZone.ACTIVE);
            player.getPokemonInPlay().add(active);
            player.setActivePokemonInstanceId(active.getInstanceId());
        }
        for (String cardId : player.getBenchCardIds()) {
            player.getPokemonInPlay().add(legacyPokemon(player, cardId, PokemonZone.BENCH));
        }
    }

    private PokemonInPlay legacyPokemon(GamePlayerEntity player, String cardId, PokemonZone zone) {
        PokemonInPlay pokemon = new PokemonInPlay(UUID.randomUUID().toString(), cardId, zone);
        pokemon.setDamage(player.getDamageByPokemonCardId().getOrDefault(cardId, 0));
        pokemon.setAttachedEnergyCardIds(new ArrayList<>(player.getAttachedEnergyCardIdsByPokemonCardId().getOrDefault(cardId, List.of())));
        return pokemon;
    }

    private void syncDerivedPokemonFields(GamePlayerEntity player) {
        PokemonInPlay active = activePokemon(player);
        player.setActivePokemonCardId(active == null ? null : active.getCardId());
        player.setBenchCardIds(benchPokemon(player).stream()
                .map(PokemonInPlay::getCardId)
                .collect(Collectors.toCollection(ArrayList::new)));

        Map<String, List<String>> energyByCardId = new LinkedHashMap<>();
        Map<String, Integer> damageByCardId = new LinkedHashMap<>();
        for (PokemonInPlay pokemon : player.getPokemonInPlay()) {
            energyByCardId.computeIfAbsent(pokemon.getCardId(), ignored -> new ArrayList<>())
                    .addAll(pokemon.getAttachedEnergyCardIds());
            damageByCardId.merge(pokemon.getCardId(), pokemon.getDamage(), Integer::sum);
        }
        player.setAttachedEnergyCardIdsByPokemonCardId(energyByCardId);
        player.setDamageByPokemonCardId(damageByCardId);
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
        ensurePokemonState(player);
        DeckEntity deck = player.getDeck();
        Map<String, CardEntity> cardsById = findCardsById(player.getPokemonInPlay().stream()
                .map(PokemonInPlay::getCardId)
                .distinct()
                .toList());
        PokemonInPlay active = activePokemon(player);
        List<PokemonInPlayResponse> benchPokemon = benchPokemon(player).stream()
                .map(pokemon -> toPokemonInPlayResponse(pokemon, cardsById))
                .toList();
        Map<String, List<String>> energyByInstanceId = player.getPokemonInPlay().stream()
                .collect(Collectors.toMap(PokemonInPlay::getInstanceId, pokemon -> pokemon.getAttachedEnergyCardIds(), (first, second) -> first, LinkedHashMap::new));
        Map<String, Integer> damageByInstanceId = player.getPokemonInPlay().stream()
                .collect(Collectors.toMap(PokemonInPlay::getInstanceId, PokemonInPlay::getDamage, (first, second) -> first, LinkedHashMap::new));

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
                player.getActivePokemonInstanceId(),
                player.getActivePokemonCardId(),
                active == null ? null : toPokemonInPlayResponse(active, cardsById),
                benchPokemon,
                player.getDeckCardIds(),
                player.getHandCardIds(),
                player.getPrizeCardIds(),
                player.getBenchCardIds(),
                energyByInstanceId,
                player.getAttachedEnergyCardIdsByPokemonCardId(),
                damageByInstanceId,
                player.getDamageByPokemonCardId(),
                player.getDiscardCardIds()
        );
    }

    private PokemonInPlayResponse toPokemonInPlayResponse(PokemonInPlay pokemon, Map<String, CardEntity> cardsById) {
        CardEntity card = cardsById.get(pokemon.getCardId());
        Integer hp = card == null ? null : card.getHp();
        int remainingHp = hp == null ? 0 : Math.max(0, hp - pokemon.getDamage());
        return new PokemonInPlayResponse(
                pokemon.getInstanceId(),
                pokemon.getCardId(),
                card == null ? pokemon.getCardId() : card.getName(),
                hp,
                pokemon.getDamage(),
                remainingHp,
                pokemon.getAttachedEnergyCardIds(),
                pokemon.getAttachedEnergyCardIds().size()
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
