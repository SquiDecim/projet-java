package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap; // Import ajouté
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.deckMenu.DeckScreen;
import io.github.squidecim.genialtcg.network.LobbyCode;

public class FirstScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private Texture backgroundTexture;
    private Texture darkOverlayTexture; // Nouvelle texture pour assombrir le fond
    private SpriteBatch batch;
    private Label messageLabel;
    private String errorMessage = null;

    private Window errorDialog;
    private TextButton btnClose;
    private Dialog joinPartyDialog;
    private TextButton btnCancelDialog;
    private TextButton btnJoinDialog;

    public FirstScreen(GenialTCG game) {
        this.game = game;
    }

    public FirstScreen(GenialTCG game, String errorMessage) {
        this.game = game;
        this.errorMessage = errorMessage;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = game.skin;

        // gestion de la musique
        if (game.menuMusic == null) {
            try {
                game.menuMusic = Gdx.audio.newMusic(
                    Gdx.files.internal("audio/music/menu_theme.mp3")
                );
                game.menuMusic.setLooping(true);
                Preferences prefs = Gdx.app.getPreferences(
                    "GenialTCG_Settings"
                );
                game.menuMusic.setVolume(prefs.getFloat("music_volume", 0.3f));
            } catch (Exception e) {
                Gdx.app.log("Audio", "Erreur lecture musique");
            }
        }
        // joue la musique si elle n'est pas déjà en train de jouer
        if (game.menuMusic != null && !game.menuMusic.isPlaying()) {
            game.menuMusic.play();
        }

        backgroundTexture = new Texture(
            Gdx.files.internal("ui/fond/planete_menu.jpg")
        );

        // --- CREATION DU VOILE SOMBRE ---
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        // La couleur est noire (0,0,0) avec 40% d'opacité (0.4f). Tu peux ajuster ce 0.4f !
        pixmap.setColor(new Color(0f, 0f, 0f, 0.4f));
        pixmap.fill();
        darkOverlayTexture = new Texture(pixmap);
        pixmap.dispose();

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("GénialTCG", skin, "title");
        messageLabel = new Label("", skin);
        messageLabel.setColor(Color.ORANGE);

        TextButton btnCreateParty = new TextButton("Créer une Partie", skin);
        TextButton btnJoinParty = new TextButton("Rejoindre une Partie", skin);
        TextButton btnDeck = new TextButton("Deck", skin);
        TextButton btnSettings = new TextButton("Paramètres", skin);
        TextButton btnQuit = new TextButton("Quitter", skin);
        TextButton btnRules = new TextButton("Règles", skin);

        // --- ACTION : CRÉER PARTIE ---
        btnCreateParty.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (game.savedDecks.size > 0) {
                        game.setScreen(new LobbyScreen(game, true));
                    } else {
                        showEphemeralMessage(
                            "Constituez vous au moins un deck avant de jouer"
                        );
                    }
                }
            }
        );

        // --- ACTION : REJOINDRE PARTIE ---
        btnJoinParty.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (game.savedDecks.size == 0) {
                        showEphemeralMessage(
                            "Constituez vous au moins un deck avant de jouer"
                        );
                        return;
                    }
                    joinPartyDialog = new Dialog("", skin);
                    TextField codeField = new TextField("", skin);
                    codeField.setMessageText("Entre le code...");
                    Label errorLabel = new Label("", skin);
                    errorLabel.setColor(Color.RED);

                    Runnable tryJoin = () -> {
                        String code = codeField.getText().trim();
                        if (code.isEmpty()) {
                            errorLabel.setText("Entre un code !");
                            return;
                        }
                        try {
                            String ip = LobbyCode.decodeCode(code);
                            if (!LobbyCode.isReachable(ip)) {
                                errorLabel.setText(
                                    "Ce code ne mène à aucun lobby"
                                );
                                return;
                            }
                            joinPartyDialog.hide();
                            game.setScreen(
                                new LobbyScreen(game, false, ip, null)
                            );
                        } catch (Exception e) {
                            errorLabel.setText("Ce code ne mène à aucun lobby");
                        }
                    };

                    codeField.addListener(
                        new InputListener() {
                            @Override
                            public boolean keyDown(
                                InputEvent event,
                                int keycode
                            ) {
                                if (keycode == Input.Keys.ENTER) {
                                    if (
                                        game.clickSound != null
                                    ) game.clickSound.play(game.uiSoundVolume);
                                    tryJoin.run();
                                    return true;
                                }
                                if (keycode == Input.Keys.ESCAPE) {
                                    if (
                                        game.clickSound != null
                                    ) game.clickSound.play(game.uiSoundVolume);
                                    joinPartyDialog.hide();
                                    return true;
                                }
                                return false;
                            }
                        }
                    );

                    joinPartyDialog
                        .getContentTable()
                        .add(new Label("Code de la partie :", skin))
                        .pad(10)
                        .row();
                    joinPartyDialog
                        .getContentTable()
                        .add(codeField)
                        .width(300)
                        .pad(10)
                        .row();
                    joinPartyDialog
                        .getContentTable()
                        .add(errorLabel)
                        .pad(5)
                        .row();

                    btnCancelDialog = new TextButton("Annuler", skin);
                    btnCancelDialog.addListener(
                        new ChangeListener() {
                            @Override
                            public void changed(
                                ChangeEvent event,
                                Actor actor
                            ) {
                                joinPartyDialog.hide();
                            }
                        }
                    );

                    btnJoinDialog = new TextButton("Rejoindre", skin);
                    btnJoinDialog.addListener(
                        new ChangeListener() {
                            @Override
                            public void changed(
                                ChangeEvent event,
                                Actor actor
                            ) {
                                tryJoin.run();
                            }
                        }
                    );

                    game.soundifyButton(btnCancelDialog);
                    game.soundifyButton(btnJoinDialog);

                    joinPartyDialog
                        .getButtonTable()
                        .defaults()
                        .width(120)
                        .height(40)
                        .pad(10);
                    joinPartyDialog.getButtonTable().add(btnCancelDialog);
                    joinPartyDialog.getButtonTable().add(btnJoinDialog);
                    joinPartyDialog.show(stage);
                    stage.setKeyboardFocus(codeField);
                }
            }
        );

        // --- ACTION : RÈGLES ---
        btnRules.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new RulesScreen(game));
                }
            }
        );

        // --- ACTION : DECK ---
        btnDeck.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new DeckScreen(game));
                }
            }
        );

        // --- ACTION : PARAMÈTRES ---
        btnSettings.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new SettingsScreen(game));
                }
            }
        );

        // --- ACTION : QUITTER ---
        btnQuit.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (game.menuMusic != null) game.menuMusic.dispose();
                    Gdx.app.exit();
                }
            }
        );

        game.soundifyButton(btnCreateParty);
        game.soundifyButton(btnJoinParty);
        game.soundifyButton(btnDeck);
        game.soundifyButton(btnRules);
        game.soundifyButton(btnSettings);
        game.soundifyButton(btnQuit);

        // --- MISE EN PAGE ---
        table.add(title).padBottom(20).row();
        table.add(messageLabel).height(30).padBottom(10).row();
        table.add(btnCreateParty).width(220).height(50).pad(10).row();
        table.add(btnJoinParty).width(220).height(50).pad(10).row();
        table.add(btnDeck).width(220).height(50).pad(10).row();
        table.add(btnRules).width(220).height(50).pad(10).row();
        table.add(btnSettings).width(220).height(50).pad(10).row();
        table.add(btnQuit).width(220).height(50).pad(10).row();

        if (errorMessage != null) {
            errorDialog = new Window("", skin);
            errorDialog.setModal(true);
            errorDialog.setMovable(false);
            float dw = 300,
                dh = 150;
            float dx = (stage.getWidth() - dw) / 2f;
            float dy = (stage.getHeight() - dh) / 2f;
            errorDialog.setSize(dw, dh);
            errorDialog.setPosition(dx, dy);
            errorDialog
                .add(new Label(errorMessage, skin))
                .expand()
                .center()
                .pad(20);
            btnClose = new TextButton("X", skin);
            btnClose.setSize(30, 30);
            btnClose.setPosition(dx + dw - 30, dy + dh - 30);
            btnClose.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        errorDialog.remove();
                        btnClose.remove();
                    }
                }
            );
            game.soundifyButton(btnClose);
            stage.addActor(errorDialog);
            stage.addActor(btnClose);
        }
    }

    private void showEphemeralMessage(String text) {
        game.playImpossibleSound();
        messageLabel.setText(text);
        messageLabel.clearActions();
        messageLabel.addAction(
            Actions.sequence(
                Actions.alpha(1),
                Actions.fadeOut(3f),
                Actions.run(() -> messageLabel.setText(""))
            )
        );
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.18f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (backgroundTexture != null) {
            batch.begin();
            // 1. Dessine le fond d'écran
            batch.draw(
                backgroundTexture,
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
            );

            if (darkOverlayTexture != null) {
                batch.draw(
                    darkOverlayTexture,
                    0,
                    0,
                    Gdx.graphics.getWidth(),
                    Gdx.graphics.getHeight()
                );
            }
            batch.end();
        }

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
        if (backgroundTexture != null) backgroundTexture.dispose();
        if (darkOverlayTexture != null) darkOverlayTexture.dispose(); // Nettoyage
        if (batch != null) batch.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
