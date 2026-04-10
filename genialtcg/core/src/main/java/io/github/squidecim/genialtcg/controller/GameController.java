package io.github.squidecim.genialtcg.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.collision.Ray;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.GameModel;
import io.github.squidecim.genialtcg.view.GameView;

public class GameController implements InputProcessor {
    private GameView view;
    private GameModel model;

    public GameController(GameView view, GameModel model) {
        this.view = view;
        this.model = model;
        Gdx.input.setInputProcessor(this);

    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        Ray ray = view.getCam().getPickRay(screenX, screenY);
        view.updateHover(ray);
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int p, int b) {
        Ray ray = view.getCam().getPickRay(x, y);
        Gdx.app.log("DEBUG", "clic à screen: " + x + ", " + y);
        Gdx.app.log("DEBUG", "ray origin: " + ray.origin);
        Gdx.app.log("DEBUG", "ray direction: " + ray.direction);

        if (view.isDeckClicked(ray)) {
            Gdx.app.log("DEBUG", "deck cliqué !");
            CardData drawn = model.drawCard();
            if (drawn != null) {
                view.addCardToHand(drawn);
                view.updateDeckVisual(model.deckSize());
            }
        } else {
            Gdx.app.log("DEBUG", "pas sur le deck");
        }
        return false;
    }

    // les autres méthodes d'InputProcessor — toutes retournent false pour l'instant
    @Override public boolean keyDown(int k)                          { return false; }
    @Override public boolean keyUp(int k)                           { return false; }

    @Override public boolean keyTyped(char c)                       { return false; }

    @Override public boolean touchUp(int x, int y, int p, int b)    { return false; }
    @Override public boolean touchDragged(int x, int y, int p)      { return false; }
    @Override public boolean scrolled(float x, float y)             { return false; }
    @Override public boolean touchCancelled(int x, int y, int p, int b) { return false; }
}
