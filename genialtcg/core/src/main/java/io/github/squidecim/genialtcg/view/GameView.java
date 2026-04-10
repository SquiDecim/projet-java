package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import io.github.squidecim.genialtcg.*;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.GameModel;

public class GameView implements Screen {

    private GenialTCG game;
    private GameModel model;

    private PerspectiveCamera cam;

    private Environment environment;

    private DecalBatch worldBatch;
    private DecalBatch handBatch;
    private DecalBatch opponentHandBatch;

    private Texture frontTexture;
    private Texture backTexture;

    private Array<CardSlot> tableSlots  = new Array<>();
    private Array<CardSlot> benchTopSlots    = new Array<>();
    private Array<CardSlot> benchBottomSlots = new Array<>();
    private Array<CardDecal> handCards = new Array<>();
    private Array<CardDecal> opponentHandCards = new Array<>();
    private CardsStackDecal deck;
    private CardsStackDecal opponentDeck;
    private CardsStackDecal discard;
    private CardsStackDecal opponentDiscard;

    private static final float TABLE_CARD_W = 1.5f;
    private static final float TABLE_CARD_H = 2.1f;
    private static final float BENCH_CARD_W = 1.1f;
    private static final float BENCH_CARD_H = 1.54f;
    private static final float BENCH_GAP_X  = 0.2f;
    private static final float BENCH_GAP_Z  = 0.3f;
    private static final float TABLE_GAP    = 0.15f;

    private ModelBatch modelBatch;

    private CardDecal hoveredCard = null;

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

        ModelBuilder builder = new ModelBuilder();
        Model sphereModel = builder.createSphere(0.1f, 0.1f, 0.1f, 8, 8,
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight()
            .set(1f, 1f, 1f,
                -1f, -0.8f, -0.2f));
        environment.add(new DirectionalLight()
            .set(1f, 1f, 1f,
                1f, 0.8f, 0.2f));


        worldBatch = new DecalBatch(new CameraGroupStrategy(cam));
        handBatch  = new DecalBatch(new InsertionOrderStrategy());
        opponentHandBatch = new DecalBatch(new InsertionOrderStrategy());

        //Création des textures de debug
        frontTexture = new Texture("frontCardTexture.jpg");
        backTexture = new Texture("backCardTexture.png");

        //Création des slots du jeu
        tableSlots.add(new CardSlot(new Vector3(0, 0, 0 + TABLE_GAP/2 + TABLE_CARD_H/2), 0, -90f, 0));
        tableSlots.add(new CardSlot(new Vector3(0, 0, 0 - TABLE_GAP/2 - TABLE_CARD_H/2), 0, -90f, 0));

        //Création des slots du banc joueur
        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (BENCH_CARD_W + BENCH_GAP_X);
            benchBottomSlots.add(new CardSlot(new Vector3(x, 0, 0.5f + TABLE_CARD_H + (BENCH_GAP_Z * 2.5f)), 0, -90f, 0));
        }

        //Création des slots du banc adverse
        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (BENCH_CARD_W + BENCH_GAP_X);
            benchTopSlots.add(new CardSlot(new Vector3(x, 0, -0.5f - TABLE_CARD_H - (BENCH_GAP_Z * 2.5f)), 0, -90f, 0));
        }

        updateHandPositions();

        //Création du deck joueur
        deck = createCardsStacks(new TextureRegion(backTexture), BENCH_CARD_W, BENCH_CARD_H, 50, 3.25f, 0, 3.2f);
        //Création du deck adverse
        opponentDeck = createCardsStacks(new TextureRegion(backTexture), BENCH_CARD_W, BENCH_CARD_H, 50, -3.25f, 0, -3.2f);

        //Création de la défausse joueur
        discard = createCardsStacks(new TextureRegion(frontTexture), BENCH_CARD_W, BENCH_CARD_H, 0, 4.75f, 0, 3.2f);
        //Création de la défausse adverse
        opponentDiscard = createCardsStacks(new TextureRegion(frontTexture), BENCH_CARD_W, BENCH_CARD_H, 0, -4.75f, 0, -3.2f);

        updateDeckVisual(model.deckSize());
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT | Gdx.gl.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(Gdx.gl.GL_DEPTH_TEST);


        for (CardSlot slot : tableSlots) {
            if (!slot.isEmpty()) {
                slot.getCard().addToBatch(worldBatch);
            }
        }

        for (CardSlot slot : benchBottomSlots) {
            if (!slot.isEmpty()) {
                slot.getCard().addToBatch(worldBatch);
            }
        }

        for (CardSlot slot : benchTopSlots) {
            if (!slot.isEmpty()) {
                slot.getCard().addToBatch(worldBatch);
            }
        }

        for (int i = handCards.size - 1; i >= 0; i--) {
            CardDecal card = handCards.get(i);
            card.update(delta);
            if (card != hoveredCard)
                card.addToBatch(handBatch);
        }
        if (hoveredCard != null)
            hoveredCard.addToBatch(handBatch);

        for (int i = 0; i < opponentHandCards.size; i++) {
            CardDecal card = opponentHandCards.get(i);
            card.addToBatch(opponentHandBatch);
        }

        modelBatch.begin(cam);
        deck.render(modelBatch, environment);
        opponentDeck.render(modelBatch,environment);
        discard.render(modelBatch, environment);
        opponentDiscard.render(modelBatch,environment);
        modelBatch.end();

        worldBatch.flush();
        opponentHandBatch.flush();
        handBatch.flush();
    }

    @Override
    public void hide() {

    }

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        worldBatch.dispose();
        frontTexture.dispose();
        backTexture.dispose();
        modelBatch.dispose();
        handBatch.dispose();
        opponentHandBatch.dispose();
    }


    public void addCardToHand(CardData data) {
        CardDecal decal = new CardDecal(data, new TextureRegion(frontTexture), new TextureRegion(backTexture), BENCH_CARD_W, BENCH_CARD_H);

        Vector3 deckPos = deck.getPosition();
        decal.setPosition(deckPos.x, deckPos.y, deckPos.z);
        decal.setRotation(0, 90f, 0);

        handCards.add(decal);

        int n = handCards.size;
        float maxWidth = 4f;
        float spacing = n == 1 ? 0f : Math.min(0.85f, maxWidth / (n - 1));
        float center = (n - 1) / 2f;

        for (int i = 0; i < n; i++) {
            CardDecal card = handCards.get(i);
            float x = (i - center) * spacing;
            float y = 0.5f;
            float z = (float) (5f + Math.pow((i - center), 2) * 0.05f);
            float angleX = -(i - center) * 5f;

            card.animateTo(new Vector3(x, y, z), angleX, -50f, 0f, 0.4f);
        }
    }

    public void updateDeckVisual(int size) {
        deck.updateSize(size);
    }

    private void updateHandPositions() {
        //Main du joueur
        int n = handCards.size;
        if (n > 0) {
            float maxWidth = 4f;
            float spacing = n == 1 ? 0f : Math.min(0.85f, maxWidth / (n - 1));
            float center = (n - 1) / 2f;

            for (int i = 0; i < n; i++) {
                CardDecal card = handCards.get(i);
                float x = (i - center) * spacing;
                float y = 0.5f;
                float z = (float) (5f + Math.pow((i - center), 2) * 0.05f);

                card.setPosition(x, y, z);

                float angleX = -(i - center) * 5f;
                card.setRotation(angleX, -50f, 0);
            }
        }

        //Main adverse
        int m = opponentHandCards.size;
        if (m > 0) {
            float maxWidth = 4f;
            float spacing = m == 1 ? 0f : Math.min(0.85f, maxWidth / (m - 1));
            float center = (m - 1) / 2f;

            for (int i = 0; i < m; i++) {
                CardDecal card = opponentHandCards.get(i);
                float x = (i - center) * spacing;
                float y = 0.5f;
                float z = (float) (-4.5f + Math.pow((i - center), 2) * 0.075f);
                card.setPosition(x, y, z);

                float angleX = -(i - center) * 5f;
                card.setRotation(angleX, -20f, 0);
            }
        }
    }

    private CardsStackDecal createCardsStacks(TextureRegion cardTexture, float width, float height, int nbrCards, float x, float y, float z) {

        CardsStackDecal cardsStack = new CardsStackDecal(cardTexture, width, height, nbrCards);
        cardsStack.setPosition(x, y, z);
        return cardsStack;
    }

    public PerspectiveCamera getCam() { return cam; }

    public CardDecal getHoveredCard(Ray ray) {
        for (CardDecal card : handCards) {
            if (card.intersects(ray)) {
                return card;
            }
        }
        return null;
    }

    public void setHoveredCard(CardDecal card) {
        if (hoveredCard != null && hoveredCard != card) {
            hoveredCard.setHovered(false);
        }
        hoveredCard = card;
        if (hoveredCard != null) {
            hoveredCard.setHovered(true);
        }
    }

    public void updateHover(Ray ray) {
        CardDecal hovered = getHoveredCard(ray);
        setHoveredCard(hovered);
    }

    public boolean isDeckClicked(Ray ray) {
        return deck.intersects(ray);
    }
}
