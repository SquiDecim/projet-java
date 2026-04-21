package io.github.squidecim.genialtcg.deckMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.mainMenu.FirstScreen;

public class DeckScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;

    public DeckScreen(GenialTCG game) {
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
        Table topTable = new Table();
        topTable.top().left();
        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new FirstScreen(game));
                }
            }
        );
        topTable.add(btnBack).width(250).height(60).pad(20);
        root.add(topTable).expandX().fillX().top().row();

        // TITRE
        Label title = new Label("Mes Decks", skin);
        title.setFontScale(1.5f);
        root.add(title).padBottom(30).row();

        // CONTENEUR DES DECKS
        Table deckListTable = new Table();

        for (int i = 1; i <= 8; i++) {
            deckListTable
                .add(createDeckSlot("Deck " + i, false))
                .width(320)
                .height(448)
                .pad(15);
        }

        deckListTable
            .add(createDeckSlot("+", true))
            .width(320)
            .height(448)
            .pad(15);

        // CONFIGURATION DU SCROLLPANE
        ScrollPane scroll = new ScrollPane(deckListTable, skin);

        scroll.setScrollingDisabled(false, true);
        scroll.setFlickScroll(true);
        scroll.setScrollBarPositions(false, false);
        scroll.getStyle().hScroll = null;
        scroll.getStyle().hScrollKnob = null;

        scroll.addListener(
            new ClickListener() {
                @Override
                public void enter(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor fromActor
                ) {
                    stage.setScrollFocus(scroll);
                }

                @Override
                public void exit(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor toActor
                ) {
                    stage.setScrollFocus(null);
                }
            }
        );

        root.add(scroll).expand().fillX().center();
    }

    private Button createDeckSlot(String text, boolean isNewDeckButton) {
        TextButton slot = new TextButton(text, skin);
        slot.getLabel().setFontScale(1.2f);

        if (isNewDeckButton) {
            slot.setColor(Color.LIGHT_GRAY);
            slot.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        game.setScreen(new NewDeckScreen(game));
                    }
                }
            );
        } else {
            slot.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        System.out.println("Deck sélectionné : " + text);
                    }
                }
            );
        }
        return slot;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.18f, 1f);
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
