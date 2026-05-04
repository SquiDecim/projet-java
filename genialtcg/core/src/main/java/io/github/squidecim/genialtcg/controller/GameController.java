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

    private boolean initialDrawDone = false;
    private int initialDrawCount = 0;
    private static final int INITIAL_HAND_SIZE = 6;

    public GameController(GameView view, GameModel model, GameClient client, String myPlayerId, GenialTCG game) {
        this.view = view;
        this.model = model;
        this.client = client;
        this.myPlayerId = myPlayerId;
        this.game = game;
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
        CardDecal card = view.getHoveredCard(ray);

        if (b == 0 && model.phase == GameModel.Phase.PLAYING && model.myTurn) {
            CardDecal myTable = view.getMyTableCard();
            if (myTable != null && myTable.intersects(ray)) {
                view.showAttackMenu(myTable.getData());
                return true;
            }
        }

        if (b == 1) {
            if (card != null) {
                view.showZoom(card);
                return true;
            }
        }

        if (model.phase == GameModel.Phase.PLAYING && !model.myTurn && b == 0) {
            return false;
        }

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

        if (draggedCard != null && slot == null) {
            view.cancelDrag(draggedCard);
            draggedCard = null;
            return true;
        }

        if (draggedCard.getData().id.startsWith("ACT-") || draggedCard.getData().id.startsWith("OUT-")) {
            view.cancelDrag(draggedCard);
            return true;
        }
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
                        if (fromTable) {
                            model.moveFromTableToBench(draggedCard.getData());
                            if (model.phase == GameModel.Phase.DRAW) view.showBanner();
                        } else {
                            model.moveFromHandToBench(draggedCard.getData());
                        }
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
                model.setupDone = true;
                view.hideBanner();
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
        if (!checkCondition(card, isMe)) return;

        for (int i = 0; i < card.specialCibles.length; i++) {

            String cible    = card.specialCibles[i];
            String variable = card.specialVariables[i];
            Object valeur   = card.specialValeurs[i];

            boolean targetIsMe = false;

            if (!(valeur instanceof Integer)) continue;
            int val = (int) valeur;

            if (!"etat".equals(variable)) continue;

            CardDecal target = null;

            switch (cible) {
                case "jeu":
                    target = isMe ? view.getMyTableCard() : view.getOpponentTableCard();
                    targetIsMe = isMe;
                    break;
                case "jeuA":
                    target = isMe ? view.getOpponentTableCard() : view.getMyTableCard();
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

    private boolean checkCondition(CardData card, boolean isMe) {
        boolean hasAny = card.condTypes != null || card.condTerrains != null
            || card.condRangs != null || card.condEtatMin != 0
            || card.condEtatMax != 0 || card.condStatMinKey != null
            || card.condStatMaxKey != null;
        if (!hasAny) return true;

        CardDecal activeDecal = isMe ? view.getMyTableCard() : view.getOpponentTableCard();
        if (activeDecal == null) return false;
        CardData active = activeDecal.getData();

        // type
        if (card.condTypes != null) {
            boolean ok = false;
            for (String t : card.condTypes) if (t.equals(active.type)) { ok = true; break; }
            if (!ok) return false;
        }

        // rang
        if (card.condRangs != null) {
            boolean ok = false;
            for (String r : card.condRangs) if (r.equals(active.rank)) { ok = true; break; }
            if (!ok) return false;
        }

        // etatMin / etatMax
        if (card.condEtatMin != 0 && active.pv < card.condEtatMin) return false;
        if (card.condEtatMax != 0 && active.pv > card.condEtatMax) return false;

        // statMin / statMax
        if (card.condStatMinKey != null && getStat(active, card.condStatMinKey) < card.condStatMinVal) return false;
        if (card.condStatMaxKey != null && getStat(active, card.condStatMaxKey) > card.condStatMaxVal) return false;

        // terrain : nécessite que le GameModel expose le terrain actif (à implémenter)
        // if (card.condTerrains != null) { ... model.getCurrentTerrain() ... }

        return true;
    }

    private int getStat(CardData card, String statName) {
        if (card.stats == null) return 0;
        switch (statName) {
            case "puissance":   return card.stats.length > 0 ? card.stats[0] : 0;
            case "economie":    return card.stats.length > 1 ? card.stats[1] : 0;
            case "ressources":  return card.stats.length > 2 ? card.stats[2] : 0;
            case "technologie": return card.stats.length > 3 ? card.stats[3] : 0;
            case "stabilite":   return card.stats.length > 4 ? card.stats[4] : 0;
            default: return 0;
        }
    }

    private void handleDeath(CardDecal card, boolean isMe) {

        Vector3 targetPos = isMe
            ? view.getMyDiscardPosition()
            : view.getOpponentDiscardPosition();

        card.animateTo(targetPos, 0, -90f, 0, 0.5f);

    }

    public void startInitialDraw() {
        for (int i = 0; i < INITIAL_HAND_SIZE; i++) {
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    Gdx.app.postRunnable(() -> client.sendDrawCard());
                }
            }, (i + 2) * drawCooldown);
        }
        com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
            @Override
            public void run() {
                Gdx.app.postRunnable(() -> {
                    initialDrawDone = true;
                    model.phase = GameModel.Phase.SETUP;
                    view.showBanner();
                    view.showActionButton("Commencer", () -> {
                        if (view.getMyTableCard() == null) return;
                        view.hideActionButton();
                        view.hideBanner();
                        view.startClicked = true;
                        model.setupDone = true;
                        client.sendReady();
                    });
                });
            }
        }, (INITIAL_HAND_SIZE + 2) * drawCooldown);
    }

    @Override
    public void onTurnChanged(NetworkMessages.TurnChanged msg) {
        boolean myTurn = msg.currentPlayerId.equals(myPlayerId);
        model.phase = GameModel.Phase.PLAYING;
        model.myTurn = myTurn;
        if (myTurn) {
            view.showActionButton("Finir le tour", () -> {
                view.hideActionButton();
                model.myTurn = false;
                client.sendEndTurn();
            });
        } else {
            view.hideActionButton();
        }
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
    public void onPlayerJoined(NetworkMessages.PlayerJoined msg) {}

    @Override
    public void onLobbyInfo(NetworkMessages.LobbyInfo msg) {}

    @Override
    public void onCreditsUpdate(NetworkMessages.CreditsUpdate obj) {

    }
}
