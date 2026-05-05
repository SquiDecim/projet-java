package io.github.squidecim.genialtcg.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class CardsStackData {

    public String name;
    private Deque<CardData> stack;
    private int startCredits = 200;

    public CardsStackData(String name, List<CardData> cards) {
        this.name = name;
        this.stack = new ArrayDeque<>(cards);
    }

    public CardsStackData(String name) {
        this.name = name;
        this.stack = new ArrayDeque<>();
    }

    public CardsStackData() {
        this.stack = new ArrayDeque<>();
    }

    public void shuffle() {
        List<CardData> temp = new ArrayList<>(this.stack);
        Collections.shuffle(temp);

        List<CardData> placeables = new ArrayList<>();
        for (CardData card : temp) {
            if (card.cost <= startCredits) {
                placeables.add(card);
            }
        }
        this.stack.clear();

        if (!placeables.isEmpty()) {
            CardData chosen = placeables.get((int)(Math.random() * placeables.size()));
            temp.remove(chosen);

            this.stack.addFirst(chosen);
        }

        this.stack.addAll(temp);
    }


    public CardData draw() {
        return this.stack.pollFirst();
    }

    public void addCard(CardData card) {
        this.stack.add(card);
    }

    public void clearCards() {
        this.stack.clear();
    }

    public List<CardData> getCards() {
        return new ArrayList<>(this.stack);
    }

    public boolean containsByName(String name) {
        for (CardData card : this.stack) {
            if (card.country != null && card.country.equals(name)) return true;
        }
        return false;
    }

    public void removeByName(String name) {
        this.stack.removeIf(
            card -> card.country != null && card.country.equals(name)
        );
    }

    public int getSize() {
        return this.stack.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(
            "\n============================================================"
        );
        sb.append("\nDECK : ").append(name != null ? name : "Sans nom");
        sb.append(
            "\n============================================================"
        );

        for (CardData card : this.getCards()) {
            sb.append("\n").append(card.toString());
            sb.append(
                "\n------------------------------------------------------------"
            );
        }

        sb.append(
            "\n============================================================\n"
        );
        return sb.toString();
    }
}
