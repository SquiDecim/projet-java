package io.github.squidecim.genialtcg.deckMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;

public class NewDeckScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private TextureAtlas atlas;

    private int cardCount = 0;
    private final int MAX_CARDS = 40;
    private Label counterLabel;
    private TextButton btnValidate;

    private Array<String> selectedCards;

    public NewDeckScreen(GenialTCG game) {
        this.game = game;
        this.selectedCards = new Array<>();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        // CHARGEMENT DE L'ATLAS
        atlas = new TextureAtlas(
            Gdx.files.internal("cards/full/country_cards.atlas")
        );

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // BARRE SUPÉRIEURE
        Table topBar = new Table();
        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new DeckScreen(game));
                }
            }
        );

        counterLabel = new Label("Cartes : 0 / " + MAX_CARDS, skin);
        btnValidate = new TextButton("Valider le Deck", skin);
        btnValidate.setDisabled(true);
        btnValidate.setColor(Color.GRAY);

        topBar.add(btnBack).width(250).height(60).pad(10);
        topBar.add(counterLabel).expandX().center();
        topBar.add(btnValidate).width(250).height(60).pad(10);
        root.add(topBar).expandX().fillX().row();

        // GRILLE DE CARTES
        Table gridTable = new Table();
        gridTable.center();

        float displayScale = 0.4f;

        // RÉCUPÉRATION DE TOUTES LES RÉGIONS
        Array<AtlasRegion> allRegions = atlas.getRegions();

        for (int i = 0; i < allRegions.size; i++) {
            AtlasRegion region = allRegions.get(i);
            final String countryId = region.name;

            Stack slot = new Stack();
            Image cardImg = new Image(region);

            // Overlay de sélection
            Image selectionOverlay = new Image(
                skin.newDrawable("white", new Color(0, 1, 0, 0.4f))
            );
            selectionOverlay.setVisible(false);

            slot.add(cardImg);
            slot.add(selectionOverlay);

            slot.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                        com.badlogic.gdx.scenes.scene2d.InputEvent event,
                        float x,
                        float y
                    ) {
                        toggleCardSelection(countryId, selectionOverlay);
                    }
                }
            );

            // On ajoute à la grille
            gridTable
                .add(slot)
                .size(
                    region.getRegionWidth() * displayScale,
                    region.getRegionHeight() * displayScale
                )
                .pad(10);

            if ((i + 1) % 6 == 0) gridTable.row();
        }

        ScrollPane scroll = new ScrollPane(gridTable, skin);
        scroll.addListener(
            new ClickListener() {
                @Override
                public void enter(
                    com.badlogic.gdx.scenes.scene2d.InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor fromActor
                ) {
                    stage.setScrollFocus(scroll);
                }

                @Override
                public void exit(
                    com.badlogic.gdx.scenes.scene2d.InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor toActor
                ) {
                    stage.setScrollFocus(null);
                }
            }
        );

        root.add(scroll).expand().fill().pad(10);
    }

    private void toggleCardSelection(String countryId, Image overlay) {
        if (selectedCards.contains(countryId, false)) {
            selectedCards.removeValue(countryId, false);
            overlay.setVisible(false);
        } else {
            if (selectedCards.size < MAX_CARDS) {
                selectedCards.add(countryId);
                overlay.setVisible(true);
            }
        }
        updateUI();
    }

    private void updateUI() {
        cardCount = selectedCards.size;
        counterLabel.setText("Cartes : " + cardCount + " / " + MAX_CARDS);

        boolean isFull = (cardCount == MAX_CARDS);
        btnValidate.setDisabled(!isFull);

        if (isFull) {
            btnValidate.setColor(Color.GREEN);
            counterLabel.setColor(Color.GOLD);
        } else {
            btnValidate.setColor(Color.WHITE);
            counterLabel.setColor(Color.WHITE);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.16f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        if (atlas != null) atlas.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
