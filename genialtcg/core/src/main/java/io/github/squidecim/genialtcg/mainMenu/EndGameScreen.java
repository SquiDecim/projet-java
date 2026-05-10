package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;

public class EndGameScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private final boolean isVictory;
    private final int myPoints;
    private final int opponentPoints;

    public EndGameScreen(
        GenialTCG game,
        boolean isVictory,
        int myPoints,
        int opponentPoints
    ) {
        this.game = game;
        this.isVictory = isVictory;
        this.myPoints = myPoints;
        this.opponentPoints = opponentPoints;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = game.skin;

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label(
            isVictory ? "VICTOIRE !" : "DEFAITE...",
            skin,
            "title"
        );
        title.setFontScale(0.6f);
        title.setColor(isVictory ? Color.GOLD : Color.RED);

        Label scoreLabel = new Label("Score Final", skin);
        scoreLabel.setFontScale(1.5f);

        Label detailsLabel = new Label(
            "Vos points : " +
                myPoints +
                " | Points adverses : " +
                opponentPoints,
            skin
        );

        TextButton btnMenu = new TextButton("Retour au menu principal", skin);
        btnMenu.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new MainScreen(game));
                }
            }
        );
        game.soundifyButton(btnMenu);

        table.add(title).padBottom(50).row();
        table.add(scoreLabel).padBottom(10).row();
        table.add(detailsLabel).padBottom(40).row();
        table.add(btnMenu).width(350).height(60);
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
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
