package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.controller.GameController;
import io.github.squidecim.genialtcg.model.GameModel;
import io.github.squidecim.genialtcg.view.GameView;

public class FirstScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;

    public FirstScreen(GenialTCG game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("GénialTCG", skin);
        title.setFontScale(1.5f);
        TextButton btnPlay = new TextButton("Jouer", skin);
        TextButton btnDeck = new TextButton("Deck", skin);
        TextButton btnSettings = new TextButton("Paramètres", skin);
        TextButton btnQuit = new TextButton("Quitter", skin);

        btnPlay.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    System.out.println("Lancement du jeu...");

                    GameModel model = new GameModel();
                    GameView view = new GameView(game, model);
                    GameController controller = new GameController(view, model);
                    view.setController(controller);
                    game.setScreen(view);
                }
            }
        );

        btnDeck.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new DeckScreen(game));
                }
            }
        );

        btnSettings.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {}
            }
        );

        btnQuit.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Gdx.app.exit();
                }
            }
        );

        table.add(title).padBottom(40).row();
        table.add(btnPlay).width(220).height(50).pad(10).row();
        table.add(btnDeck).width(220).height(50).pad(10).row();
        table.add(btnSettings).width(220).height(50).pad(10).row();
        table.add(btnQuit).width(220).height(50).pad(10).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.1f, 0.2f, 1f);
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
