package io.github.squidecim.genialtcg;

import com.badlogic.gdx.Game;
import io.github.squidecim.genialtcg.controller.GameController;
import io.github.squidecim.genialtcg.model.GameModel;
import io.github.squidecim.genialtcg.view.GameView;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class GenialTCG extends Game {
    @Override
    public void create() {
        GameModel model = new GameModel();
        GameView view = new GameView(this, model);
        GameController controller = new GameController(view, model);
        view.setController(controller);

        setScreen(view);
    }


    @Override
    public void dispose() {
    }
}
