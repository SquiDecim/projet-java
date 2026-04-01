package io.github.squidecim.genialtcg;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.utils.Array;

public class GameView implements Screen {

    private GenialTCG game;

    private PerspectiveCamera cam;

    private DecalBatch worldBatch;
    private DecalBatch handBatch;

    private Texture frontTexture;
    private Texture backTexture;

    private Array<CardDecal> tableCards  = new Array<>();
    private Array<CardDecal> benchTop    = new Array<>();
    private Array<CardDecal> benchBottom = new Array<>();
    private Array<CardDecal> handCards = new Array<>();

    private static final float TABLE_CARD_W = 1.5f;
    private static final float TABLE_CARD_H = 2.1f;
    private static final float BENCH_CARD_W = 1.1f;
    private static final float BENCH_CARD_H = 1.54f;
    private static final float BENCH_GAP_X  = 0.3f;
    private static final float BENCH_GAP_Z  = 0.3f;
    private static final float TABLE_GAP    = 0.15f;

    //debug :
    private ModelBatch modelBatch;
    private ModelInstance debugPoint;

    private CardDecal hoveredCard = null;

    public GameView(GenialTCG game) {
        this.game = game;
    }

    @Override
    public void show() {

        cam = new PerspectiveCamera(80, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        cam.position.set(0, 4, 6);
        cam.lookAt(0, 0, 2);
        cam.near = 0.1f;
        cam.far = 1000f;
        cam.update();

        modelBatch = new ModelBatch();

        ModelBuilder builder = new ModelBuilder();
        Model sphereModel = builder.createSphere(0.1f, 0.1f, 0.1f, 8, 8,
            new Material(ColorAttribute.createDiffuse(Color.RED)),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        debugPoint = new ModelInstance(sphereModel);
        debugPoint.transform.setToTranslation(0, 0, 0);

        worldBatch = new DecalBatch(new CameraGroupStrategy(cam));
        handBatch  = new DecalBatch(new InsertionOrderStrategy());

        frontTexture = new Texture("frontCardTexture.jpg");
        backTexture = new Texture("backCardTexture.png");

        for (int i = 0; i < 3; i++) {
            float x = (i - 1) * (TABLE_CARD_W + TABLE_GAP);
            tableCards.add(createCard(x, 0, 0 + TABLE_GAP/2 + TABLE_CARD_H/2, 0, -90f, 0, TABLE_CARD_W, TABLE_CARD_H));
            tableCards.add(createCard(x, 0, 0 - TABLE_GAP/2 - TABLE_CARD_H/2, 0, -90f, 0, TABLE_CARD_W, TABLE_CARD_H));
        }

        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (BENCH_CARD_W + BENCH_GAP_X);
            benchBottom.add(createCard(x, 0, 0.5f + TABLE_CARD_H + (BENCH_GAP_Z * 2.5f), 0, -90f, 0, BENCH_CARD_W, BENCH_CARD_H));
        }

        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (BENCH_CARD_W + BENCH_GAP_X);
            benchTop.add(createCard(x, 0, -0.5f - TABLE_CARD_H - (BENCH_GAP_Z * 2.5f), 0, -90f, 0, BENCH_CARD_W, BENCH_CARD_H));
        }

        for (int i = 0; i < 5; i++) {
            float x = (i - 2) * (BENCH_CARD_W - 0.2f);
            handCards.add(createCard(x, 0.5f, 5f, 0, -50f, 0, BENCH_CARD_W, BENCH_CARD_H));
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT | Gdx.gl.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(Gdx.gl.GL_DEPTH_TEST);

        for (CardDecal card : tableCards)  card.addToBatch(worldBatch);
        for (CardDecal card : benchBottom) card.addToBatch(worldBatch);
        for (CardDecal card : benchTop)    card.addToBatch(worldBatch);


        for (int i = handCards.size - 1; i >= 0; i--) {
            CardDecal card = handCards.get(i);
            card.update(delta);
            if (card != hoveredCard)
                card.addToBatch(handBatch);
        }
        if (hoveredCard != null)
            hoveredCard.addToBatch(handBatch);


        worldBatch.flush();
        handBatch.flush();

        modelBatch.begin(cam);
        modelBatch.render(debugPoint);
        modelBatch.end();
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

    }

    private CardDecal createCard(float x, float y, float z, float yaw, float pitch, float roll, float w, float h) {

        CardDecal card = new CardDecal(new TextureRegion(frontTexture), new TextureRegion(backTexture), w, h);
        card.setPosition(x, y, z);
        card.setRotation(yaw, pitch, roll);
        return card;
    }



    public PerspectiveCamera getCam() { return cam; }
    public Array<CardDecal> getHandCards() { return handCards; }

    public void setHoveredCard(CardDecal card) {
        if (hoveredCard != null && hoveredCard != card) {
            hoveredCard.setHovered(false);
        }
        hoveredCard = card;
        if (hoveredCard != null) {
            hoveredCard.setHovered(true);
        }
    }


}
