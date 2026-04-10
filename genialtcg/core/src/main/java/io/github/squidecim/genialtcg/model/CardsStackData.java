package io.github.squidecim.genialtcg.model;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class CardsStackData {
    private Deque<CardData> stack;

    public CardsStackData(List<CardData> stack){
        this.stack = new ArrayDeque<CardData>(stack);
    }

    public CardsStackData(){
        this.stack = new ArrayDeque<CardData>();
    }

    public void shuffle(){
        List<CardData> temp = new java.util.ArrayList<>(this.stack);
        Collections.shuffle(temp);
        this.stack.clear();
        this.stack.addAll(temp);
    }

    public CardData draw(){
        return this.stack.pollFirst();
    }

    public void addCard(CardData card){
        this.stack.add(card);
    }

    @Override
    public String toString(){
        StringBuilder string = new StringBuilder();
        for (CardData card : this.stack){
            string.append(card.toString()+ "\n");
        }
        return string.toString();
    }

    public int getSize() {
        return this.stack.size();
    }
}
