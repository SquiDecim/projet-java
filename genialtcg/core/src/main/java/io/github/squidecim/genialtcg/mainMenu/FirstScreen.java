package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import io.github.squidecim.genialtcg.deckMenu.DeckScreen;
import io.github.squidecim.genialtcg.network.LobbyCode;

public class FirstScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private Label messageLabel;
    private String errorMessage = null;

    //Dialog retour au menu
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
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label title = new Label("GénialTCG", skin);
        title.setFontScale(1.5f);

        messageLabel = new Label("", skin);
        messageLabel.setColor(Color.ORANGE);

        TextButton btnCreateParty = new TextButton("Créer une Partie", skin);
        TextButton btnJoinParty = new TextButton("Rejoindre une Partie", skin);
        TextButton btnDeck = new TextButton("Deck", skin);
        TextButton btnSettings = new TextButton("Paramètres", skin);
        TextButton btnQuit = new TextButton("Quitter", skin);

        btnCreateParty.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (game.savedDecks.size > 0) {
                        //GameModel model = new GameModel(game);
                        //GameView view = new GameView(game, model);
                        //view.setController(new GameController(view, model));
                        LobbyScreen lobby = new LobbyScreen(game, true);
                        game.setScreen(lobby);
                    } else {
                        showEphemeralMessage(
                            "Constituez vous au moins un deck avant de jouer"
                        );
                    }
                }
            }
        );

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

                    // logique de connexion partagée entre bouton et touche Entrée
                    Runnable tryJoin = () -> {
                        String code = codeField.getText().trim();
                        if (code.isEmpty()) {
                            errorLabel.setText("Entre un code !");
                            return;
                        }
                        try {
                            String ip = LobbyCode.decodeCode(code);
                            if (!LobbyCode.isReachable(ip)) {
                                errorLabel.setText("Ce code ne mène à aucun lobby");
                                return;
                            }
                            joinPartyDialog.hide();
                            game.setScreen(new LobbyScreen(game, false, ip, null));
                        } catch (Exception e) {
                            errorLabel.setText("Ce code ne mène à aucun lobby");
                        }
                    };

                    // Entrée = valider, Échap = fermer
                    codeField.addListener(new InputListener() {
                        @Override
                        public boolean keyDown(InputEvent event, int keycode) {
                            if (keycode == Input.Keys.ENTER) { tryJoin.run(); return true; }
                            if (keycode == Input.Keys.ESCAPE) { joinPartyDialog.hide(); return true; }
                            return false;
                        }
                    });

                    joinPartyDialog.getContentTable()
                        .add(new Label("Code de la partie :", skin)).pad(10).row();
                    joinPartyDialog.getContentTable()
                        .add(codeField).width(300).pad(10).row();
                    joinPartyDialog.getContentTable()
                        .add(errorLabel).pad(5).row();

                    btnCancelDialog = new TextButton("Annuler", skin);
                    btnCancelDialog.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            joinPartyDialog.hide();
                        }
                    });

                    btnJoinDialog = new TextButton("Rejoindre", skin);
                    btnJoinDialog.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            tryJoin.run();
                        }
                    });

                    joinPartyDialog.getButtonTable().defaults().width(120).height(40).pad(10);
                    joinPartyDialog.getButtonTable().add(btnCancelDialog);
                    joinPartyDialog.getButtonTable().add(btnJoinDialog);
                    joinPartyDialog.show(stage);
                    stage.setKeyboardFocus(codeField);
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
                public void changed(ChangeEvent event, Actor actor) {
                    showEphemeralMessage("Paramètres — bientôt disponible");
                }
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

        table.add(title).padBottom(20).row();
        table.add(messageLabel).height(30).padBottom(10).row();
        table.add(btnCreateParty).width(220).height(50).pad(10).row();
        table.add(btnJoinParty).width(220).height(50).pad(10).row();
        table.add(btnDeck).width(220).height(50).pad(10).row();
        table.add(btnSettings).width(220).height(50).pad(10).row();
        table.add(btnQuit).width(220).height(50).pad(10).row();

        if (errorMessage != null) {
            errorDialog = new Window("", skin);
            errorDialog.setModal(true);
            errorDialog.setMovable(false);

            float dw = 300, dh = 150;
            float dx = (stage.getWidth() - dw) / 2f;
            float dy = (stage.getHeight() - dh) / 2f;

            errorDialog.setSize(dw, dh);
            errorDialog.setPosition(dx, dy);

            errorDialog.add(new Label(errorMessage, skin))
                .expand().center().pad(20);

            btnClose = new TextButton("X", skin);
            btnClose.setSize(30, 30);
            btnClose.setPosition(dx + dw - 30, dy + dh - 30); // coin haut-droit

            btnClose.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    errorDialog.remove();
                    btnClose.remove();
                }
            });

            stage.addActor(errorDialog);
            stage.addActor(btnClose);
        }
    }

    private void showEphemeralMessage(String text) {
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
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (errorDialog != null && errorDialog.hasParent()) {
            float dx = (stage.getWidth() - errorDialog.getWidth()) / 2f;
            float dy = (stage.getHeight() - errorDialog.getHeight()) / 2f;
            errorDialog.setPosition(dx, dy);
            btnClose.setPosition(dx + errorDialog.getWidth() - 30, dy + errorDialog.getHeight() - 30);
        }
        if (joinPartyDialog != null && joinPartyDialog.hasParent()) {
            float dx = (stage.getWidth() - joinPartyDialog.getWidth()) / 2f;
            float dy = (stage.getHeight() - joinPartyDialog.getHeight()) / 2f;
            joinPartyDialog.setPosition(dx, dy);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
