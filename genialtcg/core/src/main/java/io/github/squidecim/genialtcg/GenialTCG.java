package io.github.squidecim.genialtcg;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.Array;
import io.github.squidecim.genialtcg.deckMenu.Deck;
import io.github.squidecim.genialtcg.mainMenu.FirstScreen;

public class GenialTCG extends Game {

    public Array<Deck> savedDecks = new Array<>();

    @Override
    public void create() {
        setScreen(new FirstScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
