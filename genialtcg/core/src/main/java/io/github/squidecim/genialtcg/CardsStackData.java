package io.github.squidecim.genialtcg;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

public class CardsStackData {
    private Deque<CardData> stack;

    public CardsStackData(List<CardData> stack){
        this.stack = new ArrayDeque<>(stack);
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
}
