package io.github.squidecim.genialtcg.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.mainMenu.FirstScreen;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.GameModel;
import io.github.squidecim.genialtcg.network.GameClient;
import io.github.squidecim.genialtcg.network.NetworkMessages;
import io.github.squidecim.genialtcg.view.CardDecal;
import io.github.squidecim.genialtcg.view.CardSlot;
import io.github.squidecim.genialtcg.view.GameView;

public class GameController implements InputProcessor, GameClient.NetworkListener {

    private final GenialTCG game;
    private final GameView view;
    private final GameModel model;
    private GameClient client;

    private String myPlayerId;

    private boolean canDraw = true;
    private float drawCooldown = 0.75f;
    private float drawTimer = 0f;

    private CardDecal draggedCard = null;

    public GameController(GameView view, GameModel model, GameClient client, String myPlayerId, GenialTCG game) {
        this.view = view;
        this.model = model;
        this.client = client;
        this.myPlayerId = myPlayerId;
        this.game = game;
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
        if (view.isZooming()) return false;
        Ray ray = view.getCam().getPickRay(screenX, screenY);
        view.updateHover(ray);
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int p, int b) {

        if (view.isZooming()) {
            view.hideZoom();
            return true;
        }

        Ray ray = view.getCam().getPickRay(x, y);

        if (b == 1) {
            CardDecal card = view.getHoveredCard(ray);
            if (card != null) {
                view.showZoom(card);
                return true;
            }
        }

        CardDecal card = view.getHoveredCard(ray);
        if (card != null) {
            if (view.isOpponentCard(card)) return false;
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
        if (draggedCard.getData().id.startsWith("ACT-") || draggedCard.getData().id.startsWith("OUT-")) view.cancelDrag(draggedCard);
        else if (slot != null) {
            boolean fromBench = draggedCard.emplacement.equals("bench");
            boolean fromTable = draggedCard.emplacement.equals("table");
            boolean toBench = slot.type.equals("bench");
            boolean toTable = slot.type.equals("table");

            if ((fromBench && toBench) || (fromTable && toTable)) {
                view.cancelDrag(draggedCard);
                draggedCard = null;
                return true;
            }

            if (toBench) {
                if (model.isBenchFull()) {
                    view.cancelDrag(draggedCard);
                } else {
                    CardSlot firstSlot = view.getFirstEmptyBenchSlot();
                    if (firstSlot != null) {
                        if (fromTable) model.moveFromTableToBench(draggedCard.getData());
                        else model.moveFromHandToBench(draggedCard.getData());
                        view.dropCardOnSlot(draggedCard, firstSlot);
                        int slotIdx = view.getBenchSlotIndex(firstSlot);
                        client.sendPlayCard(draggedCard.getData().getAtlasRegionName(), "bench", slotIdx);
                    } else {
                        view.cancelDrag(draggedCard);
                    }
                }
            } else if (toTable && slot.isEmpty()) {
                if (fromBench) model.moveFromBenchToTable(draggedCard.getData());
                else model.moveFromHandToTable(draggedCard.getData());
                view.dropCardOnSlot(draggedCard, slot);
                applyCardEffect(draggedCard.getData(), true);
                client.sendPlayCard(draggedCard.getData().getAtlasRegionName(), "table", 0);
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
        if (k == Input.Keys.ESCAPE && view.isZooming()) {
            view.hideZoom();
            return true;
        }
        return false;
    }
    @Override public boolean keyUp(int k) { return false; }
    @Override public boolean keyTyped(char c) { return false; }
    @Override public boolean scrolled(float x, float y) { return false; }
    @Override public boolean touchCancelled(int x, int y, int p, int b) { return false; }


    private void applyCardEffect(CardData card, boolean isMe) {

        for (int i = 0; i < card.specialCibles.length; i++) {

            String cible = card.specialCibles[i];
            String variable = card.specialVariables[i];
            Object valeur = card.specialValeurs[i];

            boolean targetIsMe = false;

            if (!(valeur instanceof Integer)) continue;
            int val = (int) valeur;

            if (!"etat".equals(variable)) continue;

            CardDecal target = null;

            switch (cible) {
                case "jeu":
                    target = isMe
                        ? view.getMyTableCard()
                        : view.getOpponentTableCard();
                    targetIsMe = isMe;
                    break;

                case "jeuA":
                    target = isMe
                        ? view.getOpponentTableCard()
                        : view.getMyTableCard();
                    targetIsMe = !isMe;
                    break;
            }

            if (target != null) {
                model.applyDamage(target, val);
                if (target.getData().pv <= 0) {
                    handleDeath(target, targetIsMe);
                }
            }

        }
    }

    private void handleDeath(CardDecal card, boolean isMe) {

        Vector3 targetPos = isMe
            ? view.getMyDiscardPosition()
            : view.getOpponentDiscardPosition();

        card.animateTo(targetPos, 0, -90f, 0, 0.5f);

    }

    @Override
    public void onAssignId(NetworkMessages.AssignId msg) {}

    @Override
    public void onGameStart(NetworkMessages.GameStart msg) {}

    @Override
    public void onCardDrawn(NetworkMessages.CardDrawn msg) {

        if (msg.playerId.equals(myPlayerId)) {
            CardData drawn = model.drawCard();
            if (drawn != null) {
                view.addCardToHand(drawn);
                view.updateDeckVisual(model.deckSize());
            }
            if (model.isDeckEmpty()) {
                Gdx.app.postRunnable(() ->
                    game.setScreen(new FirstScreen(game, "Votre deck est vide — vous avez perdu !"))
                );
            }
        } else {
            view.updateOpponentDeckVisual(msg.newDeckSize);
            if (msg.newDeckSize == 0) {
                Gdx.app.postRunnable(() ->
                    game.setScreen(new FirstScreen(game, "Le deck adverse est vide — vous avez gagné !"))
                );
            }
        }
    }


    @Override
    public void onCardPlayed(NetworkMessages.CardPlayed msg) {

        boolean isMe = msg.playerId.equals(myPlayerId);
        if (isMe) return;
        CardData card = model.lookupCard(msg.cardId);
        if (card == null) return;
        if ("bench".equals(msg.zone)) {
            view.addOpponentCardToBench(card);
        } else if ("table".equals(msg.zone)) {
            view.addOpponentCardToTable(card);
            applyCardEffect(card, isMe);
        }
    }

    @Override
    public void onTurnChanged(NetworkMessages.TurnChanged msg) {
        boolean myTurn = msg.currentPlayerId != null && msg.currentPlayerId.equals(myPlayerId);
        Gdx.app.log("Tour", myTurn ? "C'est votre tour" : "Tour de l'adversaire");
    }

    @Override
    public void onPlayerJoined(NetworkMessages.PlayerJoined msg) {}

    @Override
    public void onLobbyInfo(NetworkMessages.LobbyInfo msg) {}

    @Override
    public void onCreditsUpdate(NetworkMessages.CreditsUpdate obj) {

    }
}
