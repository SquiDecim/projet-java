package io.github.squidecim.genialtcg.model;

import com.badlogic.gdx.graphics.Color;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.view.CardDecal;

import java.util.ArrayList;
import java.util.List;

public class GameModel {

    public static final int MAX_BENCH = 4;
    public static final String[] TERRAINS = {
        "Désertique",
        "Glacial",
        "Montagneux",
        "Océanique",
        "Tempéré",
        "Tropical",
    };

    private final GenialTCG game;
    public String terrain = "Tempéré";
    public int opponentToolBlockedTurns = 0;
    public int myToolBlockedTurns = 0;
    public CardsStackData deck;
    public List<CardData> hand = new ArrayList<>();
    public List<CardData> bench = new ArrayList<>();
    public List<CardData> discardPile = new ArrayList<>();
    public CardData table;
    public boolean hasUseAction = true;

    public enum Phase { DRAW, SETUP, PLAYING }
    public Phase phase = Phase.DRAW;
    public boolean myTurn = false;
    public boolean setupDone = false;
    public int turnCount = 1;

    public int points = 0;

    public int myCredits = 300;
    public int opponentCredits = 300;

    public GameModel(GenialTCG game, CardsStackData deck) {
        this.game = game;
        this.deck = deck;
        this.deck.shuffle();
        this.terrain = TERRAINS[4];
    }

    public CardData drawCard() {
        CardData card = deck.draw();
        if (card != null) hand.add(card);
        return card;
    }

    public int deckSize() {
        return deck.getSize();
    }

    public void moveFromHandToBench(CardData card) {
        hand.remove(card);
        bench.add(card);
        spendCredits(card.cost);
    }

    public void moveFromHandToTable(CardData card) {
        hand.remove(card);
        table = card;
        spendCredits(card.cost);
    }

    public void moveFromBenchToTable(CardData card){
        bench.remove(card);
        table = card;
    }

    public void moveFromTableToBench(CardData card){
        table = null;
        bench.add(card);
    }

    public void useFromHand(CardData card){
        hand.remove(card);
    }

    public boolean isBenchFull() {
        return bench.size() >= MAX_BENCH;
    }

    public void discardCard(CardData card) {
        bench.remove(card);
        if (table == card) table = null;
        discardPile.add(card);
    }

    public CardData lookupCard(String regionName) {
        return game.allCardsMap.get(regionName);
    }

    public boolean isDeckEmpty() {
        return deck.getSize() == 0 && hand.isEmpty();
    }

    public void spendCredits(int amount) {
        myCredits = myCredits - amount;
    }


    public void receiveCredits(int amount) {
        myCredits += amount;
    }

    public boolean applyDamage(CardDecal card, int damage) {
        card.getData().pv = Math.max(card.getData().pv + damage, 0);
        card.refreshStats();
        card.shake();
        return card.getData().pv == 0;
    }

    public int getTableEconomy(){
        if (table == null) return 0;
        int base = table.stats[1];
        int bonus = getTerrainBonus(terrain, table.type)[1];
        int toolBonus = getToolStatBonus(table.attachedTool, true)[1];
        return Math.max(0, base + bonus + toolBonus);
    }

    public static int getToolCostEffect(CardData tool, String effectType) {
        if (tool == null || tool.specialEffectTypes == null) return 0;
        int total = 0;
        for (int i = 0; i < tool.specialEffectTypes.length; i++) {
            if (effectType.equals(tool.specialEffectTypes[i])) total += tool.specialEffectValues[i];
        }
        return total;
    }

    public static int[] getToolStatBonus(CardData tool, boolean forOwner) {
        int[] bonus = new int[5];
        if (tool == null || tool.specialEffectTypes == null) return bonus;
        for (int i = 0; i < tool.specialEffectTypes.length; i++) {
            String type = tool.specialEffectTypes[i];
            int value = tool.specialEffectValues[i];
            if (forOwner) {
                switch (type) {
                    case "puissance":   bonus[0] += value; break;
                    case "economie":    bonus[1] += value; break;
                    case "ressources":  bonus[2] += value; break;
                    case "technologie": bonus[3] += value; break;
                    case "stabilite":   bonus[4] += value; break;
                }
            } else {
                switch (type) {
                    case "puissanceA":   bonus[0] += value; break;
                    case "economieA":    bonus[1] += value; break;
                    case "ressourcesA":  bonus[2] += value; break;
                    case "technologieA": bonus[3] += value; break;
                    case "stabiliteA":   bonus[4] += value; break;
                }
            }
        }
        return bonus;
    }

    public int getBenchEconomy(){
        int sum = 0;
        for (CardData benchCard : bench) {
            int base = benchCard.stats[1];
            int bonus = getTerrainBonus(terrain, benchCard.type)[1];
            sum += Math.max(0, base + bonus);
        }
        return Math.round(sum * 0.4f);
    }

    public int getTotalEconomy(){
        return getBenchEconomy() + getTableEconomy();
    }

    public static Color getTerrainColor(String terrain) {
        switch (terrain) {
            case "Désertique":  return new Color(0.90f, 0.58f, 0.12f, 1f);
            case "Tropical":    return new Color(0.08f, 0.72f, 0.22f, 1f);
            case "Montagneux":  return new Color(0.62f, 0.42f, 0.18f, 1f);
            case "Glacial":     return new Color(0.25f, 0.72f, 0.95f, 1f);
            case "Océanique":   return new Color(0.10f, 0.48f, 0.88f, 1f);
            default:            return Color.BLACK;
        }
    }

    // Retourne les bonus de stats [puissance, economie, ressources, technologie, stabilite]
    // appliqués à un type de nation selon le terrain actif.
    public static int[] getTerrainBonus(String terrain, String type) {
        int[] bonus = new int[5];
        switch (terrain) {
            case "Désertique":
                switch (type) {
                    case "Militaire":      bonus[0] = +10; break;
                    case "Économique":     bonus[2] = -10; break;
                    case "Diplomatique":   bonus[4] = -10; break;
                    case "Renseignement":  bonus[3] = +10; break;
                    case "Isolationniste": bonus[4] = +10; break;
                    case "Technologique":  bonus[1] = -10; break;
                }
                break;
            case "Tropical":
                switch (type) {
                    case "Militaire":      bonus[0] = -10; break;
                    case "Économique":     bonus[2] = +15; break;
                    case "Diplomatique":   bonus[4] = +10; break;
                    case "Renseignement":  bonus[0] = +10; break;
                    case "Isolationniste": bonus[4] = +10; break;
                    case "Technologique":  bonus[3] = -10; break;
                }
                break;
            case "Montagneux":
                switch (type) {
                    case "Militaire":      bonus[0] = +15; break;
                    case "Économique":     bonus[1] = -10; break;
                    case "Diplomatique":   bonus[1] = -10; break;
                    case "Renseignement":  bonus[0] = +10; break;
                    case "Isolationniste": bonus[4] = +15; break;
                    case "Technologique":  bonus[3] = +10; break;
                }
                break;
            case "Glacial":
                switch (type) {
                    case "Militaire":      bonus[4] = -10; break;
                    case "Économique":     bonus[2] = -10; break;
                    case "Diplomatique":   bonus[1] = -15; break;
                    case "Renseignement":  bonus[0] = -10; break;
                    case "Isolationniste": bonus[4] = +15; break;
                    case "Technologique":  bonus[3] = +15; break;
                }
                break;
            case "Océanique":
                switch (type) {
                    case "Militaire":      bonus[0] = +10; break;
                    case "Économique":     bonus[1] = +15; break;
                    case "Diplomatique":   bonus[1] = +10; break;
                    case "Renseignement":  bonus[0] = +10; break;
                    case "Isolationniste": bonus[4] = -15; break;
                    case "Technologique":  bonus[3] = +10; break;
                }
                break;
            // Tempéré : aucun modificateur
        }
        return bonus;
    }

}
