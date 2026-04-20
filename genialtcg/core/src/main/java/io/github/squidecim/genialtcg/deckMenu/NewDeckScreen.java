package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.mainMenu.DeckScreen;

public class NewDeckScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private int cardCount = 0;
    private final int MAX_CARDS = 40;
    private Label counterLabel;
    private boolean[] occupied;

    public NewDeckScreen(GenialTCG game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // =========================
        // TOP BAR (RETOUR + VALIDER)
        // =========================
        Table topBar = new Table();
        topBar.left().top();

        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new DeckScreen(game));
                }
            }
        );

        TextButton btnValidate = new TextButton("Valider le deck", skin);
        btnValidate.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    System.out.println("Deck validé !");
                }
            }
        );
        // Compteur
        counterLabel = new Label("Cartes : 0 / " + MAX_CARDS, skin);
        if (cardCount == MAX_CARDS) {
            counterLabel.setColor(Color.RED);
        }

        // Layout top bar
        topBar.add(btnBack).left().pad(10);
        topBar.add(counterLabel).expandX().center();
        topBar.add(btnValidate).right().pad(10);

        root.add(topBar).expandX().fillX().colspan(2).row();

        Label title = new Label("Mon nouveau deck", skin);
        title.setFontScale(1.5f);
        root.add(title).colspan(2).pad(10).row();

        Table gridTable = new Table();
        gridTable.top().left();

        int columns = 8;
        int totalSlots = 400;
        occupied = new boolean[totalSlots];

        final float CARD_WIDTH = 320f;
        final float CARD_HEIGHT = 448f;
        float scale = 0.5f;

        for (int i = 0; i < totalSlots; i++) {
            Stack slot = new Stack();

            Image background = new Image(
                skin.newDrawable("white", Color.DARK_GRAY)
            );
            slot.add(background);

            final int index = i;
            slot.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                        com.badlogic.gdx.scenes.scene2d.InputEvent event,
                        float x,
                        float y
                    ) {
                        int button = event.getButton();

                        if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
                            if (!occupied[index] && cardCount < MAX_CARDS) {
                                occupied[index] = true;
                                cardCount++;

                                counterLabel.setText(
                                    "Cartes : " + cardCount + " / " + MAX_CARDS
                                );

                                // feedback visuel
                                slot.setColor(Color.GREEN);

                                System.out.println("Carte ajoutée : " + index);
                            }
                        }

                        if (button == com.badlogic.gdx.Input.Buttons.RIGHT) {
                            if (occupied[index]) {
                                occupied[index] = false;
                                cardCount--;

                                if (cardCount < 0) cardCount = 0;

                                counterLabel.setText(
                                    "Cartes : " + cardCount + " / " + MAX_CARDS
                                );
                                slot.setColor(Color.WHITE);

                                System.out.println("Carte retirée : " + index);
                            }
                        }
                    }
                }
            );

            gridTable
                .add(slot)
                .size(CARD_WIDTH * scale, CARD_HEIGHT * scale)
                .pad(5);

            if ((i + 1) % columns == 0) {
                gridTable.row();
            }
        }

        ScrollPane scrollPane = new ScrollPane(gridTable, skin);
        scrollPane.setFadeScrollBars(true);
        scrollPane.setScrollbarsOnTop(false);
        scrollPane.setScrollingDisabled(true, false);

        Table rightPanel = new Table();
        rightPanel.setBackground(
            skin.newDrawable("white", Color.valueOf("2b2b2b"))
        );

        root.add(scrollPane).expand().fill().pad(10);
        root.add(rightPanel).width(500).fillY().pad(10);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
