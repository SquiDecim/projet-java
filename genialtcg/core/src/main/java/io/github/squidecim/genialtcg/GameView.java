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

    private DecalBatch decalBatch;
    private CardDecal card;

    private Texture frontTexture;
    private Texture backTexture;

    private Array<CardDecal> tableCards  = new Array<>();
    private Array<CardDecal> benchTop    = new Array<>();
    private Array<CardDecal> benchBottom = new Array<>();
    private CardDecal handCard;

    float tableCardW = 1.5f;  float tableCardH = 2.1f;  // cartes zone de jeu
    float benchCardW = 1.1f;  float benchCardH = 1.54f; // cartes banc (environ 73%)
    float benchGapX = 0.3f;
    float benchGapZ = 0.3f;
    float tableGap = 0.15f;

    //debug :
    private ModelBatch modelBatch;
    private ModelInstance debugPoint;

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

        decalBatch = new DecalBatch(new CameraGroupStrategy(cam));

        frontTexture = new Texture("frontCardTexture.jpg");
        backTexture = new Texture("backCardTexture.png");

        for (int i = 0; i < 3; i++) {
            float x = (i - 1) * (tableCardW + tableGap);
            tableCards.add(createCard(x, 0, 0 + tableGap/2 + tableCardH/2, 0, -90f, 0, tableCardW, tableCardH));
            tableCards.add(createCard(x, 0, 0 - tableGap/2 - tableCardH/2, 0, -90f, 0, tableCardW, tableCardH));
        }

        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (benchCardW + benchGapX);
            benchBottom.add(createCard(x, 0, 0.5f + tableCardH + (benchGapZ * 2.5f), 0, -90f, 0, benchCardW, benchCardH));
        }

        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (benchCardW + benchGapX);
            benchTop.add(createCard(x, 0, -0.5f - tableCardH - (benchGapZ * 2.5f), 0, -90f, 0, benchCardW, benchCardH));
        }

        handCard = createCard(0, 0.5f, 5f, 0, -50f, 0, benchCardW, benchCardH);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT | Gdx.gl.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(Gdx.gl.GL_DEPTH_TEST);

        for (CardDecal c : tableCards)  c.addToBatch(decalBatch);
        for (CardDecal c : benchBottom) c.addToBatch(decalBatch);
        for (CardDecal c : benchTop)    c.addToBatch(decalBatch);
        handCard.addToBatch(decalBatch);

        decalBatch.flush();

        modelBatch.begin(cam);
        //modelBatch.render(debugPoint);
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
        decalBatch.dispose();
        frontTexture.dispose();
        backTexture.dispose();
        modelBatch.dispose();
    }

    private CardDecal createCard(float x, float y, float z, float yaw, float pitch, float roll, float w, float h) {

        CardDecal card = new CardDecal(new TextureRegion(frontTexture), new TextureRegion(backTexture), w, h);
        card.setPosition(x, y, z);
        card.setRotation(yaw, pitch, roll);
        return card;
    }
}
