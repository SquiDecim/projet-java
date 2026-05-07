package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;

public class SettingsScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;

    public SettingsScreen(GenialTCG game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = game.skin;

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        //bouton retour en haut a gauche
        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new FirstScreen(game));
                }
            }
        );
        game.soundifyButton(btnBack);

        Table topBar = new Table();
        topBar.setFillParent(true);
        topBar.top().left();
        topBar.add(btnBack).width(200).height(50).pad(10);
        stage.addActor(topBar);

        // Récupération des paramètres sauvegardés
        Preferences prefs = Gdx.app.getPreferences("GenialTCG_Settings");

        // --- Volume Musique ---
        Label volumeLabel = new Label("Volume Musique", skin);
        volumeLabel.setColor(Color.WHITE);

        Slider volumeSlider = new Slider(0f, 1f, 0.05f, false, skin);
        volumeSlider.setValue(prefs.getFloat("music_volume", 0.3f));
        volumeSlider.setColor(Color.WHITE);
        volumeSlider.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    float volume = volumeSlider.getValue();
                    if (game.menuMusic != null) game.menuMusic.setVolume(volume);
                    prefs.putFloat("music_volume", volume);
                    prefs.flush();
                }
            }
        );

        // --- Volume Sons UI ---
        Label uiSoundLabel = new Label("Volume Sons UI", skin);
        uiSoundLabel.setColor(Color.WHITE);

        Slider uiSoundSlider = new Slider(0f, 1f, 0.05f, false, skin);
        uiSoundSlider.setValue(prefs.getFloat("ui_sound_volume", 0.5f));
        uiSoundSlider.setColor(Color.WHITE);
        uiSoundSlider.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    float volume = uiSoundSlider.getValue();
                    game.uiSoundVolume = volume;
                    prefs.putFloat("ui_sound_volume", volume);
                    prefs.flush();
                }
            }
        );

        // --- MISE EN PAGE ---
        table.add(volumeLabel).padBottom(10).row();
        table.add(volumeSlider).width(300).padBottom(40).row();
        table.add(uiSoundLabel).padBottom(10).row();
        table.add(uiSoundSlider).width(300).padBottom(40).row();
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
    public void hide() {
        dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}
}
