package io.github.squidecim.genialtcg.model;

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
    public final String terrain;
    public CardsStackData deck;
    public List<CardData> hand = new ArrayList<>();
    public List<CardData> bench = new ArrayList<>();
    public List<CardData> discardPile = new ArrayList<>();
    public CardData table;

    public int myCredits = 100;
    public int opponentCredits = 100;

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
    }

    public void moveFromHandToTable(CardData card) {
        hand.remove(card);
        table = card;
    }

    public void moveFromBenchToTable(CardData card){
        bench.remove(card);
        table = card;
    }

    public void moveFromTableToBench(CardData card){
        table = null;
        bench.add(card);
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
        myCredits = Math.max(0, myCredits - amount);
    }

    public void receiveCredits(int amount) {
        myCredits += amount;
    }

    public void applyDamage(CardDecal card, int damage) {
        card.getData().pv = Math.max(card.getData().pv + damage, 0);
        card.refreshStats();
    }

    public void damageTableCard(int amount) {
        if (table != null) {
            table.pv += amount;
        }
    }
}
