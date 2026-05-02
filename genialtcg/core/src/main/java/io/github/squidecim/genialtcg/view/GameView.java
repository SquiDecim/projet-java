package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.*;
import io.github.squidecim.genialtcg.controller.GameController;
import io.github.squidecim.genialtcg.mainMenu.LobbyScreen;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.GameModel;

public class GameView implements Screen {

    private GenialTCG game;
    private GameModel model;

    private GameController controller;

    private PerspectiveCamera cam;
    private Environment environment;
    private ModelBatch modelBatch;

    private TextureAtlas cardAtlas;
    private TextureAtlas actionsAtlas;
    private TextureAtlas outilsAtlas;
    private Texture backTexture;

    private CardSlot tableSlot;
    private CardSlot opponentTableSlot;
    private Array<CardSlot> benchTopSlots = new Array<>();
    private Array<CardSlot> benchBottomSlots = new Array<>();
    private Array<CardDecal> handCards = new Array<>();
    private Array<CardDecal> opponentHandCards = new Array<>();

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

    private Stage uiStage;
    private Skin uiSkin;
    private Label myCreditsLabel;
    private Label opponentCreditsLabel;
    private Label setupBanner;
    private TextButton actionButton;
    private boolean setupDone = false;

    private Runnable pendingActionListener;

    public GameView(GenialTCG game, GameModel model) {
        this.game = game;
        this.model = model;
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

        Texture boardTexture = new Texture("ui/plateau/" + model.terrain + ".png");

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
            5.98f, //avant gauche
            7f,
            -0.0001f,
            5.98f, //avant droit
            7f,
            -0.0001f,
            -5.98f, //bas droit
            -7f,
            -0.0001f,
            -5.98f, //bas gauche
            0,
            -1,
            0
        );

        boardModel = builder.end();
        boardInstance = new ModelInstance(boardModel);

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

        backTexture = new Texture("cards/backCardTexture.png");
        cardAtlas = new TextureAtlas(Gdx.files.internal("cards/dynamic/country_dynamic.atlas"));
        actionsAtlas = new TextureAtlas(Gdx.files.internal("cards/action/cards_actions.atlas"));
        outilsAtlas = new TextureAtlas(Gdx.files.internal("cards/outils/cards_outils.atlas"));
        for (Texture texture : cardAtlas.getTextures())
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        for (Texture texture : actionsAtlas.getTextures())
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        for (Texture texture : outilsAtlas.getTextures())
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

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
                    new Vector3(
                        x,
                        0,
                        -2.85f - BENCH_GAP_Z * 2.5f
                    ),
                    0,
                    -90f,
                    0,
                    "bench"
                )
            );
        }

        deck = createCardsStacks(
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            40, //Passer le nombre réelle de cartes en paramètres bouffon
            5.15f,
            0,
            4.03f
        );
        opponentDeck = createCardsStacks(
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            40,
            -5.15f,
            0,
            -4.03f
        );
        discard = createCardsStacks(
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            0,
            4.75f,
            0,
            3.2f
        );
        opponentDiscard = createCardsStacks(
            new TextureRegion(backTexture),
            BENCH_CARD_W,
            BENCH_CARD_H,
            0,
            -4.75f,
            0,
            -3.2f
        );

        updateDeckVisual(model.deckSize());

        uiStage = new Stage(new ScreenViewport());
        uiSkin  = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table myCreditsTable = new Table();
        myCreditsTable.setFillParent(true);
        myCreditsTable.bottom().left().pad(20);

        myCreditsLabel = new Label("Crédits : 100", uiSkin);
        myCreditsLabel.setFontScale(1.2f);

        myCreditsTable.add(myCreditsLabel);
        uiStage.addActor(myCreditsTable);

        actionButton = new TextButton("Commencer", uiSkin);
        Table actionTable = new Table();
        actionTable.setFillParent(true);
        actionTable.right().center().pad(20);
        actionTable.add(actionButton).width(180).height(50);

        uiStage.addActor(actionTable);

        Table oppCreditsTable = new Table();
        oppCreditsTable.setFillParent(true);
        oppCreditsTable.top().right().pad(20);

        opponentCreditsLabel = new Label("Crédits : 100", uiSkin);
        opponentCreditsLabel.setFontScale(1.2f);

        oppCreditsTable.add(opponentCreditsLabel);
        uiStage.addActor(oppCreditsTable);
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(controller);
        multiplexer.addProcessor(0, uiStage);

        setupBanner = new Label("Veuillez poser une carte en Jeu pour commencer", uiSkin);
        setupBanner.setColor(Color.YELLOW);
        setupBanner.setFontScale(1.1f);

        Table bannerTable = new Table();
        bannerTable.setFillParent(true);
        bannerTable.center().padBottom(100);
        bannerTable.add(setupBanner);
        uiStage.addActor(bannerTable);

        Gdx.input.setInputProcessor(multiplexer);

    }

    @Override
    public void render(float delta) {
        if (controller != null) {
            controller.update(delta);
        }

        Gdx.gl.glViewport(
            0,
            0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

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
            if (!slot.isEmpty()) {
                CardDecal card = slot.getCard();
                if (card.isAnimating()) card.update(delta);
                card.render(modelBatch, environment);
            }
        }

        for (CardSlot slot : benchTopSlots) {
            if (!slot.isEmpty()) {
                CardDecal card = slot.getCard();
                if (card.isAnimating()) card.update(delta);
                card.render(modelBatch, environment);
            }
        }

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

        if (zoomGhost != null) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 0.6f);
            shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
            modelBatch.begin(cam);
            zoomGhost.render(modelBatch, environment);
            modelBatch.end();
            Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
        }

        uiStage.act(delta);
        uiStage.draw();
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
        opponentTableSlot.dispose();
        boardModel.dispose();
        shapeRenderer.dispose();
        if (zoomGhost != null) zoomGhost.dispose();
        uiStage.dispose();
        uiSkin.dispose();
    }

    public void setController(GameController controller) {
        this.controller = controller;
        controller.startInitialDraw();

    }

    public void updateMyCredits(int credits) {
        myCreditsLabel.setText("Crédits : " + credits);
    }

    public void updateOpponentCredits(int credits) {
        opponentCreditsLabel.setText("Crédits : " + credits);
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

        decal.setRotation(180f, 90f, 0);
        handCards.add(decal);
        decal.generateDynamicTexture(512, 716);
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
                card.animateFromDeck(deckTopPos, dest, angleX, -50f, 0f, 0.4f, true); //true if deck is from player, false if deck is from opponent
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
            BENCH_CARD_W, BENCH_CARD_H, cam, "deck"
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
        Vector3 deckTopPos = new Vector3(deckPos.x, deckPos.y + deckTop, deckPos.z);

        for (int i = 0; i < n; i++) {
            CardDecal card = opponentHandCards.get(i);
            float x = (i - center) * spacing;
            float y = 0.75f + (i - center) * THICKNESS;
            float z = (float) (-4.7f + (i - center) * THICKNESS / 1.5);
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
        if (card.id != null && card.id.startsWith("ACT-")) return actionsAtlas.findRegion(name);
        if (card.id != null && card.id.startsWith("OUT-")) return outilsAtlas.findRegion(name);
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

        CardDecal decal = removeOpponentCardFromField(card.getAtlasRegionName());
        if (decal !=null) decal.buildModel(decal.frontRegion, decal.backRegion, BENCH_CARD_W, BENCH_CARD_H);
        if (decal == null) {
            Vector3 startPos = opponentHandCards.size > 0
                ? opponentHandCards.get(opponentHandCards.size - 1).getPosition()
                : opponentDeck.getPosition();
            decal = new CardDecal(card, new TextureRegion(region),
                new TextureRegion(backTexture), BENCH_CARD_W, BENCH_CARD_H, cam, "bench");
            decal.setPosition(startPos.x, startPos.y, startPos.z);
            decal.setRotation(0, -90f, 0);
            if (opponentHandCards.size > 0) {
                CardDecal ghost = opponentHandCards.removeIndex(opponentHandCards.size - 1);
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

        CardDecal decal = removeOpponentCardFromField(card.getAtlasRegionName());

        if (decal != null) {
            decal.buildModel(decal.frontRegion, decal.backRegion, TABLE_CARD_W, TABLE_CARD_H);
        } else {
            Vector3 startPos = opponentHandCards.size > 0
                ? opponentHandCards.get(opponentHandCards.size - 1).getPosition()
                : opponentDeck.getPosition();
            decal = new CardDecal(card, new TextureRegion(region),
                new TextureRegion(backTexture), TABLE_CARD_W, TABLE_CARD_H, cam, "table");
            decal.setPosition(startPos.x, startPos.y, startPos.z);
            decal.setRotation(0, -90f, 0);
            if (opponentHandCards.size > 0) {
                CardDecal ghost = opponentHandCards.removeIndex(opponentHandCards.size - 1);
                ghost.dispose();
                repositionOpponentHand();
            }
        }
        decal.generateDynamicTexture(512, 716);
        opponentTableSlot.setCardDirect(decal);
        decal.animateTo(opponentTableSlot.getPosition(), 0, -90f, 0, 0.5f);
    }

    public CardDecal removeOpponentCardFromField(String cardId) {
        for (CardSlot slot : benchTopSlots) {
            CardDecal c = slot.getCard();
            if (c != null && c.getData() != null && c.getData().getAtlasRegionName().equals(cardId)) {
                slot.removeCard();
                return c;
            }
        }
        CardDecal c = opponentTableSlot.getCard();
        if (c != null && c.getData() != null && c.getData().getAtlasRegionName().equals(cardId)) {
            opponentTableSlot.removeCard();
            return c;
        }
        return null;
    }

    public int getBenchSlotIndex(CardSlot slot) {
        return benchBottomSlots.indexOf(slot, true);
    }

    public void updateDeckVisual(int size) {
        deck.updateSize(size);
    }

    public PerspectiveCamera getCam() {
        return cam;
    }

    public CardDecal getHoveredCard(Ray ray) {
        if (hoveredCard != null && hoveredCard.intersects(ray) && !hoveredCard.isAnimating())
            return hoveredCard;
        for (int i = handCards.size - 1; i >= 0; i--) {
            CardDecal card = handCards.get(i);
            if (card.intersects(ray) && !card.isAnimating()) return card;
        }
        for (CardSlot slot : benchBottomSlots) {
            CardDecal card = slot.getCard();
            if (card != null && card.intersects(ray)) return card;
        }
        for (CardSlot slot : benchTopSlots) { // ← ajout
            CardDecal card = slot.getCard();
            if (card != null && card.intersects(ray)) return card;
        }
        CardDecal slotCard = tableSlot.getCard();
        if (slotCard != null && slotCard.intersects(ray)) return slotCard;
        CardDecal oppSlotCard = opponentTableSlot.getCard(); // ← ajout
        if (oppSlotCard != null && oppSlotCard.intersects(ray)) return oppSlotCard;
        return null;
    }

    public void setHoveredCard(CardDecal card) {
        if (hoveredCard != null && hoveredCard != card) hoveredCard.setHovered(
            false
        );
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
        if (card.emplacement.equals("table")) draggedCard.rebuildWithDynamic(BENCH_CARD_W, BENCH_CARD_H);

        Vector3 pos = card.getPosition();
        card.setDragPosition(pos.x, 0.5f, pos.z);
        card.setRotation(0, -90f, 0);
        if (!card.emplacement.equals("bench")) {
            for (CardSlot slot : benchBottomSlots) {
                if (slot.isEmpty()) slot.setHighlighted(true);
            }
        }
        if (
            tableSlot.isEmpty() && !card.emplacement.equals("table")
        ) tableSlot.setHighlighted(true);
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
        if (!draggedCard.emplacement.equals("bench")) {
            for (CardSlot slot : benchBottomSlots) {
                if (slot.isEmpty()) slot.setHighlighted(true);
            }
        }
        if (
            tableSlot.isEmpty() && !draggedCard.emplacement.equals("table")
        ) tableSlot.setHighlighted(true);
    }

    public CardSlot getIntersectedSlot(Ray ray) {
        if (tableSlot.intersects(ray)) return tableSlot;

        for (CardSlot slot : benchBottomSlots) {
            if (slot.intersects(ray)) return slot;
        }

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
        } else {
            card.emplacement = "table";
        }
        card.animateTo(slot.getPosition(), 0, -90f, 0, 0.3f);
        for (CardSlot s : benchBottomSlots) s.setHighlighted(false);
        tableSlot.setHighlighted(false);
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
        draggedCard = null;
        originSlot = null;
    }

    public void showZoom(CardDecal card) {
        clearHover();
        if (zoomGhost != null) zoomGhost.dispose();
        zoomGhost = new CardDecal(
            card.getData(), card.frontRegion, card.backRegion,
            BENCH_CARD_W * 2.5f, BENCH_CARD_H * 2.5f, cam, "zoom"
        );
        if (card.getData() != null) zoomGhost.generateDynamicTexture(512, 716);
        zoomGhost.setPosition(0, 2f, 3.5f);
        Vector3 ghostPos = new Vector3(0, 1.75f, 4f);
        Vector3 toCam = new Vector3(cam.position).sub(ghostPos).nor();
        float pitch = (float) Math.toDegrees(Math.asin(toCam.y));
        zoomGhost.setRotation(0, -pitch+10, 0);
        hideActionButton();
        setupBanner.setVisible(false);
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
            if (model.phase == GameModel.Phase.DRAW){
                setupBanner.setVisible(true);
                actionButton.setVisible(true);
            } else if (model.myTurn){
                actionButton.setVisible(true);
            }
        }
    }

    public boolean isZooming() {
        return zoomGhost != null;
    }


    public CardDecal getMyTableCard() {
        return tableSlot.getCard();
    }

    public CardDecal getOpponentTableCard() {
        return opponentTableSlot.getCard();
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
        setupBanner.setVisible(false);
    }

}
