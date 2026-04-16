package io.github.squidecim.genialtcg.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.collision.Ray;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.GameModel;
import io.github.squidecim.genialtcg.view.CardDecal;
import io.github.squidecim.genialtcg.view.CardSlot;
import io.github.squidecim.genialtcg.view.GameView;

public class GameController implements InputProcessor {
    private GameView view;
    private GameModel model;

    private boolean canDraw = true;
    private float drawCooldown = 0.75f;
    private float drawTimer = 0f;

    private CardDecal draggedCard = null;

    public GameController(GameView view, GameModel model) {
        this.view = view;
        this.model = model;
        Gdx.input.setInputProcessor(this);

    }

    public void update(float delta) {
        if (!canDraw) {
            drawTimer += delta;
            if (drawTimer >= drawCooldown) {
                canDraw = true;
                drawTimer = 0f;
            }
        }
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


        CardDecal card = view.getHoveredCard(ray);
        if (card != null) {
            draggedCard = card;
            view.startDrag(draggedCard);
            Gdx.app.log("DEBUG", "Une carte vient d'être cliquée");
            return true;
        }


        if (view.isDeckClicked(ray)) {
            if (!canDraw) return false;
            CardData drawn = model.drawCard();
            if (drawn != null) {
                view.addCardToHand(drawn);
                view.updateDeckVisual(model.deckSize());
                canDraw = false;
            }
        }
        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int p, int b) {
        if (draggedCard == null) return false;
        Ray ray = view.getCam().getPickRay(x, y);

        CardSlot slot = view.getHighlightedSlot(ray);
        if (slot != null && slot.isEmpty()) {

            CardData data = draggedCard.getData();
            model.moveFromHandToBench(data);
            view.dropCardOnSlot(draggedCard, slot);
        } else {
            view.cancelDrag(draggedCard);
        }

        draggedCard = null;
        return true;
    }

    @Override
    public boolean touchDragged(int x, int y, int p) {
        if (draggedCard == null) return false;
        Ray ray = view.getCam().getPickRay(x, y);
        view.updateDragPosition(ray);
        return true;
    }

    @Override public boolean keyDown(int k)                          { return false; }
    @Override public boolean keyUp(int k)                           { return false; }
    @Override public boolean keyTyped(char c)                       { return false; }
    @Override public boolean scrolled(float x, float y)             { return false; }
    @Override public boolean touchCancelled(int x, int y, int p, int b) { return false; }
}
