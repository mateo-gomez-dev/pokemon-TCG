package com.pokemontcg.game.persistence;

import java.util.ArrayList;
import java.util.List;

public class PokemonInPlay {

    private String instanceId;
    private String cardId;
    private PokemonZone zone;
    private int damage;
    private List<String> attachedEnergyCardIds = new ArrayList<>();

    public PokemonInPlay() {
    }

    public PokemonInPlay(String instanceId, String cardId, PokemonZone zone) {
        this.instanceId = instanceId;
        this.cardId = cardId;
        this.zone = zone;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public PokemonZone getZone() {
        return zone;
    }

    public void setZone(PokemonZone zone) {
        this.zone = zone;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public List<String> getAttachedEnergyCardIds() {
        return attachedEnergyCardIds;
    }

    public void setAttachedEnergyCardIds(List<String> attachedEnergyCardIds) {
        this.attachedEnergyCardIds = attachedEnergyCardIds;
    }
}
