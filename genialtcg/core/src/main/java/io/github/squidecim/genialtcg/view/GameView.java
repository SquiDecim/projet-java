package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.*;
import io.github.squidecim.genialtcg.controller.GameController;
import io.github.squidecim.genialtcg.mainMenu.MainScreen;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.GameModel;
import io.github.squidecim.genialtcg.network.GameClient;
import io.github.squidecim.genialtcg.network.NetworkMessages;

public class GameView implements Screen {

    private final int deckSize;
    private GenialTCG game;
    private GameModel model;
    private GameController controller;

    private GameClient client;

    private Environment environment;
    private ModelBatch modelBatch;
    private PerspectiveCamera cam;

    private Vector3 savedCamPosition = null;
    private Vector3 savedCamDirection = null;
    private Vector3 camTargetPos = null;
    private Vector3 camTargetDir = null;

    private boolean camAnimating = false;
    private float camAnimTimer = 0f;
    private float camAnimDuration = 0.6f;

    private Runnable onCamAnimDone = null;

    private Vector3 camStartPos = null;
    private Vector3 camStartDir = null;

    private TextureAtlas cardAtlas;
    private TextureAtlas actionsAtlas;
    private TextureAtlas outilsAtlas;
    private Texture backTexture;

    private Texture backgroundTexture;
    private TextureRegion reversedBackgroundRegion;

    private CardSlot tableSlot;
    private CardSlot opponentTableSlot;
    private Array<CardSlot> benchTopSlots = new Array<>();
    private Array<CardSlot> benchBottomSlots = new Array<>();
    private Array<CardDecal> handCards = new Array<>();
    private Array<CardDecal> opponentHandCards = new Array<>();
    private CardSlot actionSlot;

    private CardsStackDecal deck;
    private CardsStackDecal opponentDeck;
    private CardsStackDecal discard;
    private CardsStackDecal opponentDiscard;

    private CardDecal zoomGhost = null;
    private ShapeRenderer shapeRenderer;

    public static final float TABLE_CARD_W = 1.3f;
    public static final float TABLE_CARD_H = 1.82f;
    public static final float BENCH_CARD_W = 1.1f;
    public static final float BENCH_CARD_H = 1.54f;
    private static final float BENCH_GAP_X = 0.56f;
    private static final float BENCH_GAP_Z = 0.285f;
    private static final float TABLE_GAP = 0.48f;

    private static final float THICKNESS = 0.007f;

    private CardDecal hoveredCard = null;
    private CardDecal draggedCard = null;
    private CardSlot originSlot = null;

    private ModelInstance boardInstance;
    private Model boardModel;
    private Texture boardTexture;
    private float flashAlpha = 0f;
    private boolean flashingOut = false;
    private boolean flashingIn = false;
    private float flashSpeed = 2.5f;
    private String pendingField = null;
    private Material boardMaterial;

    private Stage uiStage;
    private Skin uiSkin;

    private Label myCreditsLabel;
    private Label opponentCreditsLabel;
    private Label myPointsLabel;
    private Label opponentPointsLabel;
    private Label turnCountLabel;
    private BitmapFont uiLabelFont;

    private Texture vignetteTexture;

    private Label setupBanner;
    private TextButton actionButton;
    private Table attackMenu = null;

    private Label ephemeralLabel;
    private Table pauseOverlay;
    private Table pauseMenuTable;
    private Table settingsPanelTable;
    private Dialog forfeitDialog;

    private Array<FloatingText> floatingTexts = new Array<>();
    private SpriteBatch floatBatch;
    private BitmapFont floatFont;
    private BitmapFont specialDescFont;

    public boolean startClicked = false;
    private boolean attackMenuVisible = false;

    private Array<CardDecal> discardingCards = new Array<>();

    private int cardsInFlight = 0;

    private Runnable pendingActionListener;
    private Table bannerRow;

    public GameView(
        GenialTCG game,
        GameModel model,
        GameClient client,
        int deckSize
    ) {
        this.game = game;
        this.model = model;
        this.client = client;
        this.deckSize = deckSize;
    }

    @Override
    public void show() {
        cam = new PerspectiveCamera(
            70,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        cam.position.set(0, 5.5f, 5.75f);
        cam.lookAt(0, 0, 2f);
        cam.near = 0.1f;
        cam.far = 1000f;
        cam.update();

        modelBatch = new ModelBatch();

        shapeRenderer = new ShapeRenderer();

        vignetteTexture = createVignetteTexture(512, 512);

        boardTexture = new Texture("ui/plateau/" + model.terrain + ".png");

        ModelBuilder builder = new ModelBuilder();
        builder.begin();

        MeshPartBuilder mpb = builder.part(
            "plateau",
            GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position |
                VertexAttributes.Usage.Normal |
                VertexAttributes.Usage.TextureCoordinates,
            new Material(
                TextureAttribute.createDiffuse(boardTexture),
                IntAttribute.createCullFace(GL20.GL_NONE)
            )
        );

        mpb.rect(
            -7f,
            -0.0001f,
            5.98f,
            7f,
            -0.0001f,
            5.98f,
            7f,
            -0.0001f,
            -5.98f,
            -7f,
            -0.0001f,
            -5.98f,
            0,
            -1,
            0
        );

        boardModel = builder.end();
        boardInstance = new ModelInstance(boardModel);

        boardMaterial = boardInstance.materials.get(0);

        environment = new Environment();
        environment.set(
            new ColorAttribute(
                ColorAttribute.AmbientLight,
                0.4f,
                0.4f,
                0.4f,
                1f
            )
        );
        environment.add(
            new DirectionalLight().set(1f, 1f, 1f, -1f, -0.8f, -0.2f)
        );
        environment.add(new DirectionalLight().set(1f, 1f, 1f, 1f, 0.8f, 0.2f));

        backgroundTexture = new Texture(Gdx.files.internal("ui/fond/bois.png"));
        reversedBackgroundRegion = new TextureRegion(
            backgroundTexture,
            0,
            0,
            backgroundTexture.getWidth(),
            backgroundTexture.getHeight()
        );
        reversedBackgroundRegion.flip(true, false);

        backTexture = new Texture("cards/backCardTexture.png");
        cardAtlas = new TextureAtlas(
            Gdx.files.internal("cards/dynamic/country_dynamic.atlas")
        );
        actionsAtlas = new TextureAtlas(
            Gdx.files.internal("cards/action/cards_actions.atlas")
        );
        outilsAtlas = new TextureAtlas(
            Gdx.files.internal("cards/outils/cards_outils.atlas")
        );
        for (Texture texture : cardAtlas.getTextures())
            texture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );
        for (Texture texture : actionsAtlas.getTextures())
            texture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );
        for (Texture texture : outilsAtlas.getTextures())
            texture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );

        tableSlot = new CardSlot(
            new Vector3(0, 0, TABLE_GAP / 2 + TABLE_CARD_H / 2),
            0,
            -90f,
            0,
            "table"
        );
        opponentTableSlot = new CardSlot(
            new Vector3(0, 0, -TABLE_GAP / 2 - TABLE_CARD_H / 2),
            0,
            -90f,
            0,
            "table"
        );

        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (BENCH_CARD_W + BENCH_GAP_X);
            benchBottomSlots.add(
                new CardSlot(
                    new Vector3(x, 0, 2.845f + BENCH_GAP_Z * 2.5f),
                    0,
                    -90f,
                    0,
                    "bench"
                )
            );
        }
        for (int i = 0; i < 4; i++) {
            float x = (i - 1.5f) * (BENCH_CARD_W + BENCH_GAP_X);
            benchTopSlots.add(
                new CardSlot(
                    new Vector3(x, 0, -2.85f - BENCH_GAP_Z * 2.5f),
                    0,
                    -90f,
                    0,
                    "bench"
                )
            );
        }

        actionSlot = new CardSlot(
            new Vector3(3.91f, 0, 0),
            0,
            -90f,
            0,
            "action"
        );

        deck = createCardsStacks(
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            deckSize,
            5.15f,
            0,
            4.03f
        );
        opponentDeck = createCardsStacks(
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            deckSize,
            -5.15f,
            0,
            -4.03f
        );
        discard = createCardsStacks(
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            0,
            5.15f,
            0,
            2.125f
        );
        opponentDiscard = createCardsStacks(
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            0,
            -5.15f,
            0,
            -2.125f
        );

        updateDeckVisual(model.deckSize());

        uiStage = new Stage(new ScreenViewport());
        uiSkin = game.skin;

        FreeTypeFontGenerator uiGen = new FreeTypeFontGenerator(
            Gdx.files.internal("ui/dejavu-sans/DejaVuSans-Bold.ttf")
        );
        FreeTypeFontGenerator.FreeTypeFontParameter uiParams =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        uiParams.size = 18;
        uiLabelFont = uiGen.generateFont(uiParams);
        uiGen.dispose();

        FreeTypeFontGenerator specialDescGen = new FreeTypeFontGenerator(
            Gdx.files.internal("ui/dejavu-sans/DejaVuSans-Oblique.ttf")
        );
        FreeTypeFontGenerator.FreeTypeFontParameter specialDescParams =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        specialDescParams.size = 14;
        specialDescFont = specialDescGen.generateFont(specialDescParams);
        specialDescGen.dispose();

        Label.LabelStyle uiStyle = new Label.LabelStyle(
            uiLabelFont,
            Color.WHITE
        );
        Color boxBg = new Color(0f, 0f, 0f, 0.82f);

        Table myPanel = new Table();
        myPanel.setFillParent(true);
        myPanel.bottom().left().padLeft(20).padBottom(130);

        Table myPointsBox = new Table();
        myPointsBox.setBackground(uiSkin.newDrawable("white", boxBg));
        myPointsLabel = new Label("Vos points : 0/6", uiStyle);
        myPointsBox.add(myPointsLabel).pad(9, 18, 9, 18);

        Table myCreditsBox = new Table();
        myCreditsBox.setBackground(uiSkin.newDrawable("white", boxBg));
        myCreditsLabel = new Label("Vos crédits : " + model.myCredits, uiStyle);
        myCreditsBox.add(myCreditsLabel).pad(9, 18, 9, 18);

        myPanel.add(myPointsBox).fillX().row();
        myPanel.add(myCreditsBox).fillX().padTop(7);
        uiStage.addActor(myPanel);

        actionButton = new TextButton("Commencer", uiSkin);
        actionButton.setSize(180, 50);
        actionButton.setPosition(
            Gdx.graphics.getWidth() * 0.875f,
            Gdx.graphics.getHeight() / 2f - 25
        );
        game.soundifyButton(actionButton);
        uiStage.addActor(actionButton);

        Table oppPanel = new Table();
        oppPanel.setFillParent(true);
        oppPanel.top().right().pad(20);

        Table oppPointsBox = new Table();
        oppPointsBox.setBackground(uiSkin.newDrawable("white", boxBg));
        opponentPointsLabel = new Label("Points adverses : 0/6", uiStyle);
        oppPointsBox.add(opponentPointsLabel).pad(9, 18, 9, 18);

        Table oppCreditsBox = new Table();
        oppCreditsBox.setBackground(uiSkin.newDrawable("white", boxBg));
        opponentCreditsLabel = new Label(
            "Crédits adverses : " + model.opponentCredits,
            uiStyle
        );
        oppCreditsBox.add(opponentCreditsLabel).pad(9, 18, 9, 18);

        oppPanel.add(oppPointsBox).fillX().row();
        oppPanel.add(oppCreditsBox).fillX().padTop(7);
        uiStage.addActor(oppPanel);

        Table turnPanel = new Table();
        turnPanel.setFillParent(true);
        turnPanel.top().left().padLeft(20).padTop(20);

        Table turnBox = new Table();
        turnBox.setBackground(uiSkin.newDrawable("white", boxBg));
        turnCountLabel = new Label("Nombre de tours : 0", uiStyle);
        turnBox.add(turnCountLabel).pad(9, 18, 9, 18);

        turnPanel.add(turnBox);
        uiStage.addActor(turnPanel);
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(controller);
        multiplexer.addProcessor(0, uiStage);

        setupBanner = new Label(
            "Veuillez poser une carte en Jeu pour commencer",
            game.skin,
            "title"
        );
        setupBanner.setFontScale(0.25f);
        setupBanner.setColor(Color.ORANGE);

        bannerRow = new Table();
        bannerRow.setBackground(
            uiSkin.newDrawable("white", new Color(0, 0, 0, 0.65f))
        );
        bannerRow.add(setupBanner).expandX().center().pad(12);
        bannerRow.setSize(
            Gdx.graphics.getWidth(),
            setupBanner.getPrefHeight() + 24
        );
        bannerRow.setPosition(0, Gdx.graphics.getHeight() * 0.80f);
        uiStage.addActor(bannerRow);

        hideActionButton();
        hideBanner();

        floatBatch = new SpriteBatch();
        floatFont = new BitmapFont();

        floatBatch.setProjectionMatrix(uiStage.getCamera().combined);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("ui/dejavu-sans/DejaVuSans-Bold.ttf")
        );
        FreeTypeFontGenerator.FreeTypeFontParameter params =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        params.size = 32;

        floatFont = generator.generateFont(params);
        generator.dispose();

        ephemeralLabel = new Label("", game.skin, "title");
        ephemeralLabel.setFontScale(0.25f);
        ephemeralLabel.setColor(Color.ORANGE);

        Table ephemeralTable = new Table();
        ephemeralTable.setFillParent(true);
        ephemeralTable.center();
        ephemeralTable.add(ephemeralLabel);
        uiStage.addActor(ephemeralTable);

        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void render(float delta) {
        if (controller != null) {
            controller.update(delta);

            if (camAnimating) {
                camAnimTimer += delta;
                float t = Math.min(camAnimTimer / camAnimDuration, 1f);
                t = t * t * (3f - 2f * t);
                cam.position.set(
                    lerp(camStartPos.x, camTargetPos.x, t),
                    lerp(camStartPos.y, camTargetPos.y, t),
                    lerp(camStartPos.z, camTargetPos.z, t)
                );
                Vector3 dir = new Vector3(
                    lerp(camStartDir.x, camTargetDir.x, t),
                    lerp(camStartDir.y, camTargetDir.y, t),
                    lerp(camStartDir.z, camTargetDir.z, t)
                ).nor();
                cam.lookAt(cam.position.cpy().add(dir));
                cam.update();
                if (camAnimTimer >= camAnimDuration) {
                    camAnimating = false;
                    cam.position.set(camTargetPos);
                    cam.lookAt(cam.position.cpy().add(camTargetDir));
                    cam.update();
                    if (onCamAnimDone != null) {
                        Runnable cb = onCamAnimDone;
                        onCamAnimDone = null;
                        cb.run();
                    }
                }
            }
        }

        Gdx.gl.glViewport(
            0,
            0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        floatBatch.begin();
        floatBatch.draw(
            reversedBackgroundRegion,
            0,
            0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        floatBatch.end();

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        if (flashingOut) {
            flashAlpha += delta * flashSpeed;
            if (flashAlpha >= 1f) {
                flashAlpha = 1f;
                flashingOut = false;
                Texture oldTex = boardTexture;
                boardTexture = new Texture(
                    "ui/plateau/" + pendingField + ".png"
                );
                boardTexture.setFilter(
                    Texture.TextureFilter.Linear,
                    Texture.TextureFilter.Linear
                );
                boardMaterial.set(TextureAttribute.createDiffuse(boardTexture));
                oldTex.dispose();
                pendingField = null;
                flashingIn = true;
            }
        } else if (flashingIn) {
            flashAlpha -= delta * flashSpeed;
            if (flashAlpha <= 0f) {
                flashAlpha = 0f;
                flashingIn = false;
            }
        }
        if (flashAlpha > 0f) {
            boardMaterial.set(
                ColorAttribute.createEmissive(
                    flashAlpha,
                    flashAlpha,
                    flashAlpha,
                    1f
                )
            );
        } else {
            boardMaterial.remove(ColorAttribute.Emissive);
        }

        modelBatch.begin(cam);
        modelBatch.render(boardInstance, environment);

        if (!tableSlot.isEmpty()) {
            CardDecal tc = tableSlot.getCard();
            if (tc.isAnimating()) tc.update(delta);
            tc.render(modelBatch, environment);
        }
        if (!opponentTableSlot.isEmpty()) {
            CardDecal otc = opponentTableSlot.getCard();
            if (otc.isAnimating()) otc.update(delta);
            otc.render(modelBatch, environment);
        }

        for (int i = 0; i < handCards.size; i++) {
            CardDecal card = handCards.get(i);
            card.update(delta);
            card.render(modelBatch, environment);
        }

        for (int i = 0; i < opponentHandCards.size; i++) {
            CardDecal card = opponentHandCards.get(i);
            card.update(delta);
            card.render(modelBatch, environment);
        }

        for (CardSlot slot : benchBottomSlots) {
            slot.renderHighlight(modelBatch, environment);
            slot.renderSelectable(modelBatch, environment);
            if (!slot.isEmpty()) {
                CardDecal card = slot.getCard();
                if (card.isAnimating()) card.update(delta);
                card.render(modelBatch, environment);
            }
        }

        for (CardSlot slot : benchTopSlots) {
            slot.renderSelectable(modelBatch, environment);
            if (!slot.isEmpty()) {
                CardDecal card = slot.getCard();
                if (card.isAnimating()) card.update(delta);
                card.render(modelBatch, environment);
            }
        }

        if (!actionSlot.isEmpty()) {
            CardDecal ac = actionSlot.getCard();
            if (ac.isAnimating()) ac.update(delta);
            ac.render(modelBatch, environment);
        }

        tableSlot.renderHighlight(modelBatch, environment);

        actionSlot.renderHighlight(modelBatch, environment);

        deck.render(modelBatch, environment);
        opponentDeck.render(modelBatch, environment);
        discard.render(modelBatch, environment);
        opponentDiscard.render(modelBatch, environment);

        if (draggedCard != null) {
            draggedCard.update(delta);
            draggedCard.render(modelBatch, environment);
        }

        for (CardDecal card : discardingCards) {
            card.update(delta);
            card.render(modelBatch, environment);
        }

        modelBatch.end();

        if (zoomGhost != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 0.6f);
            shapeRenderer.rect(
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
            );
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            modelBatch.begin(cam);
            zoomGhost.render(modelBatch, environment);
            modelBatch.end();
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        floatBatch.begin();
        floatBatch.setColor(1f, 1f, 1f, 1f);
        floatBatch.draw(
            vignetteTexture,
            0,
            0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        floatBatch.end();

        uiStage.act(delta);
        uiStage.draw();

        if (floatingTexts.size > 0) {
            GlyphLayout layout = new GlyphLayout();
            floatBatch.begin();
            for (int i = floatingTexts.size - 1; i >= 0; i--) {
                FloatingText ft = floatingTexts.get(i);
                ft.update(delta);
                if (ft.isDead()) {
                    floatingTexts.removeIndex(i);
                    continue;
                }
                Vector3 screenPos = cam.project(ft.worldPos.cpy());
                floatFont.getData().setScale(ft.scale);
                floatFont.setColor(ft.color);
                layout.setText(floatFont, ft.text);
                floatFont.draw(
                    floatBatch,
                    ft.text,
                    screenPos.x - layout.width / 2f,
                    screenPos.y
                );
            }
            floatBatch.end();
            floatFont.getData().setScale(1f);
        }
    }

    @Override
    public void hide() {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void resize(int width, int height) {
        cam.viewportWidth = width;
        cam.viewportHeight = height;
        cam.update();
        uiStage.getViewport().update(width, height, true);

        floatBatch.setProjectionMatrix(uiStage.getCamera().combined);

        if (actionButton != null) {
            actionButton.setPosition(width * 0.875f, height / 2f - 25);
        }
        if (bannerRow != null) {
            bannerRow.setSize(width, setupBanner.getPrefHeight() + 24);
            bannerRow.setPosition(0, height * 0.80f);
        }
        if (forfeitDialog != null && forfeitDialog.hasParent()) {
            forfeitDialog.setPosition(
                (uiStage.getWidth() - forfeitDialog.getWidth()) / 2f,
                (uiStage.getHeight() - forfeitDialog.getHeight()) / 2f
            );
        }
    }

    @Override
    public void dispose() {
        backTexture.dispose();
        modelBatch.dispose();
        cardAtlas.dispose();
        actionsAtlas.dispose();
        outilsAtlas.dispose();
        for (CardDecal card : handCards) card.dispose();
        for (CardDecal card : opponentHandCards) card.dispose();
        for (CardSlot slot : benchBottomSlots) slot.dispose();
        for (CardSlot slot : benchTopSlots) slot.dispose();
        tableSlot.dispose();
        actionSlot.dispose();
        opponentTableSlot.dispose();
        boardModel.dispose();
        shapeRenderer.dispose();
        if (zoomGhost != null) zoomGhost.dispose();
        uiStage.dispose();
        floatBatch.dispose();
        if (floatFont != null) floatFont.dispose();
        if (uiLabelFont != null) uiLabelFont.dispose();
        if (specialDescFont != null) specialDescFont.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (vignetteTexture != null) vignetteTexture.dispose();
        if (boardTexture != null) boardTexture.dispose();
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public void moveCamToOpponentBench(Runnable onDone) {
        savedCamPosition = cam.position.cpy();
        savedCamDirection = new Vector3(cam.direction).cpy();

        camStartPos = cam.position.cpy();
        camStartDir = cam.direction.cpy();
        camTargetPos = new Vector3(0f, 3.5f, -0.8f);
        camTargetDir = new Vector3(0f, -3.5f, -3.25f).sub(camTargetPos).nor();

        camAnimTimer = 0f;
        camAnimating = true;
        onCamAnimDone = onDone;
    }

    public void restoreCam(Runnable onDone) {
        if (savedCamPosition == null) {
            if (onDone != null) onDone.run();
            return;
        }
        camStartPos = cam.position.cpy();
        camStartDir = cam.direction.cpy();
        camTargetPos = savedCamPosition.cpy();
        camTargetDir = savedCamDirection.cpy();
        savedCamPosition = null;
        savedCamDirection = null;

        camAnimTimer = 0f;
        camAnimating = true;
        onCamAnimDone = onDone;
    }

    public boolean isCamAnimating() {
        return camAnimating;
    }

    public void togglePauseMenu() {
        if (pauseOverlay != null) {
            if (settingsPanelTable != null && settingsPanelTable.isVisible()) {
                settingsPanelTable.setVisible(false);
                pauseMenuTable.setVisible(true);
            } else if (
                forfeitDialog != null && forfeitDialog.getParent() != null
            ) {
                forfeitDialog.hide();
                pauseMenuTable.setVisible(true);
            } else {
                hidePauseMenu();
            }
        } else {
            showPauseMenu();
        }
    }

    public boolean isPauseMenuVisible() {
        return pauseOverlay != null;
    }

    private Texture createVignetteTexture(int w, int h) {
        Pixmap px = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        float cx = w / 2f,
            cy = h / 2f;
        float maxDist = (float) Math.sqrt(cx * cx + cy * cy);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float dx = (x - cx) / cx;
                float dy = (y - cy) / cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float alpha = Math.max(0f, Math.min(1f, (dist - 0.4f) * 1.25f));
                alpha = alpha * alpha;
                px.setColor(0f, 0f, 0f, alpha * 0.85f);
                px.drawPixel(x, y);
            }
        }
        Texture t = new Texture(px);
        px.dispose();
        return t;
    }

    private void showPauseMenu() {
        if (pauseOverlay != null) return;
        actionButton.setTouchable(Touchable.disabled);
        Preferences prefs = Gdx.app.getPreferences("GenialTCG_Settings");

        pauseOverlay = new Table();
        pauseOverlay.setFillParent(true);
        pauseOverlay.setBackground(
            uiSkin.newDrawable("white", new Color(0, 0, 0, 0.45f))
        );
        pauseOverlay.addListener(
            new InputListener() {
                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    return true;
                }
            }
        );

        pauseMenuTable = new Table();
        pauseMenuTable.setBackground(
            uiSkin.newDrawable("white", new Color(0.1f, 0.12f, 0.18f, 0.97f))
        );
        pauseMenuTable.pad(40);

        Label pauseTitle = new Label("Pause", uiSkin, "title");
        pauseTitle.setFontScale(0.35f);
        pauseMenuTable.add(pauseTitle).padBottom(30).row();

        TextButton btnResume = new TextButton("Reprendre", uiSkin);
        TextButton btnSettingsMenu = new TextButton("Paramètres", uiSkin);
        TextButton btnForfeit = new TextButton("Déclarer Forfait", uiSkin);

        game.soundifyButton(btnResume);
        game.soundifyButton(btnSettingsMenu);
        game.soundifyButton(btnForfeit);

        pauseMenuTable.add(btnResume).width(250).height(50).padBottom(15).row();
        pauseMenuTable
            .add(btnSettingsMenu)
            .width(250)
            .height(50)
            .padBottom(15)
            .row();
        pauseMenuTable.add(btnForfeit).width(250).height(50).row();

        settingsPanelTable = new Table();
        settingsPanelTable.setBackground(
            uiSkin.newDrawable("white", new Color(0.1f, 0.12f, 0.18f, 0.97f))
        );
        settingsPanelTable.pad(40);

        Label title = new Label("Paramètres", uiSkin, "title");
        title.setFontScale(0.35f);
        settingsPanelTable.add(title).padBottom(30).row();

        Label volLabel = new Label("Volume Musique", uiSkin);
        Slider volSlider = new Slider(0f, 1f, 0.05f, false, uiSkin);
        volSlider.setValue(prefs.getFloat("music_volume", 0.3f));
        volSlider.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    float v = volSlider.getValue();
                    if (game.menuMusic != null) game.menuMusic.setVolume(v);
                    prefs.putFloat("music_volume", v);
                    prefs.flush();
                }
            }
        );
        settingsPanelTable.add(volLabel).left().padBottom(5).row();
        settingsPanelTable.add(volSlider).width(300).padBottom(20).row();

        Label uiVolLabel = new Label("Volume Sons UI", uiSkin);
        Slider uiVolSlider = new Slider(0f, 1f, 0.05f, false, uiSkin);
        uiVolSlider.setValue(prefs.getFloat("ui_sound_volume", 0.5f));
        uiVolSlider.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    float v = uiVolSlider.getValue();
                    game.uiSoundVolume = v;
                    prefs.putFloat("ui_sound_volume", v);
                    prefs.flush();
                }
            }
        );
        settingsPanelTable.add(uiVolLabel).left().padBottom(5).row();
        settingsPanelTable.add(uiVolSlider).width(300).padBottom(20).row();

        Label brightLabel = new Label("Luminosité", uiSkin);
        Slider brightSlider = new Slider(0.2f, 1.0f, 0.05f, false, uiSkin);
        brightSlider.setValue(game.globalBrightness);
        brightSlider.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.globalBrightness = brightSlider.getValue();
                    prefs.putFloat("brightness", game.globalBrightness);
                    prefs.flush();
                }
            }
        );
        settingsPanelTable.add(brightLabel).left().padBottom(5).row();
        settingsPanelTable.add(brightSlider).width(300).padBottom(30).row();

        Label displayLabel = new Label("Mode d'affichage", uiSkin);
        SelectBox<String> displayBox = new SelectBox<>(uiSkin);
        displayBox.setItems("Plein ecran", "Fenetre sans bordure", "Fenetre");
        displayBox.setSelected(prefs.getString("display_mode", "Plein ecran"));
        displayBox.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    String selected = displayBox.getSelected();
                    applyDisplayMode(selected);
                    prefs.putString("display_mode", selected);
                    prefs.flush();
                }
            }
        );
        settingsPanelTable.add(displayLabel).left().padBottom(5).row();
        settingsPanelTable.add(displayBox).width(300).padBottom(30).row();

        TextButton btnBackToPause = new TextButton("Retour", uiSkin);
        game.soundifyButton(btnBackToPause);
        settingsPanelTable.add(btnBackToPause).width(200).height(50);

        forfeitDialog = new Dialog("", uiSkin) {
            @Override
            protected void result(Object object) {
                if (object.equals(true)) {
                    client.sendPlayerQuit();
                    game.setScreen(new MainScreen(game));
                } else {
                    pauseMenuTable.setVisible(true);
                }
            }
        };
        forfeitDialog
            .getContentTable()
            .add(new Label("Voulez-vous vraiment déclarer forfait ?", uiSkin))
            .pad(10)
            .row();
        forfeitDialog.getButtonTable().defaults().width(120).height(40).pad(10);
        forfeitDialog.button("Non", false);
        forfeitDialog.button("Oui", true);
        for (com.badlogic.gdx.scenes.scene2d.ui.Cell<?> cell : forfeitDialog
            .getButtonTable()
            .getCells()) {
            if (cell.getActor() instanceof TextButton) {
                game.soundifyButton((TextButton) cell.getActor());
            }
        }
        forfeitDialog.setResizable(true);
        forfeitDialog.addListener(
            new InputListener() {
                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    if (
                        x < 0 ||
                        x > forfeitDialog.getWidth() ||
                        y < 0 ||
                        y > forfeitDialog.getHeight()
                    ) {
                        forfeitDialog.hide();
                    }
                    return false;
                }
            }
        );

        btnResume.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    hidePauseMenu();
                }
            }
        );

        btnSettingsMenu.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    pauseMenuTable.setVisible(false);
                    settingsPanelTable.setVisible(true);
                }
            }
        );

        btnBackToPause.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    settingsPanelTable.setVisible(false);
                    pauseMenuTable.setVisible(true);
                }
            }
        );

        btnForfeit.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    pauseMenuTable.setVisible(false);
                    forfeitDialog.show(uiStage);
                }
            }
        );

        Stack stack = new Stack();
        stack.add(pauseMenuTable);
        stack.add(settingsPanelTable);

        pauseMenuTable.setVisible(true);
        settingsPanelTable.setVisible(false);

        pauseOverlay.add(stack).center();
        uiStage.addActor(pauseOverlay);
    }

    private void hidePauseMenu() {
        if (pauseOverlay != null) {
            pauseOverlay.remove();
            pauseOverlay = null;
            actionButton.setTouchable(Touchable.enabled);
        }
    }

    private void applyDisplayMode(String mode) {
        if ("Plein ecran".equals(mode)) {
            Gdx.graphics.setUndecorated(false);
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else if ("Fenetre sans bordure".equals(mode)) {
            Gdx.graphics.setUndecorated(true);
            Gdx.graphics.setWindowedMode(
                Gdx.graphics.getDisplayMode().width,
                Gdx.graphics.getDisplayMode().height
            );
        } else {
            Gdx.graphics.setUndecorated(false);
            Gdx.graphics.setWindowedMode(1280, 720);
        }
    }

    public void setController(GameController controller) {
        this.controller = controller;
        controller.startInitialDraw();
    }

    public void updateMyCredits(int credits) {
        myCreditsLabel.setText("Vos crédits : " + credits);
    }

    public Vector2 getMyCreditsLabelPos() {
        return myCreditsLabel.localToScreenCoordinates(
            new Vector2(
                myCreditsLabel.getWidth() * 0.5f,
                myCreditsLabel.getHeight() * 0.5f
            )
        );
    }

    public Vector2 getOpponentCreditsLabelPos() {
        return opponentCreditsLabel.localToScreenCoordinates(
            new Vector2(
                opponentCreditsLabel.getWidth() * 0.5f,
                opponentCreditsLabel.getHeight() * 0.5f
            )
        );
    }

    public void updateOpponentCredits(int credits) {
        opponentCreditsLabel.setText("Crédits adverses : " + credits);
    }

    public void updateTurnCount(int count) {
        turnCountLabel.setText("Nombre de tours : " + count);
    }

    public void addCardToHand(CardData data) {
        String regionName = data.getAtlasRegionName();
        AtlasRegion region;
        if (data.id != null && data.id.startsWith("ACT-")) {
            region = actionsAtlas.findRegion(regionName);
        } else if (data.id != null && data.id.startsWith("OUT-")) {
            region = outilsAtlas.findRegion(regionName);
        } else {
            region = cardAtlas.findRegion(regionName);
        }
        if (region == null) return;
        TextureRegion front = new TextureRegion(region);

        CardDecal decal = new CardDecal(
            data,
            front,
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            cam,
            "deck"
        );

        cardsInFlight++;
        decal.setDeckLandCallback(() -> cardsInFlight--);
        decal.setRotation(180f, 90f, 0);
        handCards.add(decal);
        decal.generateDynamicTexture(512, 716);
        repositionHand();
    }

    public boolean isAnyCardBeingDrawn() {
        return cardsInFlight > 0;
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
        Vector3 deckTopPos = new Vector3(
            deckPos.x,
            deckPos.y + deckTop,
            deckPos.z
        );

        for (int i = 0; i < n; i++) {
            CardDecal card = handCards.get(i);
            card.setHandIndex(i);

            float x = (i - center) * spacing;
            float y = 0.5f + i * THICKNESS;
            float z = 5.2f;
            float angleX = 0;
            Vector3 dest = new Vector3(x, y, z);
            if (card.emplacement.equals("deck")) {
                card.animateFromDeck(
                    deckTopPos,
                    dest,
                    angleX,
                    -50f,
                    0f,
                    0.4f,
                    true
                );
                card.emplacement = "hand";
            } else {
                card.animateTo(dest, angleX, -50f, 0f, 0.4f);
            }
        }
    }

    public void addCardToOpponentHand() {
        CardDecal decal = new CardDecal(
            null,
            new TextureRegion(backTexture),
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            cam,
            "deck"
        );
        decal.setRotation(180f, -90f, 0);
        opponentHandCards.add(decal);
        repositionOpponentHand();
    }

    private void repositionOpponentHand() {
        int n = opponentHandCards.size;
        float maxWidth = 7f;
        float spacing = BENCH_CARD_W * 0.7f;
        float totalWidth = spacing * (n - 1);
        if (totalWidth > maxWidth) spacing = maxWidth / (n - 1);

        float center = (n - 1) / 2f;
        Vector3 deckPos = opponentDeck.getPosition();
        float deckTop = opponentDeck.nbrCards * THICKNESS;
        Vector3 deckTopPos = new Vector3(
            deckPos.x,
            deckPos.y + deckTop,
            deckPos.z
        );

        for (int i = 0; i < n; i++) {
            CardDecal card = opponentHandCards.get(i);
            float x = (i - center) * spacing;
            float y = 0.75f + (i - center) * THICKNESS;
            float z = (float) (-4.7f + ((i - center) * THICKNESS) / 1.5);
            Vector3 dest = new Vector3(x, y, z);
            if (card.emplacement.equals("deck")) {
                card.animateFromDeck(deckTopPos, dest, 0, 25f, 0f, 0.4f, false);
                card.emplacement = "hand";
            } else {
                card.animateTo(dest, 0, 25f, 0f, 0.4f);
            }
        }
    }

    public void updateOpponentDeckVisual(int size) {
        opponentDeck.updateSize(size);
        addCardToOpponentHand();
    }

    private AtlasRegion findRegionForCard(CardData card) {
        String name = card.getAtlasRegionName();
        if (
            card.id != null && card.id.startsWith("ACT-")
        ) return actionsAtlas.findRegion(name);
        if (
            card.id != null && card.id.startsWith("OUT-")
        ) return outilsAtlas.findRegion(name);
        return cardAtlas.findRegion(name);
    }

    private CardSlot findEmptyOpponentBenchSlot() {
        for (CardSlot slot : benchTopSlots) {
            if (slot.isEmpty()) return slot;
        }
        return null;
    }

    public void addOpponentCardToBench(CardData card) {
        AtlasRegion region = findRegionForCard(card);
        if (region == null) return;
        CardSlot slot = findEmptyOpponentBenchSlot();
        if (slot == null) return;

        CardDecal decal = removeOpponentCardFromField(
            card.getAtlasRegionName()
        );
        if (decal != null) decal.buildModel(
            decal.frontRegion,
            decal.backRegion,
            BENCH_CARD_W,
            BENCH_CARD_H
        );
        if (decal == null) {
            Vector3 startPos =
                opponentHandCards.size > 0
                    ? opponentHandCards
                          .get(opponentHandCards.size - 1)
                          .getPosition()
                    : opponentDeck.getPosition();
            decal = new CardDecal(
                card,
                new TextureRegion(region),
                new TextureRegion(backTexture),
                BENCH_CARD_W,
                BENCH_CARD_H,
                cam,
                "bench"
            );
            decal.setPosition(startPos.x, startPos.y, startPos.z);
            decal.setRotation(0, -90f, 0);
            if (opponentHandCards.size > 0) {
                CardDecal ghost = opponentHandCards.removeIndex(
                    opponentHandCards.size - 1
                );
                ghost.dispose();
                repositionOpponentHand();
            }
        }
        decal.generateDynamicTexture(512, 716);
        slot.setCardDirect(decal);
        decal.animateTo(slot.getPosition(), 0, -90f, 0, 0.5f);
    }

    public void addOpponentCardToTable(CardData card) {
        AtlasRegion region = findRegionForCard(card);
        if (region == null) return;

        CardDecal decal = removeOpponentCardFromField(
            card.getAtlasRegionName()
        );

        if (decal != null) {
            decal.buildModel(
                decal.frontRegion,
                decal.backRegion,
                TABLE_CARD_W,
                TABLE_CARD_H
            );
            decal.emplacement = "table";
        } else {
            Vector3 startPos =
                opponentHandCards.size > 0
                    ? opponentHandCards
                          .get(opponentHandCards.size - 1)
                          .getPosition()
                    : opponentDeck.getPosition();
            decal = new CardDecal(
                card,
                new TextureRegion(region),
                new TextureRegion(backTexture),
                TABLE_CARD_W,
                TABLE_CARD_H,
                cam,
                "table"
            );
            decal.emplacement = "table";
            decal.setPosition(startPos.x, startPos.y, startPos.z);
            decal.setRotation(0, -90f, 0);
            if (opponentHandCards.size > 0) {
                CardDecal ghost = opponentHandCards.removeIndex(
                    opponentHandCards.size - 1
                );
                ghost.dispose();
                repositionOpponentHand();
            }
        }
        decal.generateDynamicTexture(512, 716);
        opponentTableSlot.setCardDirect(decal);
        decal.animateTo(opponentTableSlot.getPosition(), 0, -90f, 0, 0.5f);
    }

    public void addOpponentCardToAction(CardData card) {
        AtlasRegion region = findRegionForCard(card);
        if (region == null) return;

        CardDecal decal = removeOpponentCardFromField(
            card.getAtlasRegionName()
        );

        if (decal != null) {
            decal.buildModel(
                decal.frontRegion,
                decal.backRegion,
                BENCH_CARD_W,
                BENCH_CARD_H
            );
            decal.emplacement = "action";
        } else {
            Vector3 startPos =
                opponentHandCards.size > 0
                    ? opponentHandCards
                          .get(opponentHandCards.size - 1)
                          .getPosition()
                    : opponentDeck.getPosition();
            decal = new CardDecal(
                card,
                new TextureRegion(region),
                new TextureRegion(backTexture),
                BENCH_CARD_W,
                BENCH_CARD_H,
                cam,
                "action"
            );
            decal.setPosition(startPos.x, startPos.y, startPos.z);
            decal.setRotation(0, -90f, 0);
            if (opponentHandCards.size > 0) {
                CardDecal ghost = opponentHandCards.removeIndex(
                    opponentHandCards.size - 1
                );
                ghost.dispose();
                repositionOpponentHand();
            }
        }
        decal.generateDynamicTexture(512, 716);
        actionSlot.setCardDirect(decal);
        decal.animateTo(actionSlot.getPosition(), 0, -90f, 0, 0.5f);
    }

    public CardDecal removeOpponentCardFromField(String cardId) {
        for (CardSlot slot : benchTopSlots) {
            CardDecal c = slot.getCard();
            if (
                c != null &&
                c.getData() != null &&
                c.getData().getAtlasRegionName().equals(cardId)
            ) {
                slot.removeCard();
                return c;
            }
        }
        CardDecal c = opponentTableSlot.getCard();
        if (
            c != null &&
            c.getData() != null &&
            c.getData().getAtlasRegionName().equals(cardId)
        ) {
            opponentTableSlot.removeCard();
            return c;
        }
        return null;
    }

    public int getBenchSlotIndex(CardSlot slot) {
        return benchBottomSlots.indexOf(slot, true);
    }

    public CardDecal getMyBenchCardById(String cardId) {
        for (CardSlot slot : benchBottomSlots) {
            CardDecal c = slot.getCard();
            if (
                c != null &&
                c.getData() != null &&
                c.getData().getAtlasRegionName().equals(cardId)
            ) return c;
        }
        return null;
    }

    public void clearTableSlot(boolean mine) {
        if (mine) tableSlot.removeCard();
        else opponentTableSlot.removeCard();
    }

    public void clearBenchSlot(CardDecal card) {
        for (CardSlot slot : benchBottomSlots) {
            if (slot.getCard() == card) {
                slot.removeCard();
                return;
            }
        }
    }

    public void clearOpponentBenchSlot(CardDecal card) {
        for (CardSlot slot : benchTopSlots) {
            if (slot.getCard() == card) {
                slot.removeCard();
                return;
            }
        }
    }

    public void promoteFromBenchToTable(CardDecal benchCard) {
        CardSlot benchSlot = null;
        for (CardSlot slot : benchBottomSlots) {
            if (slot.getCard() == benchCard) {
                benchSlot = slot;
                break;
            }
        }
        if (benchSlot == null) return;
        benchSlot.removeCard();
        benchCard.rebuildWithDynamic(TABLE_CARD_W, TABLE_CARD_H);
        benchCard.emplacement = "table";
        tableSlot.setCardDirect(benchCard);
        benchCard.animateTo(tableSlot.getPosition(), 0, -90f, 0, 0.4f);
    }

    public void updateDeckVisual(int size) {
        deck.updateSize(size);
    }

    public PerspectiveCamera getCam() {
        return cam;
    }

    public CardDecal getHoveredCard(Ray ray) {
        if (
            hoveredCard != null &&
            hoveredCard.intersects(ray) &&
            !hoveredCard.isAnimating()
        ) return hoveredCard;
        for (int i = handCards.size - 1; i >= 0; i--) {
            CardDecal card = handCards.get(i);
            if (card.intersects(ray) && !card.isAnimating()) return card;
        }
        for (CardSlot slot : benchBottomSlots) {
            CardDecal card = slot.getCard();
            if (card != null && card.intersects(ray)) return card;
        }
        for (CardSlot slot : benchTopSlots) {
            CardDecal card = slot.getCard();
            if (card != null && card.intersects(ray)) return card;
        }
        CardDecal slotCard = tableSlot.getCard();
        if (slotCard != null && slotCard.intersects(ray)) return slotCard;
        CardDecal oppSlotCard = opponentTableSlot.getCard();
        if (
            oppSlotCard != null && oppSlotCard.intersects(ray)
        ) return oppSlotCard;
        return null;
    }

    public void setHoveredCard(CardDecal card) {
        if (hoveredCard != null && hoveredCard != card) hoveredCard.setHovered(
            false
        );
        if (
            card != null &&
            card != hoveredCard &&
            handCards.contains(card, true)
        ) {
            if (game.overpassCardsSound != null) game.overpassCardsSound.play(
                game.uiSoundVolume
            );
        }
        hoveredCard = card;
        if (hoveredCard != null) hoveredCard.setHovered(true);
    }

    public void updateHover(Ray ray) {
        if (cardsInFlight > 0) {
            if (hoveredCard != null) {
                hoveredCard.setHovered(false);
                hoveredCard = null;
            }
            return;
        }
        setHoveredCard(getHoveredCard(ray));
    }

    public boolean isDeckClicked(Ray ray) {
        return deck.intersects(ray);
    }

    public boolean isDiscardClicked(Ray ray) {
        return discard.intersects(ray);
    }

    public boolean isOpponentDiscardClicked(Ray ray) {
        return opponentDiscard.intersects(ray);
    }

    public CardsStackDecal getMyDiscard() {
        return discard;
    }

    public CardsStackDecal getOpponentDiscard() {
        return opponentDiscard;
    }

    private CardsStackDecal createCardsStacks(
        TextureRegion tex,
        float w,
        float h,
        int n,
        float x,
        float y,
        float z
    ) {
        CardsStackDecal stack = new CardsStackDecal(tex, w, h, n);
        stack.setPosition(x, y, z);
        return stack;
    }

    public boolean isOpponentCard(CardDecal card) {
        for (CardSlot slot : benchTopSlots) {
            if (slot.getCard() == card) return true;
        }
        return opponentTableSlot.getCard() == card;
    }

    public void startDrag(CardDecal card, Ray ray) {
        card.setHovered(false);
        hoveredCard = null;
        draggedCard = card;

        if (card.emplacement.equals("hand")) {
            handCards.removeValue(card, true);
            repositionHand();
            originSlot = null;
        } else if (
            card.emplacement.equals("bench") || card.emplacement.equals("table")
        ) {
            originSlot = findSlotContaining(card);
        }
        if (card.emplacement.equals("table")) draggedCard.rebuildWithDynamic(
            BENCH_CARD_W,
            BENCH_CARD_H
        );

        Vector3 pos = card.getPosition();
        card.setDragPosition(pos.x, 0.5f, pos.z);
        card.setRotation(0, -90f, 0);
        if (
            card.getData().id.startsWith("ACT-") &&
            model.phase == GameModel.Phase.PLAYING
        ) actionSlot.setHighlighted(true);
        else {
            if (!card.emplacement.equals("bench")) {
                for (CardSlot slot : benchBottomSlots) {
                    if (slot.isEmpty()) slot.setHighlighted(true);
                }
            }
            if (
                tableSlot.isEmpty() && !card.emplacement.equals("table")
            ) tableSlot.setHighlighted(true);
        }
    }

    public void updateDragPosition(Ray ray) {
        if (draggedCard == null) return;
        Plane groundPlane = new Plane(
            new Vector3(0, 1, 0),
            new Vector3(0, 0.3f, 0)
        );
        Vector3 intersection = new Vector3();
        if (Intersector.intersectRayPlane(ray, groundPlane, intersection)) {
            draggedCard.setDragPosition(intersection.x, 0.5f, intersection.z);
        }
        if (
            draggedCard.getData().id.startsWith("ACT-") &&
            model.phase == GameModel.Phase.PLAYING
        ) actionSlot.setHighlighted(true);
        else {
            if (!draggedCard.emplacement.equals("bench")) {
                for (CardSlot slot : benchBottomSlots) {
                    if (slot.isEmpty()) slot.setHighlighted(true);
                }
            }
            if (
                tableSlot.isEmpty() && !draggedCard.emplacement.equals("table")
            ) tableSlot.setHighlighted(true);
        }
    }

    public CardSlot getIntersectedSlot(Ray ray) {
        if (tableSlot.intersects(ray)) return tableSlot;

        for (CardSlot slot : benchBottomSlots) {
            if (slot.intersects(ray)) return slot;
        }

        if (actionSlot.intersects(ray)) return actionSlot;

        return null;
    }

    public CardSlot getFirstEmptyBenchSlot() {
        for (CardSlot slot : benchBottomSlots) {
            if (slot.isEmpty()) return slot;
        }
        return null;
    }

    public CardSlot findSlotContaining(CardDecal card) {
        for (CardSlot slot : benchBottomSlots) {
            if (slot.getCard() == card) return slot;
        }
        if (tableSlot.getCard() == card) return tableSlot;
        return null;
    }

    public void dropCardOnSlot(CardDecal card, CardSlot slot) {
        if (originSlot != null) {
            originSlot.removeCard();
        }
        slot.setCard(card);
        if (slot.type.equals("bench")) {
            card.emplacement = "bench";
        } else if (slot.type.equals("table")) {
            card.emplacement = "table";
        }
        card.animateTo(slot.getPosition(), 0, -90f, 0, 0.3f);
        for (CardSlot s : benchBottomSlots) s.setHighlighted(false);
        tableSlot.setHighlighted(false);
        actionSlot.setHighlighted(false);
        draggedCard = null;
        originSlot = null;
    }

    public void cancelDrag(CardDecal card) {
        card.setDragging(false);

        if (card.emplacement.equals("hand")) {
            int index = Math.min(card.getHandIndex(), handCards.size);
            handCards.insert(index, card);
            repositionHand();
        } else if (originSlot != null) {
            originSlot.setCard(card);
            card.animateTo(originSlot.getPosition(), 0, -90f, 0, 0.25f);
            card.emplacement = originSlot.type;
        }
        for (CardSlot s : benchBottomSlots) s.setHighlighted(false);
        tableSlot.setHighlighted(false);
        actionSlot.setHighlighted(false);
        draggedCard = null;
        originSlot = null;
    }

    public void showZoom(CardDecal card) {
        clearHover();
        if (zoomGhost != null) zoomGhost.dispose();
        zoomGhost = new CardDecal(
            card.getData(),
            card.frontRegion,
            card.backRegion,
            BENCH_CARD_W * 2.5f,
            BENCH_CARD_H * 2.5f,
            cam,
            "zoom"
        );
        if (card.getData() != null) zoomGhost.generateDynamicTexture(512, 716);
        zoomGhost.setPosition(0, 2f, 3.5f);
        Vector3 ghostPos = new Vector3(0, 1.75f, 4f);
        Vector3 toCam = new Vector3(cam.position).sub(ghostPos).nor();
        float pitch = (float) Math.toDegrees(Math.asin(toCam.y));
        zoomGhost.setRotation(0, -pitch + 10, 0);
        hideActionButton();
        hideBanner();
    }

    public void showZoom(CardsStackDecal stack) {
        clearHover();
        CardDecal card = stack.getCardOnTop();
        if (card == null) return;
        if (zoomGhost != null) zoomGhost.dispose();
        zoomGhost = new CardDecal(
            card.getData(),
            card.frontRegion,
            card.backRegion,
            BENCH_CARD_W * 2.5f,
            BENCH_CARD_H * 2.5f,
            cam,
            "zoom"
        );
        if (card.getData() != null) zoomGhost.generateDynamicTexture(512, 716);
        zoomGhost.setPosition(0, 2f, 3.5f);
        Vector3 ghostPos = new Vector3(0, 1.75f, 4f);
        Vector3 toCam = new Vector3(cam.position).sub(ghostPos).nor();
        float pitch = (float) Math.toDegrees(Math.asin(toCam.y));
        zoomGhost.setRotation(0, -pitch + 10, 0);
        hideActionButton();
        hideBanner();
    }

    public void clearHover() {
        if (hoveredCard != null) {
            hoveredCard.setHovered(false);
            hoveredCard = null;
        }
    }

    public void hideZoom() {
        if (zoomGhost != null) {
            zoomGhost.dispose();
            zoomGhost = null;
            if (model.phase == GameModel.Phase.SETUP && startClicked) return;
            if (model.phase == GameModel.Phase.SETUP && !model.setupDone) {
                showBanner();
                actionButton.setVisible(true);
            } else if (model.phase == GameModel.Phase.SETUP) {
                actionButton.setVisible(true);
                hideBanner();
            } else if (model.myTurn) {
                actionButton.setVisible(true);
            }
        }
    }

    public boolean isZooming() {
        return zoomGhost != null;
    }

    public boolean isZoomCardHit(Ray ray) {
        return zoomGhost != null && zoomGhost.intersects(ray);
    }

    public CardDecal getMyTableCard() {
        return tableSlot.getCard();
    }

    public CardDecal getOpponentTableCard() {
        return opponentTableSlot.getCard();
    }

    public CardDecal getActionCard() {
        return actionSlot.getCard();
    }

    public Vector3 getMyDiscardPosition() {
        return discard.getPosition();
    }

    public Vector3 getOpponentDiscardPosition() {
        return opponentDiscard.getPosition();
    }

    private ChangeListener currentActionListener = null;

    public void showActionButton(String text, Runnable action) {
        actionButton.setText(text);
        actionButton.setVisible(true);
        if (currentActionListener != null) {
            actionButton.removeListener(currentActionListener);
        }
        currentActionListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                action.run();
            }
        };
        actionButton.addListener(currentActionListener);
    }

    public void hideActionButton() {
        actionButton.setVisible(false);
    }

    public void hideBanner() {
        if (bannerRow != null) bannerRow.setVisible(false);
    }

    public void showBanner() {
        if (bannerRow != null) bannerRow.setVisible(true);
    }

    public void showBanner(String text) {
        if (bannerRow != null) {
            setupBanner.setText(text);
            bannerRow.setVisible(true);
        }
    }

    public void showAttackMenu(
        CardData myCard,
        GameClient client,
        GameController controller
    ) {
        if (attackMenu != null) attackMenu.remove();

        actionButton.setTouchable(Touchable.disabled);

        attackMenuVisible = true;

        attackMenu = new Table();
        attackMenu.setFillParent(true);
        attackMenu.center();
        attackMenu.setBackground(
            uiSkin.newDrawable("white", new Color(0, 0, 0, 0.3f))
        );

        Table content = new Table();

        Label title = new Label("Choisissez une attaque", game.skin, "title");
        title.setFontScale(0.5f);
        content.add(title).padBottom(20).colspan(2).center().row();

        String[] statNames = {
            "Puissance",
            "Ressources",
            "Technologie",
            "Stabilité",
        };

        for (int i = 0; i < statNames.length; i++) {
            final int index = i;
            TextButton btn = new TextButton(statNames[i], uiSkin);
            btn.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        hideAttackMenu();

                        CardDecal myTable = getMyTableCard();
                        CardDecal oppTable = getOpponentTableCard();
                        if (myTable == null || oppTable == null) return;

                        int[] statMapping = { 0, 2, 3, 4 };
                        int realIdx = statMapping[index];
                        int myVal = myTable.getData().stats[realIdx];
                        int oppVal = oppTable.getData().stats[realIdx];
                        int damage = myVal - oppVal;

                        NetworkMessages.NormalAttack msg =
                            new NetworkMessages.NormalAttack();
                        msg.damage = damage;
                        client.sendNormalAttack(damage, index);
                    }
                }
            );

            content
                .add(btn)
                .width(220)
                .height(50)
                .padTop(10)
                .padBottom(10)
                .padRight(10)
                .padLeft(10);

            if (i % 2 == 1) {
                content.row();
            }
        }

        content
            .add(new Label("──────────────", uiSkin))
            .padTop(10)
            .padBottom(10)
            .row();

        String specialName =
            myCard.specialName != null ? myCard.specialName : "Spécial";
        String specialDesc =
            myCard.specialDescription != null ? myCard.specialDescription : "";
        int specialCost = myCard.specialCost;
        int revocationCost = myCard.revocation;

        Color typeColor;
        switch (myCard.type != null ? myCard.type : "") {
            case "Militaire":
                typeColor = new Color(0.51f, 0.40f, 0.11f, 1f);
                break;
            case "Diplomatique":
                typeColor = new Color(0.04f, 0.35f, 0.70f, 1f);
                break;
            case "Économique":
                typeColor = new Color(0.82f, 0.60f, 0.29f, 1f);
                break;
            case "Renseignement":
                typeColor = new Color(0.47f, 0.47f, 0.47f, 1f);
                break;
            case "Isolationniste":
                typeColor = new Color(0.44f, 0.15f, 0.04f, 1f);
                break;
            default:
                typeColor = new Color(0.10f, 0.10f, 0.30f, 1f);
                break;
        }
        Color typeColorHover = new Color(
            Math.min(typeColor.r + 0.12f, 1f),
            Math.min(typeColor.g + 0.12f, 1f),
            Math.min(typeColor.b + 0.12f, 1f),
            1f
        );

        Table specialBlock = new Table();
        specialBlock.setBackground(uiSkin.newDrawable("white", typeColor));
        specialBlock.pad(10);
        specialBlock.setTouchable(Touchable.enabled);

        Label.LabelStyle boldBlackStyle = new Label.LabelStyle(
            uiLabelFont,
            Color.BLACK
        );
        Label specialNameLabel = new Label(
            specialName + " (" + specialCost + " crédits)",
            boldBlackStyle
        );
        Label.LabelStyle italicBlackStyle = new Label.LabelStyle(
            specialDescFont,
            Color.BLACK
        );
        Label specialDescLabel = new Label(specialDesc, italicBlackStyle);
        specialDescLabel.setWrap(true);

        specialBlock.add(specialNameLabel).left().padBottom(5).row();
        specialBlock.add(specialDescLabel).width(300).left();

        specialBlock.addListener(
            new InputListener() {
                @Override
                public void enter(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor fromActor
                ) {
                    if (pointer == -1) {
                        specialBlock.setBackground(
                            uiSkin.newDrawable("white", typeColorHover)
                        );
                        if (
                            fromActor == null ||
                            !fromActor.isDescendantOf(specialBlock)
                        ) {
                            if (game.hoverSound != null) game.hoverSound.play(
                                game.uiSoundVolume
                            );
                        }
                    }
                }

                @Override
                public void exit(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor toActor
                ) {
                    if (
                        pointer == -1 &&
                        (toActor == null ||
                            !toActor.isDescendantOf(specialBlock))
                    ) {
                        specialBlock.setBackground(
                            uiSkin.newDrawable("white", typeColor)
                        );
                    }
                }

                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    return true;
                }

                @Override
                public void touchUp(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    controller.handleSpecialAttack(myCard);
                }
            }
        );

        content
            .add(specialBlock)
            .colspan(2)
            .center()
            .expandX()
            .padBottom(10)
            .row();

        TextButton revocationBtn = new TextButton(
            "Retrait (" + revocationCost + " crédits)",
            uiSkin
        );
        revocationBtn.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    controller.startRetreat(myCard);
                }
            }
        );
        game.soundifyButton(revocationBtn);
        content
            .add(revocationBtn)
            .colspan(2)
            .center()
            .width(220)
            .height(50)
            .padTop(10)
            .padBottom(10)
            .padRight(10)
            .padLeft(10)
            .row();

        attackMenu.addListener(
            new InputListener() {
                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    if (event.getTarget() == attackMenu) hideAttackMenu();
                    return false;
                }
            }
        );
        attackMenu.add(content).center();
        uiStage.addActor(attackMenu);
    }

    public void hideAttackMenu() {
        if (attackMenu != null) {
            actionButton.setTouchable(Touchable.enabled);
            attackMenu.remove();
            attackMenu = null;
            attackMenuVisible = false;
        }
    }

    public boolean isAttackMenuVisible() {
        return attackMenuVisible;
    }

    public void spawnFloatingText(String text, Vector3 worldPos, Color color) {
        floatingTexts.add(new FloatingText(text, worldPos, 1.2f, color));
    }

    public void showEphemeralMessage(String text) {
        game.playImpossibleSound();
        ephemeralLabel.setText(text);
        ephemeralLabel.clearActions();
        ephemeralLabel.addAction(
            Actions.sequence(
                Actions.alpha(1f),
                Actions.delay(1f),
                Actions.fadeOut(0.3f),
                Actions.run(() -> ephemeralLabel.setText(""))
            )
        );
    }

    public void setSelectableBorder(boolean selectable) {
        for (CardSlot slot : benchBottomSlots) {
            if (!slot.isEmpty()) slot.setSelectable(selectable);
        }
    }

    public void setSelectableBorderForOwnBench(boolean selectable) {
        for (CardSlot slot : benchBottomSlots) {
            if (!slot.isEmpty()) slot.setSelectable(selectable);
        }
    }

    public void setSelectableBorderForOpponentBench(boolean selectable) {
        for (CardSlot slot : benchTopSlots) {
            if (!slot.isEmpty()) slot.setSelectable(selectable);
        }
    }

    public void clearAllSelectableBorders() {
        setSelectableBorder(false);
        setSelectableBorderForOpponentBench(false);
    }

    public CardDecal getBenchCardAt(Ray ray) {
        for (CardSlot slot : benchBottomSlots) {
            CardDecal card = slot.getCard();
            if (card != null && card.intersects(ray)) return card;
        }
        return null;
    }

    public CardDecal getOpponentBenchCardAt(Ray ray) {
        for (CardSlot slot : benchTopSlots) {
            CardDecal card = slot.getCard();
            if (card != null && card.intersects(ray)) return card;
        }
        return null;
    }

    public void swapTableAndBench(CardDecal benchCard) {
        CardDecal tableCard = tableSlot.getCard();
        CardSlot benchSlot = null;
        for (CardSlot slot : benchBottomSlots) {
            if (slot.getCard() == benchCard) {
                benchSlot = slot;
                break;
            }
        }
        if (tableCard == null || benchSlot == null) return;

        Vector3 tablePos = tableSlot.getPosition().cpy();
        Vector3 benchPos = benchSlot.getPosition().cpy();

        tableSlot.removeCard();
        benchSlot.removeCard();

        tableSlot.setCardDirect(benchCard);
        benchSlot.setCardDirect(tableCard);

        benchCard.emplacement = "table";
        tableCard.emplacement = "bench";

        benchCard.rebuildWithDynamic(TABLE_CARD_W, TABLE_CARD_H);
        tableCard.rebuildWithDynamic(BENCH_CARD_W, BENCH_CARD_H);

        benchCard.animateTo(tablePos, 0, -90f, 0, 0.4f);
        tableCard.animateTo(benchPos, 0, -90f, 0, 0.4f);
    }

    public CardDecal getFirstOpponentBenchCard() {
        for (CardSlot slot : benchTopSlots) {
            CardDecal c = slot.getCard();
            if (c != null) return c;
        }
        return null;
    }

    public CardDecal getFirstMyBenchCard() {
        for (CardSlot slot : benchBottomSlots) {
            CardDecal c = slot.getCard();
            if (c != null) return c;
        }
        return null;
    }

    public CardDecal getOpponentBenchCardById(String cardId) {
        for (CardSlot slot : benchTopSlots) {
            CardDecal c = slot.getCard();
            if (
                c != null &&
                c.getData() != null &&
                c.getData().getAtlasRegionName().equals(cardId)
            ) return c;
        }
        return null;
    }

    public void swapOpponentTableAndBench(CardDecal benchCard) {
        CardDecal tableCard = opponentTableSlot.getCard();
        CardSlot benchSlot = null;
        for (CardSlot slot : benchTopSlots) {
            if (slot.getCard() == benchCard) {
                benchSlot = slot;
                break;
            }
        }
        if (tableCard == null || benchSlot == null) return;

        Vector3 tablePos = opponentTableSlot.getPosition().cpy();
        Vector3 benchPos = benchSlot.getPosition().cpy();

        opponentTableSlot.removeCard();
        benchSlot.removeCard();

        opponentTableSlot.setCardDirect(benchCard);
        benchSlot.setCardDirect(tableCard);

        benchCard.emplacement = "table";
        tableCard.emplacement = "bench";

        benchCard.rebuildWithDynamic(TABLE_CARD_W, TABLE_CARD_H);
        tableCard.rebuildWithDynamic(BENCH_CARD_W, BENCH_CARD_H);

        benchCard.animateTo(tablePos, 0, -90f, 0, 0.4f);
        tableCard.animateTo(benchPos, 0, -90f, 0, 0.4f);
    }

    public void sendToDiscard(CardDecal card, boolean mine) {
        CardsStackDecal discardStack = mine ? discard : opponentDiscard;
        Vector3 discardPos = discardStack.getPosition().cpy();
        discardPos.y += discardStack.nbrCards * 0.007f + 0.007f;
        discardStack.setCardOnTop(card);
        discardingCards.add(card);

        card.animateTo(discardPos, 0, -90f, 0, 0.6f);

        com.badlogic.gdx.utils.Timer.schedule(
            new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    Gdx.app.postRunnable(() -> {
                        discardStack.updateSize(discardStack.nbrCards + 1);
                        if (card.getData() != null) {
                            discardStack.setTopTexture(
                                card.getTopTextureRegion()
                            );
                        }
                        discardingCards.removeValue(card, true);
                    });
                }
            },
            1f
        );
    }

    public void showToCam(
        CardDecal cardAction,
        boolean isMyCard,
        String targetBenchCardId
    ) {
        Vector3 newPosition = new Vector3(0, 4.35f, 5f);
        cardAction.animateTo(newPosition, 0, -55f, 0, 0.3f);

        com.badlogic.gdx.utils.Timer.schedule(
            new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    sendToDiscard(cardAction, isMyCard);
                }
            },
            1.3f
        );

        com.badlogic.gdx.utils.Timer.schedule(
            new com.badlogic.gdx.utils.Timer.Task() {
                @Override
                public void run() {
                    actionSlot.removeCard();
                    controller.handleAction(
                        cardAction,
                        isMyCard,
                        targetBenchCardId
                    );
                }
            },
            2f
        );
    }

    public void showToCam(CardDecal cardAction, boolean isMyCard) {
        showToCam(cardAction, isMyCard, null);
    }

    public void changeField(String field) {
        if (flashingOut || flashingIn) return;
        pendingField = field;
        flashingOut = true;
    }
}
