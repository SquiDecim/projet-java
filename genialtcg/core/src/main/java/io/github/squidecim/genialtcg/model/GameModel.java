package io.github.squidecim.genialtcg.model;

import io.github.squidecim.genialtcg.GenialTCG;
import java.util.ArrayList;
import java.util.List;

public class GameModel {

    private final GenialTCG game;
    public CardsStackData deck;
    public List<CardData> hand = new ArrayList<>();
    public List<CardData> bench = new ArrayList<>();
    public CardData table;

    public GameModel(GenialTCG game, CardsStackData deck) {
        this.game = game;
        this.deck = deck;
        this.deck.shuffle();
    }

    public CardData drawCard() {
        CardData card = deck.draw();
        if (card != null) hand.add(card);
        return card;
    }

    public int deckSize() { return deck.getSize(); }

    public void moveFromHandToBench(CardData card) {
        hand.remove(card);
        bench.add(card);
    }

    public void moveFromHandToTable(CardData card) {
        hand.remove(card);
        table = card;
    }
}
