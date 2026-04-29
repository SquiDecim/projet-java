package io.github.squidecim.genialtcg.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.collision.Ray;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.GameModel;
import io.github.squidecim.genialtcg.network.GameClient;
import io.github.squidecim.genialtcg.network.NetworkMessages;
import io.github.squidecim.genialtcg.view.CardDecal;
import io.github.squidecim.genialtcg.view.CardSlot;
import io.github.squidecim.genialtcg.view.GameView;

public class GameController implements InputProcessor, GameClient.NetworkListener {

    private final GameView view;
    private final GameModel model;
    private GameClient client;

    private String myPlayerId;

    private boolean canDraw = true;
    private float drawCooldown = 0.75f;
    private float drawTimer = 0f;

    private CardDecal draggedCard = null;

    public GameController(GameView view, GameModel model, GameClient client, String myPlayerId) {
        this.view = view;
        this.model = model;
        this.client = client;
        this.myPlayerId = myPlayerId;
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
            view.startDrag(draggedCard, ray);
            return true;
        }

        if (view.isDeckClicked(ray)) {
            if (!canDraw) return false;
            client.sendDrawCard();
            canDraw = false;
        }
        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int p, int b) {
        if (draggedCard == null) return false;
        Ray ray = view.getCam().getPickRay(x, y);

        CardSlot slot = view.getIntersectedSlot(ray);
        if (slot != null) {
            boolean fromBench = draggedCard.emplacement.equals("bench");
            boolean toBench = slot.type.equals("bench");
            boolean toTable = slot.type.equals("table");
            if (fromBench && toBench) {
                view.cancelDrag(draggedCard);
                draggedCard = null;
                return true;
            }

            if (toBench) {
                CardSlot firstSlot = view.getFirstEmptyBenchSlot();
                if (firstSlot != null) {
                    model.moveFromHandToBench(draggedCard.getData());
                    view.dropCardOnSlot(draggedCard, firstSlot);
                } else {
                    view.cancelDrag(draggedCard);
                }
            } else if (toTable && slot.isEmpty()) {
                model.moveFromHandToTable(draggedCard.getData());
                view.dropCardOnSlot(draggedCard, slot);
            } else {
                view.cancelDrag(draggedCard);
            }
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

    @Override
    public boolean keyDown(int k) {
        return false;
    }

    @Override
    public boolean keyUp(int k) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean scrolled(float x, float y) {
        return false;
    }

    @Override
    public boolean touchCancelled(int x, int y, int p, int b) {
        return false;
    }

    @Override
    public void onAssignId(NetworkMessages.AssignId msg) {

    }

    @Override
    public void onGameStart(NetworkMessages.GameStart msg) {

    }

    @Override
    public void onCardDrawn(NetworkMessages.CardDrawn msg) {
        if (msg.playerId.equals(myPlayerId)) {
            CardData drawn = model.drawCard();
            if (drawn != null) {
                view.addCardToHand(drawn);
                view.updateDeckVisual(model.deckSize());
            }
        } else {
            view.updateOpponentDeckVisual(msg.newDeckSize);
        }
    }

    @Override
    public void onCardPlayed(NetworkMessages.CardPlayed msg) {

    }

    @Override
    public void onTurnChanged(NetworkMessages.TurnChanged msg) {

    }

    @Override
    public void onPlayerJoined(NetworkMessages.PlayerJoined msg) {

    }

    @Override
    public void onLobbyInfo(NetworkMessages.LobbyInfo msg) {

    }
}
