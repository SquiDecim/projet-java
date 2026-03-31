package io.github.squidecim.genialtcg;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class GenialTCG extends Game {
    @Override
    public void create() {
        setScreen(new GameView(this));
    }


    @Override
    public void dispose() {
    }
}
