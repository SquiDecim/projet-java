package io.github.squidecim.genialtcg.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.mainMenu.MainScreen;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.GameModel;
import io.github.squidecim.genialtcg.network.GameClient;
import io.github.squidecim.genialtcg.network.NetworkMessages;
import io.github.squidecim.genialtcg.view.CardDecal;
import io.github.squidecim.genialtcg.view.CardSlot;
import io.github.squidecim.genialtcg.view.GameView;
import java.util.Arrays;
import java.util.Random;

public class GameController
    implements InputProcessor, GameClient.NetworkListener
{

    private final GenialTCG game;
    private final GameView view;
    private final GameModel model;
    private GameClient client;

    private String myPlayerId;
    private String firstPlayerId = null;

    private boolean canDraw = true;
    private float drawCooldown = 0.75f;
    private float drawTimer = 0f;

    private CardDecal draggedCard = null;
    private boolean selectingRetreat = false;
    private boolean selectingReplacementAfterDeath = false;

    private boolean selectingBenchTarget = false;
    private boolean benchTargetIsOpponent = false;
    private java.util.function.Consumer<CardDecal> onBenchTargetSelected = null;

    private boolean initialDrawDone = false;
    private int initialDrawCount = 0;
    private static final int INITIAL_HAND_SIZE = 6;
    private boolean startTurnWithDiedCard = false;
    public CardDecal actionCardPlayed;

    public GameController(
        GameView view,
        GameModel model,
        GameClient client,
        String myPlayerId,
        GenialTCG game
    ) {
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
        if (view.isPauseMenuVisible()) return false;
        if (view.isZooming()) return false;
        if (view.isAttackMenuVisible()) return false;
        if (
            model.phase == GameModel.Phase.DRAW || view.isAnyCardBeingDrawn()
        ) return false;
        Ray ray = view.getCam().getPickRay(screenX, screenY);
        view.updateHover(ray);
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int p, int b) {
        if (view.isPauseMenuVisible()) return false;
        if (view.isCamAnimating()) return false;

        if (view.isZooming()) {
            Ray ray = view.getCam().getPickRay(x, y);
            if (!view.isZoomCardHit(ray)) view.hideZoom();
            return true;
        }

        if (model.phase == GameModel.Phase.DRAW || view.isAnyCardBeingDrawn()) {
            return false;
        }

        if (view.isAttackMenuVisible()) {
            view.hideAttackMenu();
            return true;
        }

        Ray ray = view.getCam().getPickRay(x, y);
        CardDecal card = view.getHoveredCard(ray);

        if (selectingBenchTarget && b == 0) {
            CardDecal benchCard = benchTargetIsOpponent
                ? view.getOpponentBenchCardAt(ray)
                : view.getBenchCardAt(ray);
            if (benchCard != null) {
                selectingBenchTarget = false;
                benchTargetIsOpponent = false;
                view.clearAllSelectableBorders();
                view.hideBanner();
                if (onBenchTargetSelected != null) {
                    onBenchTargetSelected.accept(benchCard);
                    onBenchTargetSelected = null;
                }
            }
            return true;
        }

        if (selectingRetreat && b == 0) {
            CardDecal benchCard = view.getBenchCardAt(ray);
            if (benchCard != null) {
                executeRetreat(benchCard);
            } else {
                selectingRetreat = false;
                view.setSelectableBorder(false);
            }
            return true;
        }

        if (selectingReplacementAfterDeath && b == 0) {
            CardDecal benchCard = view.getBenchCardAt(ray);
            if (benchCard != null) {
                selectingReplacementAfterDeath = false;
                view.setSelectableBorder(false);
                view.hideBanner();
                if (game.switchSound != null) game.switchSound.play(
                    game.uiSoundVolume
                );
                model.moveFromBenchToTable(benchCard.getData());
                view.promoteFromBenchToTable(benchCard);
                client.sendPlayCard(
                    benchCard.getData().getAtlasRegionName(),
                    "table",
                    0
                );

                if (!startTurnWithDiedCard) {
                    client.sendEndTurn();
                } else {
                    view.showActionButton("Finir le tour", () -> {
                        startTurnWithDiedCard = false;
                        view.hideActionButton();
                        model.myTurn = false;
                        client.sendEndTurn();
                    });
                }
            }
            return true;
        }

        if (b == 0 && model.phase == GameModel.Phase.PLAYING && model.myTurn) {
            CardDecal myTable = view.getMyTableCard();
            if (myTable != null && myTable.intersects(ray)) {
                view.showAttackMenu(myTable.getData(), client, this);
                return true;
            }
        }

        if (b == 1) {
            if (card != null) {
                view.showZoom(card);
                return true;
            } else if (view.isDiscardClicked(ray)) {
                view.showZoom(view.getMyDiscard());
                return true;
            } else if (view.isOpponentDiscardClicked(ray)) {
                view.showZoom(view.getOpponentDiscard());
                return true;
            }
        }

        if (b == 3) return false;
        if (model.phase == GameModel.Phase.PLAYING && !model.myTurn && b == 0) {
            if (view.isDeckClicked(ray)) {
                view.showEphemeralMessage("Ce n'est pas votre tour !");
                return true;
            }
        }

        if (card != null) {
            if (view.isOpponentCard(card)) return false;
            if ("table".equals(card.emplacement)) return false;
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
        draggedCard.setDragging(false);

        if (model.phase == GameModel.Phase.PLAYING && !model.myTurn) {
            view.cancelDrag(draggedCard);
            view.showEphemeralMessage("Ce n'est pas votre tour !");
            draggedCard = null;
            return true;
        }

        Ray ray = view.getCam().getPickRay(x, y);

        CardSlot slot = view.getIntersectedSlot(ray);

        if (draggedCard != null && slot == null) {
            view.cancelDrag(draggedCard);
            draggedCard = null;
            return true;
        }

        assert draggedCard != null;
        if (draggedCard.getData().id.startsWith("OUT-")) {
            view.cancelDrag(draggedCard);
            return true;
        } else if (slot != null) {
            boolean fromBench = draggedCard.emplacement.equals("bench");
            boolean toBench = slot.type.equals("bench");
            boolean toTable = slot.type.equals("table");
            boolean toAction = slot.type.equals("action");

            if (fromBench && toBench) {
                view.cancelDrag(draggedCard);
                draggedCard = null;
                return true;
            }
            if (draggedCard.getData().id.startsWith("ACT-")) {
                if (
                    toAction &&
                    slot.isEmpty() &&
                    model.phase == GameModel.Phase.PLAYING
                ) {
                    if (conditionsRespected(draggedCard)) {
                        model.useFromHand(draggedCard.getData());
                        view.dropCardOnSlot(draggedCard, slot);

                        boolean hasSoinBanc = false;
                        for (String t : draggedCard.getData().specialEffectTypes) {
                            if ("soinBanc".equals(t)) {
                                hasSoinBanc = true;
                                break;
                            }
                        }
                        if (!hasSoinBanc) {
                            client.sendPlayCard(
                                draggedCard.getData().getAtlasRegionName(),
                                "action",
                                0
                            );
                        }

                        final CardDecal played = draggedCard;
                        com.badlogic.gdx.utils.Timer.schedule(
                            new com.badlogic.gdx.utils.Timer.Task() {
                                @Override
                                public void run() {
                                    executeAction(view.getActionCard(), true);
                                }
                            },
                            0.5f
                        );
                    } else {
                        Gdx.app.log(
                            "GameController",
                            "Conditions de la carte action non respectées"
                        );
                        view.cancelDrag(draggedCard);
                        draggedCard = null;
                        view.showEphemeralMessage(
                            "Les conditions de la carte ne sont pas respectées"
                        );
                        return true;
                    }
                } else {
                    view.cancelDrag(draggedCard);
                    draggedCard = null;
                    return true;
                }
            } else if (toBench) {
                if (model.isBenchFull()) {
                    view.cancelDrag(draggedCard);
                } else {
                    CardSlot firstSlot = view.getFirstEmptyBenchSlot();
                    if (firstSlot != null) {
                        if (model.myCredits >= draggedCard.getData().cost) {
                            model.moveFromHandToBench(draggedCard.getData());
                            view.dropCardOnSlot(draggedCard, firstSlot);
                            if (
                                game.posingCardsSound != null
                            ) game.posingCardsSound.play(game.uiSoundVolume);
                            int slotIdx = view.getBenchSlotIndex(firstSlot);
                            client.sendPlayCard(
                                draggedCard.getData().getAtlasRegionName(),
                                "bench",
                                slotIdx
                            );
                            client.sendCreditsUpdate(model.myCredits);
                        } else {
                            view.showEphemeralMessage("Pas assez de crédits ! (" + draggedCard.getData().cost + " requis)");
                            view.cancelDrag(draggedCard);
                            draggedCard = null;
                            return true;
                        }
                    } else {
                        view.cancelDrag(draggedCard);
                    }
                }
            } else if (toTable && slot.isEmpty()) {
                if (!fromBench) {
                    if (model.myCredits >= draggedCard.getData().cost) {
                        model.moveFromHandToTable(draggedCard.getData());
                        client.sendCreditsUpdate(model.myCredits);
                    } else {
                        view.showEphemeralMessage("Pas assez de crédits ! (" + draggedCard.getData().cost + " requis)");
                        view.cancelDrag(draggedCard);
                        draggedCard = null;
                        return true;
                    }
                } else model.moveFromBenchToTable(draggedCard.getData());
                view.dropCardOnSlot(draggedCard, slot);
                if (game.posingCardsSound != null) game.posingCardsSound.play(
                    game.uiSoundVolume
                );
                client.sendPlayCard(
                    draggedCard.getData().getAtlasRegionName(),
                    "table",
                    0
                );
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
        if (k == Input.Keys.ESCAPE) {
            view.togglePauseMenu();
            return true;
        }
        if (k == Input.Keys.TAB) {
            view.toggleChatPanel();
            return true;
        }
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

    public void startInitialDraw() {
        for (int i = 0; i < INITIAL_HAND_SIZE; i++) {
            com.badlogic.gdx.utils.Timer.schedule(
                new com.badlogic.gdx.utils.Timer.Task() {
                    @Override
                    public void run() {
                        Gdx.app.postRunnable(() -> client.sendDrawCard());
                    }
                },
                (i + 2) * (drawCooldown + 0.1f)
            );
        }
        com.badlogic.gdx.utils.Timer.schedule(
            new com.badlogic.gdx.utils.Timer.Task() {
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
            },
            (INITIAL_HAND_SIZE + 2) * (drawCooldown + 0.1f)
        );
    }

    public void startRetreat(CardData tableCard) {
        if (model.myCredits < tableCard.revocation) {
            view.showAttackMenuError(
                "Pas assez de crédits ! (il manque " + (tableCard.revocation - model.myCredits) + " crédits)"
            );
            return;
        }
        if (model.bench.isEmpty()) {
            view.showEphemeralMessage("Votre banc est vide !");
            return;
        }
        view.hideAttackMenu();
        selectingRetreat = true;
        view.setSelectableBorder(true);
    }

    private void executeRetreat(CardDecal benchCard) {
        selectingRetreat = false;
        view.setSelectableBorder(false);

        CardData tableCardData = model.table;
        model.spendCredits(tableCardData.revocation);
        if (game.switchSound != null) game.switchSound.play(game.uiSoundVolume);
        view.swapTableAndBench(benchCard);

        model.moveFromTableToBench(tableCardData);
        model.moveFromBenchToTable(benchCard.getData());

        client.sendCreditsUpdate(model.myCredits);
        client.sendRetreat(benchCard.getData().getAtlasRegionName());
    }

    private void executeAction(CardDecal cardAction, boolean isMyCard) {
        view.showToCam(cardAction, isMyCard, null);
    }

    private void executeAction(
        CardDecal cardAction,
        boolean isMyCard,
        String targetBenchCardId
    ) {
        view.showToCam(cardAction, isMyCard, targetBenchCardId);
    }

    @Override
    public void onTurnChanged(NetworkMessages.TurnChanged msg) {
        boolean myTurn = msg.currentPlayerId.equals(myPlayerId);
        model.phase = GameModel.Phase.PLAYING;
        model.myTurn = myTurn;
        if (firstPlayerId == null) {
            firstPlayerId = msg.currentPlayerId;
        } else if (msg.currentPlayerId.equals(firstPlayerId)) {
            model.turnCount++;
            view.updateTurnCount(model.turnCount);
        }
        if (myTurn) {
            model.receiveCredits(model.getTotalEconomy());
            client.sendCreditsUpdate(model.myCredits);
            client.sendDrawCard();
            view.showActionButton("Finir le tour", () -> {
                view.hideActionButton();
                model.myTurn = false;
                client.sendEndTurn();
                selectingRetreat = false;
                view.setSelectableBorder(false);
            });
        } else {
            view.hideActionButton();
        }
    }

    @Override
    public void onPlayerQuit() {
        client.disconnect();
        game.setScreen(new MainScreen(game));
    }

    @Override
    public void onField(NetworkMessages.Field msg) {
        String field = msg.field;
        model.terrain = field;
        view.changeField(field);
    }

    @Override
    public void onAssignId(NetworkMessages.AssignId msg) {}

    @Override
    public void onGameStart(NetworkMessages.GameStart msg) {}

    @Override
    public void onChatMessage(NetworkMessages.ChatMessage msg) {
        boolean isMe = msg.senderId != null && msg.senderId.equals(myPlayerId);
        String name = msg.senderName != null ? msg.senderName : "?";
        String text = msg.text != null ? msg.text : "";
        view.showChatMessage(name, text, isMe);
    }

    @Override
    public void onCardDrawn(NetworkMessages.CardDrawn msg) {
        if (msg.playerId.equals(myPlayerId)) {
            CardData drawn = model.drawCard();
            if (drawn != null) {
                view.addCardToHand(drawn);
                view.updateDeckVisual(model.deckSize());
                if (game.takingCardsSound != null) game.takingCardsSound.play(
                    game.uiSoundVolume
                );
            }
            if (model.isDeckEmpty()) {
                Gdx.app.postRunnable(() ->
                    game.setScreen(
                        new MainScreen(
                            game,
                            "Votre deck est vide — vous avez perdu !"
                        )
                    )
                );
            }
        } else {
            view.updateOpponentDeckVisual(msg.newDeckSize);
            if (msg.newDeckSize == 0) {
                Gdx.app.postRunnable(() ->
                    game.setScreen(
                        new MainScreen(
                            game,
                            "Le deck adverse est vide — vous avez gagné !"
                        )
                    )
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
        } else if ("action".equals(msg.zone)) {
            view.addOpponentCardToAction(card);
            final String targetId = msg.targetBenchCardId;
            com.badlogic.gdx.utils.Timer.schedule(
                new com.badlogic.gdx.utils.Timer.Task() {
                    @Override
                    public void run() {
                        executeAction(view.getActionCard(), false, targetId);
                    }
                },
                0.5f
            );
        }
    }

    @Override
    public void onPlayerJoined(NetworkMessages.PlayerJoined msg) {}

    @Override
    public void onLobbyInfo(NetworkMessages.LobbyInfo msg) {}

    @Override
    public void onCreditsUpdate(NetworkMessages.CreditsUpdate msg) {
        boolean isMe = msg.playerId.equals(myPlayerId);
        if (isMe) {
            model.myCredits = msg.credits;
            view.updateMyCredits(model.myCredits);
        } else {
            model.opponentCredits = msg.credits;
            view.updateOpponentCredits(model.opponentCredits);
        }
    }

    @Override
    public void onNormalAttack(NetworkMessages.NormalAttack msg) {
        CardDecal myTable = view.getMyTableCard();
        CardDecal oppTable = view.getOpponentTableCard();
        if (myTable == null || oppTable == null) return;

        Sound[] damageSounds = {
            game.damagePuissanceSound,
            game.damageRessourceSound,
            game.damageTechnologieSound,
            game.damageStabiliteSound,
        };
        if (msg.statIndex >= 0 && msg.statIndex < damageSounds.length) {
            Sound s = damageSounds[msg.statIndex];
            if (s != null) s.play(game.uiSoundVolume);
        }

        boolean iAmAttacker = model.myTurn;
        if (msg.damage > 0) {
            CardDecal target = iAmAttacker ? oppTable : myTable;
            applyDamageAndFloat(target, -msg.damage, iAmAttacker);
        } else if (msg.damage < 0) {
            CardDecal target = iAmAttacker ? myTable : oppTable;
            applyDamageAndFloat(target, msg.damage, !iAmAttacker);
        }

        if (iAmAttacker) {
            resolveEndOfAttack();
        }
    }

    @Override
    public void onRetreat(NetworkMessages.Retreat msg) {
        boolean isMe = msg.playerId.equals(myPlayerId);
        if (isMe) return;

        CardDecal oppBenchCard = view.getOpponentBenchCardById(msg.benchCardId);
        if (oppBenchCard != null) {
            view.swapOpponentTableAndBench(oppBenchCard);
        }
    }

    @Override
    public void onCardDied(NetworkMessages.CardDied msg) {
        boolean isMe = msg.playerId.equals(myPlayerId);
        boolean iMustReplace =
            (isMe && !msg.isOpponent) || (!isMe && msg.isOpponent);

        if (!model.myTurn && iMustReplace) startTurnWithDiedCard = true;

        CardDecal deadCard;
        if (!iMustReplace) {
            deadCard = "table".equals(msg.zone)
                ? view.getOpponentTableCard()
                : view.getOpponentBenchCardById(msg.cardId);
        } else {
            Gdx.app.log(
                "GameController",
                "Remplacement requis — zone : " + msg.zone
            );
            deadCard = "table".equals(msg.zone)
                ? view.getMyTableCard()
                : view.getMyBenchCardById(msg.cardId);
        }
        Gdx.app.log("GameController", "Carte morte : " + deadCard);
        if (deadCard == null) return;

        view.sendToDiscard(deadCard, iMustReplace);

        if (msg.isOpponent == isMe) {
            if ("table".equals(msg.zone)) view.clearTableSlot(false);
            else view.clearOpponentBenchSlot(deadCard);
        } else {
            model.discardCard(deadCard.getData());
            if ("table".equals(msg.zone)) view.clearTableSlot(true);
            else view.clearBenchSlot(deadCard);
        }

        com.badlogic.gdx.utils.Timer.schedule(
            new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    Gdx.app.postRunnable(() -> {
                        if (
                            iMustReplace &&
                            "table".equals(msg.zone) &&
                            !model.bench.isEmpty()
                        ) {
                            view.showBanner(
                                "Choisissez une carte du banc à remettre en jeu"
                            );
                            view.setSelectableBorder(true);
                            selectingReplacementAfterDeath = true;
                        }
                    });
                }
            },
            1.1f
        );
    }

    private void applyDamageAndFloat(
        CardDecal target,
        int damage,
        boolean onOpponent
    ) {
        boolean died = model.applyDamage(target, damage);
        Vector3 pos = target.getPosition().cpy();
        pos.y += 0.5f;
        view.spawnFloatingText("-" + Math.abs(damage), pos, Color.RED);
        if (died && model.myTurn) {
            client.sendCardDied(
                target.getData().getAtlasRegionName(),
                target.emplacement,
                onOpponent
            );
        }
    }

    public void handleSpecialAttack(CardData card) {
        int attackCost = card.specialCost;
        if (model.myCredits < attackCost) {
            view.showAttackMenuError(
                "Pas assez de crédits ! (il manque " + (attackCost - model.myCredits) + " crédits)"
            );
            return;
        }

        String[] types = card.specialEffectTypes;
        int[] values = card.specialEffectValues;
        if (types == null || types.length == 0) {
            view.hideAttackMenu();
            return;
        }

        model.spendCredits(attackCost);
        view.updateMyCredits(model.myCredits);
        view.hideAttackMenu();

        boolean needsBenchChoice = false;
        for (String type : types) {
            if ("degatBanc".equals(type) || "soinBanc".equals(type)) {
                needsBenchChoice = true;
                break;
            }
        }

        if (needsBenchChoice) {
            boolean targetingOwnBench = false;
            for (String type : types) {
                if ("soinBanc".equals(type)) {
                    targetingOwnBench = true;
                    break;
                }
            }
            benchTargetIsOpponent = !targetingOwnBench;

            boolean benchIsEmpty = benchTargetIsOpponent
                ? view.getFirstOpponentBenchCard() == null
                : view.getFirstMyBenchCard() == null;

            if (benchIsEmpty) {
                client.sendSpecialAttack(types, values, model.deckSize(), null);
                return;
            }
            if (targetingOwnBench) {
                view.setSelectableBorderForOwnBench(true);
            } else {
                view.setSelectableBorderForOpponentBench(true);
            }

            view.showBanner(
                targetingOwnBench
                    ? "Choisissez une carte de votre banc à soigner"
                    : "Choisissez une carte du banc adverse à attaquer"
            );
            selectingBenchTarget = true;

            onBenchTargetSelected = chosenCard -> {
                view.restoreCam(() ->
                    client.sendSpecialAttack(
                        types,
                        values,
                        model.deckSize(),
                        chosenCard.getData().getAtlasRegionName()
                    )
                );
            };

            if (benchTargetIsOpponent) {
                view.moveCamToOpponentBench(null);
            }
        } else {
            client.sendSpecialAttack(types, values, model.deckSize(), null);
        }
    }

    @Override
    public void onSpecialAttack(NetworkMessages.SpecialAttack msg) {
        if (game.specialEffectSound != null) game.specialEffectSound.play(
            game.uiSoundVolume
        );

        boolean iAmAttacker = model.myTurn;

        CardDecal myTable = view.getMyTableCard();
        CardDecal oppTable = view.getOpponentTableCard();

        for (int i = 0; i < msg.effectTypes.length; i++) {
            String type = msg.effectTypes[i];
            int value = msg.effectValues[i];
            switch (type) {
                case "degatAdverse": {
                    CardDecal target = iAmAttacker ? oppTable : myTable;
                    if (target != null) applyDamageAndFloat(
                        target,
                        -value,
                        iAmAttacker
                    );
                    break;
                }
                case "degatBanc": {
                    CardDecal target;
                    if (iAmAttacker) {
                        target =
                            msg.targetBenchCardId != null
                                ? view.getOpponentBenchCardById(
                                      msg.targetBenchCardId
                                  )
                                : view.getFirstOpponentBenchCard();
                    } else {
                        target =
                            msg.targetBenchCardId != null
                                ? view.getMyBenchCardById(msg.targetBenchCardId)
                                : view.getFirstMyBenchCard();
                    }
                    if (target != null) applyDamageAndFloat(
                        target,
                        -value,
                        iAmAttacker
                    );
                    break;
                }
                case "soinJeu": {
                    CardDecal target = iAmAttacker ? myTable : oppTable;
                    if (target != null) {
                        model.applyDamage(target, value);
                        Vector3 pos = target.getPosition().cpy();
                        pos.y += 0.5f;
                        view.spawnFloatingText("+" + value, pos, Color.GREEN);
                    }
                    break;
                }
                case "soinBanc": {
                    CardDecal target;
                    if (iAmAttacker) {
                        target =
                            msg.targetBenchCardId != null
                                ? view.getMyBenchCardById(msg.targetBenchCardId)
                                : view.getFirstMyBenchCard();
                    } else {
                        target =
                            msg.targetBenchCardId != null
                                ? view.getOpponentBenchCardById(
                                      msg.targetBenchCardId
                                  )
                                : view.getFirstOpponentBenchCard();
                    }
                    if (target != null) {
                        model.applyDamage(target, value);
                        Vector3 pos = target.getPosition().cpy();
                        pos.y += 0.5f;
                        view.spawnFloatingText("+" + value, pos, Color.GREEN);
                    }
                    break;
                }
                case "voleCredit": {
                    if (iAmAttacker) {
                        model.receiveCredits(value);
                    } else model.spendCredits(value);
                    break;
                }
                case "pioche": {
                    if (iAmAttacker) {
                        for (int j = 0; j < value; j++) {
                            CardData drawn = model.drawCard();
                            if (drawn != null) {
                                view.addCardToHand(drawn);
                                view.updateDeckVisual(model.deckSize());
                                if (
                                    game.takingCardsSound != null
                                ) game.takingCardsSound.play(
                                    game.uiSoundVolume
                                );
                            }
                        }
                    } else {
                        for (
                            int j = 0;
                            j < value;
                            j++
                        ) view.updateOpponentDeckVisual(
                            msg.newDeckSize + value - 1 - j
                        );
                    }
                    break;
                }
            }
        }

        client.sendCreditsUpdate(model.myCredits);
        view.updateMyCredits(model.myCredits);
        view.updateOpponentCredits(model.opponentCredits);

        if (iAmAttacker) {
            resolveEndOfAttack();
        }
    }

    public void resolveEndOfAttack() {
        CardDecal myTable = view.getMyTableCard();
        CardDecal opponentTable = view.getOpponentTableCard();
        boolean myCardDead = myTable == null || myTable.getData().pv <= 0;
        boolean opponentCardDead =
            opponentTable == null || opponentTable.getData().pv <= 0;

        if (myCardDead && !model.bench.isEmpty()) {
            view.hideActionButton();
            view.showBanner("Choisissez une carte du banc à remettre en jeu");
            view.setSelectableBorder(true);
            selectingReplacementAfterDeath = true;
        } else {
            client.sendEndTurn();
        }
    }

    public boolean conditionsRespected(CardDecal card) {
        CardData cardData = card.getData();

        String[] condTypes = cardData.condTypes;
        String[] condTerrains = cardData.condTerrains;
        String[] condRangs = cardData.condRangs;
        int condEtatMin = cardData.condEtatMin;
        int condEtatMax = cardData.condEtatMax;
        String condStatMinKey = cardData.condStatMinKey;
        int condStatMinVal = cardData.condStatMinVal;

        if (
            condTerrains != null &&
            !Arrays.asList(condTerrains).contains(model.terrain)
        ) return false;

        CardDecal tableDecal = view.getMyTableCard();
        if (tableDecal == null) {
            return (
                condTypes == null &&
                condRangs == null &&
                condEtatMin == 0 &&
                condEtatMax == 0 &&
                condStatMinKey == null
            );
        }
        CardData cardInTable = tableDecal.getData();

        if (
            condTypes != null &&
            !Arrays.asList(condTypes).contains(cardInTable.type)
        ) return false;
        if (
            condRangs != null &&
            !Arrays.asList(condRangs).contains(cardInTable.rank)
        ) return false;
        if (condEtatMax != 0 && cardInTable.pv > condEtatMax) return false;
        if (condEtatMin != 0 && cardInTable.pv < condEtatMin) return false;

        if (condStatMinKey != null) {
            int statValue = getStatByKey(cardInTable, condStatMinKey);
            if (statValue < condStatMinVal) return false;
        }

        return true;
    }

    private int getStatByKey(CardData card, String key) {
        switch (key) {
            case "puissance":
                return card.stats[0];
            case "economie":
                return card.stats[1];
            case "ressources":
                return card.stats[2];
            case "technologie":
                return card.stats[3];
            case "stabilite":
                return card.stats[4];
            default:
                return 0;
        }
    }

    public void handleAction(
        CardDecal cardAction,
        boolean isMyCard,
        String targetBenchCardId
    ) {
        applyActionEffects(cardAction, isMyCard, targetBenchCardId, 0);
    }

    private void applyActionEffects(
        CardDecal cardAction,
        boolean isMyCard,
        String targetBenchCardId,
        int startIndex
    ) {
        String[] types = cardAction.getData().specialEffectTypes;
        int[] values = cardAction.getData().specialEffectValues;

        for (int i = startIndex; i < types.length; i++) {
            String type = types[i];
            int value = values[i];
            final int nextIndex = i + 1;

            switch (type) {
                case "pioche": {
                    if (isMyCard) {
                        for (int j = 0; j < value; j++) {
                            CardData drawn = model.drawCard();
                            if (drawn != null) {
                                view.addCardToHand(drawn);
                                view.updateDeckVisual(model.deckSize());
                                if (
                                    game.takingCardsSound != null
                                ) game.takingCardsSound.play(
                                    game.uiSoundVolume
                                );
                            }
                        }
                    }
                    break;
                }
                case "soinJeu": {
                    CardDecal target = isMyCard
                        ? view.getMyTableCard()
                        : view.getOpponentTableCard();
                    if (target != null) {
                        model.applyDamage(target, value);
                        Vector3 pos = target.getPosition().cpy();
                        pos.y += 0.5f;
                        view.spawnFloatingText("+" + value, pos, Color.GREEN);
                    }
                    break;
                }
                case "soinBanc": {
                    if (!isMyCard) {
                        CardDecal target =
                            targetBenchCardId != null
                                ? view.getMyBenchCardById(targetBenchCardId)
                                : view.getFirstMyBenchCard();
                        if (target != null) {
                            model.applyDamage(target, value);
                            Vector3 pos = target.getPosition().cpy();
                            pos.y += 0.5f;
                            view.spawnFloatingText(
                                "+" + value,
                                pos,
                                Color.GREEN
                            );
                        }
                        break;
                    }
                    if (model.bench.isEmpty()) break;
                    view.setSelectableBorderForOwnBench(true);
                    view.showBanner(
                        "Choisissez une carte de votre banc à soigner"
                    );
                    selectingBenchTarget = true;
                    benchTargetIsOpponent = false;
                    onBenchTargetSelected = chosenCard -> {
                        model.applyDamage(chosenCard, value);
                        Vector3 pos = chosenCard.getPosition().cpy();
                        pos.y += 0.5f;
                        view.spawnFloatingText("+" + value, pos, Color.GREEN);
                        // On envoie ICI avec la cible connue
                        client.sendPlayCardWithTarget(
                            cardAction.getData().getAtlasRegionName(),
                            "action",
                            0,
                            chosenCard.getData().getAtlasRegionName()
                        );
                        applyActionEffects(
                            cardAction,
                            isMyCard,
                            chosenCard.getData().getAtlasRegionName(),
                            nextIndex
                        );
                    };
                    return;
                }
                case "echangeBanc": {
                    if (!isMyCard) break;
                    if (model.bench.isEmpty()) {
                        view.showEphemeralMessage("Votre banc est vide !");
                        break;
                    }
                    view.setSelectableBorderForOwnBench(true);
                    view.showBanner(
                        "Choisissez une carte du banc pour l'échanger"
                    );
                    selectingBenchTarget = true;
                    benchTargetIsOpponent = false;
                    onBenchTargetSelected = chosenCard -> {
                        view.setSelectableBorder(false);
                        if (game.switchSound != null) game.switchSound.play(
                            game.uiSoundVolume
                        );
                        CardData tableCardData = model.table;
                        view.swapTableAndBench(chosenCard);
                        model.moveFromTableToBench(tableCardData);
                        model.moveFromBenchToTable(chosenCard.getData());
                        client.sendRetreat(
                            chosenCard.getData().getAtlasRegionName()
                        );
                        applyActionEffects(
                            cardAction,
                            isMyCard,
                            targetBenchCardId,
                            nextIndex
                        );
                    };
                    return;
                }
                case "echangeBancRandom": {
                    if (!isMyCard) break;
                    if (model.bench.isEmpty()) {
                        view.showEphemeralMessage("Votre banc est vide !");
                        break;
                    }
                    CardData randomBenchCard = model.bench.get(
                        new Random().nextInt(model.bench.size())
                    );
                    CardData tableCardData = model.table;
                    view.swapTableAndBench(
                        view.getMyBenchCardById(
                            randomBenchCard.getAtlasRegionName()
                        )
                    );
                    model.moveFromTableToBench(tableCardData);
                    model.moveFromBenchToTable(randomBenchCard);
                    client.sendRetreat(randomBenchCard.getAtlasRegionName());
                    break;
                }
                case "ChangementT": {
                    if (!isMyCard) break;
                    String[] climats = {
                        "Tempéré",
                        "Désertique",
                        "Océanique",
                        "Montagneux",
                        "Tropical",
                        "Glacial",
                    };
                    String climatAction = climats[value];
                    if (model.terrain.equals(climatAction)) {
                        view.showEphemeralMessage(
                            "Le terrain à déjà un climat " + climatAction
                        );
                        break;
                    }
                    client.sendField(climatAction);
                }
            }
        }
    }
}
