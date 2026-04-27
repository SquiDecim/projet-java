package io.github.squidecim.genialtcg.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class CardsStackData {

    public String name;
    private Deque<CardData> stack;

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
        this.stack.clear();
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
            if (card.country.equals(name)) return true;
        }
        return false;
    }

    public void removeByName(String name) {
        this.stack.removeIf(card -> card.country.equals(name));
    }

    public int getSize() {
        return this.stack.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CardData card : this.stack) {
            sb.append(card.toString()).append("\n");
        }
        return sb.toString();
    }
}
