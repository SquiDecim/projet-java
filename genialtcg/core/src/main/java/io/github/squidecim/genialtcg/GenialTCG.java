package io.github.squidecim.genialtcg;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.Array;
import io.github.squidecim.genialtcg.mainMenu.FirstScreen;
import io.github.squidecim.genialtcg.model.CardsStackData;

public class GenialTCG extends Game {

    public Array<CardsStackData> savedDecks = new Array<>();

    @Override
    public void create() {
        setScreen(new FirstScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
