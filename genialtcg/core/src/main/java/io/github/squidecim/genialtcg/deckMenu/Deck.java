package io.github.squidecim.genialtcg.deckMenu;

import com.badlogic.gdx.utils.Array;

public class Deck {

    public String name;
    public Array<String> cardNames;

    public Deck(String name, Array<String> cardNames) {
        this.name = name;
        this.cardNames = new Array<>(cardNames);
    }
}
