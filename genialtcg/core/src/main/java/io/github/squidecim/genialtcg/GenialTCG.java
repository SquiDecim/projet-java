package io.github.squidecim.genialtcg;

import com.badlogic.gdx.Game;
import io.github.squidecim.genialtcg.mainMenu.FirstScreen;

public class GenialTCG extends Game {

    @Override
    public void create() {
        setScreen(new FirstScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
