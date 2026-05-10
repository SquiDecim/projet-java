package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import java.util.List;

public class SettingsScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private Dialog changePseudoDialog;

    public SettingsScreen(GenialTCG game) {
        this.game = game;
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

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = game.skin;

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new MainScreen(game));
                }
            }
        );
        game.soundifyButton(btnBack);

        Table topBar = new Table();
        topBar.setFillParent(true);
        topBar.top().left();
        topBar.add(btnBack).width(200).height(50).pad(10);
        stage.addActor(topBar);

        Preferences prefs = Gdx.app.getPreferences("GenialTCG_Profile_" + game.playerPseudo);
        String savedDisplayMode = prefs.getString(
            "display_mode",
            "Plein ecran"
        );

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
                    if (game.menuMusic != null) game.menuMusic.setVolume(
                        GenialTCG.curveVolume(volume)
                    );
                    prefs.putFloat("music_volume", volume);
                    prefs.flush();
                }
            }
        );

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
                    game.uiSoundVolume = GenialTCG.curveVolume(volume);
                    prefs.putFloat("ui_sound_volume", volume);
                    prefs.flush();
                }
            }
        );

        Label gameSoundLabel = new Label("Game Sound", skin);
        gameSoundLabel.setColor(Color.WHITE);

        Slider gameSoundSlider = new Slider(0f, 1f, 0.05f, false, skin);
        gameSoundSlider.setValue(prefs.getFloat("game_sound_volume", 0.5f));
        gameSoundSlider.setColor(Color.WHITE);
        gameSoundSlider.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    float volume = gameSoundSlider.getValue();
                    game.gameSoundVolume = GenialTCG.curveVolume(volume);
                    game.updateTerrainMusicVolume();
                    prefs.putFloat("game_sound_volume", volume);
                    prefs.flush();
                }
            }
        );

        Label brightnessLabel = new Label("Luminosite", skin);
        brightnessLabel.setColor(Color.WHITE);

        Slider brightnessSlider = new Slider(0.2f, 1.0f, 0.05f, false, skin);
        brightnessSlider.setValue(game.globalBrightness);
        brightnessSlider.setColor(Color.WHITE);
        brightnessSlider.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.globalBrightness = brightnessSlider.getValue();
                    prefs.putFloat("brightness", game.globalBrightness);
                    prefs.flush();
                }
            }
        );

        Label displayLabel = new Label("Mode d'affichage", skin);
        displayLabel.setColor(Color.WHITE);

        SelectBox<String> displayBox = new SelectBox<>(skin);
        displayBox.setItems("Plein ecran", "Fenetre sans bordure", "Fenetre");
        displayBox.setSelected(savedDisplayMode);

        applyDisplayMode(savedDisplayMode);

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

        TextButton btnChangeProfil = new TextButton("Changer de profil", skin);
        game.soundifyButton(btnChangeProfil);
        btnChangeProfil.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.playerPseudo = "";
                    game.savedDecks.clear();
                    game.setScreen(new ProfileSelectionScreen(game));
                }
            }
        );

        TextButton btnChangePseudo = new TextButton("Changer de pseudo", skin);
        game.soundifyButton(btnChangePseudo);
        btnChangePseudo.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    showChangePseudoDialog();
                }
            }
        );

        table.add(volumeLabel).padBottom(5).row();
        table.add(volumeSlider).width(300).padBottom(15).row();
        table.add(uiSoundLabel).padBottom(5).row();
        table.add(uiSoundSlider).width(300).padBottom(15).row();
        table.add(gameSoundLabel).padBottom(5).row();
        table.add(gameSoundSlider).width(300).padBottom(15).row();
        table.add(brightnessLabel).padBottom(5).row();
        table.add(brightnessSlider).width(300).padBottom(15).row();
        table.add(displayLabel).padBottom(5).row();
        table.add(displayBox).width(300).padBottom(25).row();
        table.add(btnChangePseudo).width(300).height(50).padBottom(10).row();
        table.add(btnChangeProfil).width(300).height(50).padBottom(15).row();
    }

    private void showChangePseudoDialog() {
        changePseudoDialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {}
        };

        String current = game.playerPseudo.isEmpty()
            ? "(aucun)"
            : game.playerPseudo;
        changePseudoDialog
            .getContentTable()
            .add(new Label("Pseudo actuel : " + current, skin))
            .width(320)
            .pad(10)
            .row();
        changePseudoDialog
            .getContentTable()
            .add(new Label("Nouveau pseudo :", skin))
            .width(320)
            .padTop(5)
            .padLeft(10)
            .padRight(10)
            .row();

        TextField pseudoField = new TextField("", skin);
        pseudoField.setMaxLength(18);
        changePseudoDialog
            .getContentTable()
            .add(pseudoField)
            .width(320)
            .pad(10)
            .row();

        Label errorLbl = new Label("", skin);
        errorLbl.setColor(Color.RED);
        changePseudoDialog.getContentTable().add(errorLbl).pad(5).row();

        TextButton btnValider = new TextButton("Valider", skin);
        game.soundifyButton(btnValider);
        TextButton btnCancel = new TextButton("Annuler", skin);
        game.soundifyButton(btnCancel);

        Runnable valider = () -> {
            String newPseudo = pseudoField.getText().trim();
            if (newPseudo.isEmpty()) {
                errorLbl.setText("Le pseudo ne peut pas etre vide.");
                return;
            }
            if (newPseudo.equals(game.playerPseudo)) {
                changePseudoDialog.hide();
                return;
            }
            List<String> existing = game.getSavedProfiles();
            if (existing.contains(newPseudo)) {
                errorLbl.setText("Ce pseudo est deja utilise.");
                return;
            }
            game.renameProfile(game.playerPseudo, newPseudo);
            changePseudoDialog.hide();
        };

        btnValider.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    valider.run();
                }
            }
        );
        btnCancel.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    changePseudoDialog.hide();
                }
            }
        );
        pseudoField.addListener(
            new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ENTER) {
                        valider.run();
                        return true;
                    }
                    return false;
                }
            }
        );
        changePseudoDialog.addListener(
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
                        x > changePseudoDialog.getWidth() ||
                        y < 0 ||
                        y > changePseudoDialog.getHeight()
                    ) {
                        changePseudoDialog.hide();
                    }
                    return false;
                }
            }
        );

        changePseudoDialog
            .getButtonTable()
            .defaults()
            .width(130)
            .height(40)
            .pad(10);
        changePseudoDialog.getButtonTable().add(btnCancel);
        changePseudoDialog.getButtonTable().add(btnValider);
        changePseudoDialog.setResizable(true);
        changePseudoDialog.show(stage);
        stage.setKeyboardFocus(pseudoField);
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
