package io.github.squidecim.genialtcg.model;

import io.github.squidecim.genialtcg.controller.GameController;
import io.github.squidecim.genialtcg.view.GameView;

import java.util.ArrayList;
import java.util.List;

public class GameModel {

    public CardsStackData deck;
    public List<CardData> hand = new ArrayList<>();
    public List<CardData> bench = new ArrayList<>();

    public GameModel(){

        this.deck = new CardsStackData();
        for (int i = 0; i < 50; i++){
            CardData card = new CardData(String.format("Pays%d", i), Integer.toString(i), "Faible", "Diplomatique", 10, 350, new int[]{100, 60, 45, 75, 28});
            this.deck.addCard(card);
        }
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
}
