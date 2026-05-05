package com.pokemontcg.game.persistence;

import com.pokemontcg.deck.persistence.DeckEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "game_players")
public class GamePlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "deck_id", nullable = false)
    private DeckEntity deck;

    @Column(nullable = false)
    private String playerName;

    @Column(name = "player_order", nullable = false)
    private int playerOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> deckCardIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> handCardIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> prizeCardIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> benchCardIds = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, List<String>> attachedEnergyCardIdsByPokemonCardId = new HashMap<>();

    @Column(nullable = false)
    private boolean energyAttachedThisTurn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> discardCardIds = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public GameEntity getGame() {
        return game;
    }

    public void setGame(GameEntity game) {
        this.game = game;
    }

    public DeckEntity getDeck() {
        return deck;
    }

    public void setDeck(DeckEntity deck) {
        this.deck = deck;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getPlayerOrder() {
        return playerOrder;
    }

    public void setPlayerOrder(int playerOrder) {
        this.playerOrder = playerOrder;
    }

    public List<String> getDeckCardIds() {
        return deckCardIds;
    }

    public void setDeckCardIds(List<String> deckCardIds) {
        this.deckCardIds = deckCardIds;
    }

    public List<String> getHandCardIds() {
        return handCardIds;
    }

    public void setHandCardIds(List<String> handCardIds) {
        this.handCardIds = handCardIds;
    }

    public List<String> getPrizeCardIds() {
        return prizeCardIds;
    }

    public void setPrizeCardIds(List<String> prizeCardIds) {
        this.prizeCardIds = prizeCardIds;
    }

    public List<String> getBenchCardIds() {
        return benchCardIds;
    }

    public void setBenchCardIds(List<String> benchCardIds) {
        this.benchCardIds = benchCardIds;
    }

    public Map<String, List<String>> getAttachedEnergyCardIdsByPokemonCardId() {
        return attachedEnergyCardIdsByPokemonCardId;
    }

    public void setAttachedEnergyCardIdsByPokemonCardId(Map<String, List<String>> attachedEnergyCardIdsByPokemonCardId) {
        this.attachedEnergyCardIdsByPokemonCardId = attachedEnergyCardIdsByPokemonCardId;
    }

    public boolean isEnergyAttachedThisTurn() {
        return energyAttachedThisTurn;
    }

    public void setEnergyAttachedThisTurn(boolean energyAttachedThisTurn) {
        this.energyAttachedThisTurn = energyAttachedThisTurn;
    }

    public List<String> getDiscardCardIds() {
        return discardCardIds;
    }

    public void setDiscardCardIds(List<String> discardCardIds) {
        this.discardCardIds = discardCardIds;
    }
}
