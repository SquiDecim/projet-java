package io.github.squidecim.genialtcg;

import com.badlogic.gdx.Game;
import io.github.squidecim.genialtcg.controller.GameController;
import io.github.squidecim.genialtcg.view.GameView;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class GenialTCG extends Game {
    @Override
    public void create() {
        GameView view = new GameView(this);
        GameController controller = new GameController(view);
        setScreen(view);
    }


    @Override
    public void dispose() {
    }
}
