package io.github.squidecim.genialtcg.deckMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.mainMenu.FirstScreen;
import io.github.squidecim.genialtcg.model.CardsStackData;

public class DeckScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private Label alertLabel;

    public DeckScreen(GenialTCG game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = game.skin;

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        alertLabel = new Label("", skin);
        alertLabel.setColor(Color.ORANGE);

        Table topTable = new Table();
        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new FirstScreen(game));
                }
            }
        );
        topTable.add(btnBack).width(200).height(50).pad(10).left().expandX();
        root.add(topTable).expandX().fillX().top().row();

        root.add(alertLabel).height(30).padBottom(10).row();

        Label title = new Label("Mes Decks", skin);
        title.setFontScale(1.5f);
        root.add(title).padBottom(30).row();

        Table listTable = new Table();
        for (final CardsStackData deck : game.savedDecks) {
            final Stack stack = new Stack();
            TextButton db = new TextButton(deck.name, skin);
            db.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        game.setScreen(new NewDeckScreen(game, deck));
                    }
                }
            );

            final TextButton del = new TextButton("X", skin);
            del.setColor(Color.RED);
            del.setVisible(false);
            del.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        game.savedDecks.removeValue(deck, true);
                        game.saveDecks();
                        game.setScreen(new DeckScreen(game));
                    }
                }
            );

            stack.addListener(
                new InputListener() {
                    @Override
                    public void enter(
                        InputEvent event,
                        float x,
                        float y,
                        int pointer,
                        Actor fromActor
                    ) {
                        del.setVisible(true);
                    }

                    @Override
                    public void exit(
                        InputEvent event,
                        float x,
                        float y,
                        int pointer,
                        Actor toActor
                    ) {
                        del.setVisible(false);
                    }
                }
            );

            stack.add(db);
            stack.add(new Container<>(del).size(40).top().right());
            listTable.add(stack).width(250).height(350).pad(10);
        }

        TextButton btnNew = new TextButton("+", skin);
        btnNew.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (game.savedDecks.size < 4) {
                        game.setScreen(new NewDeckScreen(game));
                    } else {
                        showAlert("Vous ne pouvez avoir que 4 decks maximum");
                    }
                }
            }
        );
        listTable.add(btnNew).width(250).height(350).pad(10);

        ScrollPane scroll = new ScrollPane(listTable, skin);
        root.add(scroll).center().expand();
    }

    private void showAlert(String text) {
        alertLabel.setText(text);
        alertLabel.clearActions();
        alertLabel.addAction(
            Actions.sequence(
                Actions.alpha(1),
                Actions.fadeOut(3f),
                Actions.run(() -> alertLabel.setText(""))
            )
        );
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
    public void dispose() {
        stage.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
