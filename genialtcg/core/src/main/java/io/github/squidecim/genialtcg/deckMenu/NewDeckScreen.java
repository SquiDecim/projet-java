package io.github.squidecim.genialtcg.deckMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;

public class NewDeckScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private Texture atlasTexture;

    private final int SCALE = 2;
    private final int CARD_WIDTH = 320 * SCALE;
    private final int CARD_HEIGHT = 448 * SCALE;
    private final int PADDING = 20 * SCALE;
    private final int COLUMN_COUNT = 10;

    private int cardCount = 0;
    private final int MAX_CARDS = 40;
    private Label counterLabel;
    private TextButton btnValidate;
    private boolean[] occupied;

    public NewDeckScreen(GenialTCG game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        atlasTexture = new Texture(Gdx.files.internal("cards/atlas_pays.png"));
        atlasTexture.setFilter(
            Texture.TextureFilter.Linear,
            Texture.TextureFilter.Linear
        );

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // --- BARRE SUPÉRIEURE ---
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
        btnValidate.setDisabled(true); // Désactivé par défaut
        btnValidate.setColor(Color.GRAY);
        btnValidate.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    System.out.println("Deck validé !");
                    // Logique de sauvegarde ici
                }
            }
        );

        topBar.add(btnBack).pad(10);
        topBar.add(counterLabel).expandX().center();
        topBar.add(btnValidate).pad(10);
        root.add(topBar).expandX().fillX().row();

        // --- GRILLE DE CARTES ---
        Table gridTable = new Table();
        gridTable.center(); // Centre le contenu de la table

        int totalCards = 196;
        occupied = new boolean[totalCards];
        float displayScale = 0.4f;

        for (int i = 0; i < totalCards; i++) {
            final int index = i;

            // Calcul des coordonnées dans l'atlas
            int col = i % COLUMN_COUNT;
            int row = i / COLUMN_COUNT;
            int srcX = PADDING + (col * CARD_WIDTH);
            int srcY = PADDING + (row * CARD_HEIGHT);

            TextureRegion region = new TextureRegion(
                atlasTexture,
                srcX,
                srcY,
                CARD_WIDTH,
                CARD_HEIGHT
            );

            Stack slot = new Stack();
            Image cardImg = new Image(region);
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
                        toggleCardSelection(index, selectionOverlay);
                    }
                }
            );

            gridTable
                .add(slot)
                .size(CARD_WIDTH * displayScale, CARD_HEIGHT * displayScale)
                .pad(10);
            if ((i + 1) % 6 == 0) gridTable.row();
        }

        ScrollPane scroll = new ScrollPane(gridTable, skin);

        // 1. DÉSACTIVER L'AFFICHAGE DES BARRES
        scroll.setScrollingDisabled(false, false); // Permet le scroll horizontal/vertical
        scroll.setScrollBarPositions(false, false); // Ne pas réserver d'espace pour les barres
        scroll.setFadeScrollBars(false); // Empêche l'animation d'apparition

        // On peut aussi dire au style de ne rien dessiner pour les barres
        scroll.getStyle().vScroll = null;
        scroll.getStyle().vScrollKnob = null;

        // 2. RÉPARER LA MOLETTE DE LA SOURIS
        // On force le focus sur le ScrollPane dès qu'on survole la grille
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

    private void toggleCardSelection(int index, Image overlay) {
        if (occupied[index]) {
            // Désélectionner
            occupied[index] = false;
            cardCount--;
            overlay.setVisible(false);
        } else {
            // Sélectionner (si pas complet)
            if (cardCount < MAX_CARDS) {
                occupied[index] = true;
                cardCount++;
                overlay.setVisible(true);
            }
        }
        updateUI();
    }

    private void updateUI() {
        counterLabel.setText("Cartes : " + cardCount + " / " + MAX_CARDS);

        // Gestion de l'état du bouton Valider
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
        if (atlasTexture != null) atlasTexture.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
