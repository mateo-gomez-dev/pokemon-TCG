package com.pokemontcg.card.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class PokemonTcgCardDto {

    private String id;
    private String name;
    private String supertype;
    private List<String> subtypes;
    private String hp;
    private List<String> types;
    private String evolvesFrom;
    private List<String> rules;
    private JsonNode abilities;
    private JsonNode attacks;
    private JsonNode weaknesses;
    private JsonNode resistances;
    private List<String> retreatCost;
    private Integer convertedRetreatCost;
    private String number;
    private String rarity;
    private PokemonTcgImagesDto images;
    private PokemonTcgSetDto set;
    private JsonNode rawJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSupertype() {
        return supertype;
    }

    public void setSupertype(String supertype) {
        this.supertype = supertype;
    }

    public List<String> getSubtypes() {
        return subtypes;
    }

    public void setSubtypes(List<String> subtypes) {
        this.subtypes = subtypes;
    }

    public String getHp() {
        return hp;
    }

    public void setHp(String hp) {
        this.hp = hp;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public String getEvolvesFrom() {
        return evolvesFrom;
    }

    public void setEvolvesFrom(String evolvesFrom) {
        this.evolvesFrom = evolvesFrom;
    }

    public List<String> getRules() {
        return rules;
    }

    public void setRules(List<String> rules) {
        this.rules = rules;
    }

    public JsonNode getAbilities() {
        return abilities;
    }

    public void setAbilities(JsonNode abilities) {
        this.abilities = abilities;
    }

    public JsonNode getAttacks() {
        return attacks;
    }

    public void setAttacks(JsonNode attacks) {
        this.attacks = attacks;
    }

    public JsonNode getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(JsonNode weaknesses) {
        this.weaknesses = weaknesses;
    }

    public JsonNode getResistances() {
        return resistances;
    }

    public void setResistances(JsonNode resistances) {
        this.resistances = resistances;
    }

    public List<String> getRetreatCost() {
        return retreatCost;
    }

    public void setRetreatCost(List<String> retreatCost) {
        this.retreatCost = retreatCost;
    }

    public Integer getConvertedRetreatCost() {
        return convertedRetreatCost;
    }

    public void setConvertedRetreatCost(Integer convertedRetreatCost) {
        this.convertedRetreatCost = convertedRetreatCost;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public PokemonTcgImagesDto getImages() {
        return images;
    }

    public void setImages(PokemonTcgImagesDto images) {
        this.images = images;
    }

    public PokemonTcgSetDto getSet() {
        return set;
    }

    public void setSet(PokemonTcgSetDto set) {
        this.set = set;
    }

    public JsonNode getRawJson() {
        return rawJson;
    }

    public void setRawJson(JsonNode rawJson) {
        this.rawJson = rawJson;
    }
}
