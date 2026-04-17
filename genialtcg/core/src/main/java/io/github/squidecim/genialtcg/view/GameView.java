package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import io.github.squidecim.genialtcg.*;
import io.github.squidecim.genialtcg.controller.GameController;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.GameModel;

public class GameView implements Screen {

    private GenialTCG game;
    private GameModel model;

    private GameController controller;

    private PerspectiveCamera cam;
    private Environment environment;
    private ModelBatch modelBatch;

    private Texture frontTexture;
    private Texture backTexture;

    private CardSlot tableSlot;
    private CardSlot opponentTableSlot;
    private Array<CardSlot>  benchTopSlots = new Array<>();
    private Array<CardSlot>  benchBottomSlots = new Array<>();
    private Array<CardDecal> handCards = new Array<>();
    private Array<CardDecal> opponentHandCards = new Array<>();

    private CardsStackDecal deck;
    private CardsStackDecal opponentDeck;
    private CardsStackDecal discard;
    private CardsStackDecal opponentDiscard;

    public static final float TABLE_CARD_W = 1.5f;
    public static final float TABLE_CARD_H = 2.1f;
    public static final float BENCH_CARD_W = 1.1f;
    public static final float BENCH_CARD_H = 1.54f;
    private static final float BENCH_GAP_X = 0.2f;
    private static final float BENCH_GAP_Z = 0.3f;
    private static final float TABLE_GAP = 0.15f;

    private static final float THICKNESS = 0.007f;

    private CardDecal hoveredCard = null;

    private CardDecal draggedCard = null;

    public GameView(GenialTCG game, GameModel model) {
        this.game = game;
        this.model = model;
    }

    @Override
    public void show() {
        cam = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        cam.position.set(0, 5.5f, 5.75f);
        cam.lookAt(0, 0, 2f);
        cam.near = 0.1f;
        cam.far = 1000f;
        cam.update();

        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f));
        environment.add(new DirectionalLight().set(1f, 1f, 1f,  1f,  0.8f,  0.2f));

        frontTexture = new Texture("frontCardTexture.jpg");
        backTexture = new Texture("backCardTexture.png");

        tableSlot = new CardSlot(new Vector3(0, 0,  TABLE_GAP / 2 + TABLE_CARD_H / 2), 0, -90f, 0, "table");
        opponentTableSlot = new CardSlot(new Vector3(0, 0, -TABLE_GAP / 2 - TABLE_CARD_H / 2), 0, -90f, 0, "table");

        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (BENCH_CARD_W + BENCH_GAP_X);
            benchBottomSlots.add(new CardSlot(new Vector3(x, 0, 0.5f + TABLE_CARD_H + BENCH_GAP_Z * 2.5f), 0, -90f, 0, "bench"));
        }
        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (BENCH_CARD_W + BENCH_GAP_X);
            benchTopSlots.add(new CardSlot(new Vector3(x, 0, -0.5f - TABLE_CARD_H - BENCH_GAP_Z * 2.5f), 0, -90f, 0, "bench"));
        }

        deck = createCardsStacks(new TextureRegion(backTexture),  BENCH_CARD_W, BENCH_CARD_H, 50,  3.25f, 0,  3.2f);
        opponentDeck = createCardsStacks(new TextureRegion(backTexture),  BENCH_CARD_W, BENCH_CARD_H, 50, -3.25f, 0, -3.2f);
        discard = createCardsStacks(new TextureRegion(frontTexture), BENCH_CARD_W, BENCH_CARD_H,  0,  4.75f, 0,  3.2f);
        opponentDiscard = createCardsStacks(new TextureRegion(frontTexture), BENCH_CARD_W, BENCH_CARD_H,  0, -4.75f, 0, -3.2f);

        updateDeckVisual(model.deckSize());
    }

    @Override
    public void render(float delta) {

        if (controller != null) {
            controller.update(delta);
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        modelBatch.begin(cam);

        if (!tableSlot.isEmpty()) tableSlot.getCard().render(modelBatch, environment);
        if (!opponentTableSlot.isEmpty()) opponentTableSlot.getCard().render(modelBatch, environment);

        for (CardSlot slot : benchBottomSlots)
            if (!slot.isEmpty()) slot.getCard().render(modelBatch, environment);
        for (CardSlot slot : benchTopSlots)
            if (!slot.isEmpty()) slot.getCard().render(modelBatch, environment);

        for (int i = 0; i < handCards.size; i++) {
            CardDecal card = handCards.get(i);
            card.update(delta);
            card.render(modelBatch, environment);
        }

        for (int i = 0; i < opponentHandCards.size; i++)
            opponentHandCards.get(i).render(modelBatch, environment);

        for (CardSlot slot : benchBottomSlots) slot.renderHighlight(modelBatch, environment);

        tableSlot.renderHighlight(modelBatch, environment);

        deck.render(modelBatch, environment);
        opponentDeck.render(modelBatch, environment);
        discard.render(modelBatch, environment);
        opponentDiscard.render(modelBatch, environment);

        if (draggedCard != null) {
            draggedCard.update(delta);
            draggedCard.render(modelBatch, environment);
        }

        modelBatch.end();
    }

    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    @Override
    public void dispose() {
        frontTexture.dispose();
        backTexture.dispose();
        modelBatch.dispose();
        for (CardDecal card : handCards) card.dispose();
        for (CardDecal card : opponentHandCards) card.dispose();
        for (CardSlot slot : benchBottomSlots) slot.dispose();
        for (CardSlot slot : benchTopSlots) slot.dispose();
        tableSlot.dispose();
        opponentTableSlot.dispose();
    }

    public void setController(GameController controller) {
        this.controller = controller;
    }

    public void addCardToHand(CardData data) {
        CardDecal decal = new CardDecal(data,
            new TextureRegion(frontTexture),
            new TextureRegion(backTexture),
            BENCH_CARD_W, BENCH_CARD_H, cam, "deck");

        decal.setRotation(180f, 90f, 0);
        handCards.add(decal);
        repositionHand();
    }

    private void repositionHand() {

        int n = handCards.size;
        float maxWidth = 7f;
        float cardWidth = BENCH_CARD_W;
        float visibleRatio = 0.7f;
        float spacing = cardWidth * visibleRatio;
        float totalWidth = spacing * (n - 1);
        if (totalWidth > maxWidth) {
            spacing = maxWidth / (n - 1);
        }

        float center = (n - 1) / 2f;
        Vector3 deckPos = deck.getPosition();
        float deckTop = deck.nbrCards * THICKNESS;
        Vector3 deckTopPos = new Vector3(deckPos.x, deckPos.y + deckTop, deckPos.z);

        for (int i = 0; i < n; i++) {
            CardDecal card = handCards.get(i);
            card.setHandIndex(i);

            float x = (i - center) * spacing;
            float y = 0.5f + (i - center) * THICKNESS;
            float z = 5f;
            float angleX = 0;
            Vector3 dest = new Vector3(x, y, z);
            if (card.emplacement.equals("deck")) {
                card.animateFromDeck(deckTopPos, dest, angleX, -50f, 0f, 0.4f);
                card.emplacement = "hand";
            } else {
                card.animateTo(dest, angleX, -50f, 0f, 0.4f);
            }
        }
    }

    public void updateDeckVisual(int size) {
        deck.updateSize(size);
    }

    public PerspectiveCamera getCam() { return cam; }

    public CardDecal getHoveredCard(Ray ray) {
        if (hoveredCard != null && hoveredCard.intersects(ray) && !hoveredCard.isAnimating()) return hoveredCard;
        for (int i = handCards.size - 1; i >= 0; i--) {
            CardDecal card = handCards.get(i);
            if (card.intersects(ray) && !card.isAnimating()) return card;
        }
        return null;
    }

    public void setHoveredCard(CardDecal card) {
        if (hoveredCard != null && hoveredCard != card) hoveredCard.setHovered(false);
        hoveredCard = card;
        if (hoveredCard != null) hoveredCard.setHovered(true);
    }

    public void updateHover(Ray ray) {
        setHoveredCard(getHoveredCard(ray));
    }

    public boolean isDeckClicked(Ray ray) {
        return deck.intersects(ray);
    }

    private CardsStackDecal createCardsStacks(TextureRegion tex, float w, float h, int n, float x, float y, float z) {
        CardsStackDecal stack = new CardsStackDecal(tex, w, h, n);
        stack.setPosition(x, y, z);
        return stack;
    }

    public void startDrag(CardDecal card) {
        card.setHovered(false);
        hoveredCard = null;
        draggedCard = card;
        handCards.removeValue(card, true);
        repositionHand();
        card.setRotation(0, -90f, 0);
        for (CardSlot slot : benchBottomSlots) {
            if (slot.isEmpty()) slot.setHighlighted(true);
        }
        if (tableSlot.isEmpty()) tableSlot.setHighlighted(true);
    }

    public void updateDragPosition(Ray ray) {
        if (draggedCard == null) return;
        Plane groundPlane = new Plane(new Vector3(0, 1, 0), new Vector3(0, 0.3f, 0));
        Vector3 intersection = new Vector3();
        if (Intersector.intersectRayPlane(ray, groundPlane, intersection)) {
            draggedCard.setDragPosition(intersection.x, 0.5f, intersection.z);
        }
        for (CardSlot slot : benchBottomSlots) {
            if (slot.isEmpty()) slot.setHighlighted(true);
        }
        if (tableSlot.isEmpty()) tableSlot.setHighlighted(true);
    }

    public CardSlot getIntersectedSlot(Ray ray) {
        for (CardSlot slot : benchBottomSlots) {
            if (slot.intersects(ray)) return slot;
        }

        if (tableSlot.intersects(ray)) return tableSlot;

        return null;
    }

    public CardSlot getFirstEmptyBenchSlot() {
        for (CardSlot slot : benchBottomSlots) {
            if (slot.isEmpty()) return slot;
        }
        return null;
    }

    public void dropCardOnSlot(CardDecal card, CardSlot slot) {
        card.emplacement = "bench";
        slot.setCard(card);
        card.animateTo(slot.getPosition(), 0, -90f, 0, 0.3f);
        for (CardSlot s : benchBottomSlots) s.setHighlighted(false);
        tableSlot.setHighlighted(false);
        draggedCard = null;
    }

    public void cancelDrag(CardDecal card) {

        int index = Math.min(card.getHandIndex(), handCards.size);
        handCards.insert(index, card);
        repositionHand();
        for (CardSlot s : benchBottomSlots) s.setHighlighted(false);
        tableSlot.setHighlighted(false);
        draggedCard = null;
    }
}
