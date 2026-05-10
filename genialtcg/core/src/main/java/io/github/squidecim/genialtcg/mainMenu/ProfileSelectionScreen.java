package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;

import java.util.List;

public class ProfileSelectionScreen implements Screen {

    private static final int MAX_PROFILES = 4;

    private final GenialTCG game;
    private Stage stage;
    private Texture backgroundTexture;
    private Texture darkOverlayTexture;
    private SpriteBatch batch;
    private final Array<Texture> circleTextures = new Array<>();
    private final Array<Image> circleImages = new Array<>();
    private Dialog pseudoDialog;

    private boolean deletionMode = false;
    private TextButton btnDeleteToggle;
    private Label hintLabel;

    private static final Color TINT_DELETE    = new Color(1f, 0.15f, 0.15f, 1f);
    private static final Color TINT_HOVER_DEL = new Color(1f, 0.05f, 0.05f, 1f);

    private final Array<Color> originalColors = new Array<>();

    // Couleurs identiques aux boutons coups speciaux in-game (GameView)
    private static final Color[] CIRCLE_COLORS = {
        new Color(0.04f, 0.35f, 0.70f, 1f),
        new Color(0.44f, 0.15f, 0.04f, 1f),
        new Color(0.82f, 0.60f, 0.29f, 1f),
        new Color(0.47f, 0.47f, 0.47f, 1f),
        new Color(0.51f, 0.40f, 0.11f, 1f),
        new Color(0.10f, 0.10f, 0.30f, 1f),
    };

    public ProfileSelectionScreen(GenialTCG game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        Skin skin = game.skin;

        if (game.menuMusic == null) {
            try {
                game.menuMusic = Gdx.audio.newMusic(
                    Gdx.files.internal("audio/music/menu_theme.mp3")
                );
                game.menuMusic.setLooping(true);
                game.menuMusic.setVolume(GenialTCG.curveVolume(0.3f));
            } catch (Exception e) {
                Gdx.app.log("Audio", "Erreur lecture musique");
            }
        }
        if (game.menuMusic != null && !game.menuMusic.isPlaying()) {
            game.menuMusic.play();
        }

        backgroundTexture = new Texture(Gdx.files.internal("ui/fond/planete_menu.jpg"));
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0f, 0f, 0f, 0.4f));
        pixmap.fill();
        darkOverlayTexture = new Texture(pixmap);
        pixmap.dispose();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Titre
        Label title = new Label("Choisissez votre profil", skin, "title");
        title.setFontScale(0.3f);
        title.setAlignment(Align.center);
        root.add(title).padTop(60).padBottom(30).row();

        List<String> profiles = game.getSavedProfiles();

        // Cercles de profils
        if (!profiles.isEmpty()) {
            Table circlesRow = new Table();
            int circleSize = 130;

            for (int i = 0; i < profiles.size(); i++) {
                final String pseudo = profiles.get(i);
                final boolean locked = game.isProfileLocked(pseudo);
                final Color baseColor = locked
                    ? new Color(0.25f, 0.25f, 0.25f, 1f)
                    : new Color(CIRCLE_COLORS[i % CIRCLE_COLORS.length]);
                originalColors.add(baseColor);

                Texture circleTex = createCircleTexture(circleSize);
                circleTextures.add(circleTex);

                Image circleImg = new Image(circleTex);
                circleImg.setColor(baseColor);
                circleImages.add(circleImg);

                Label.LabelStyle labelStyle = new Label.LabelStyle(game.uiFont,
                    locked ? new Color(0.6f, 0.6f, 0.6f, 1f) : Color.WHITE);
                Label pseudoLabel = new Label(pseudo, labelStyle);
                pseudoLabel.setAlignment(Align.center);
                pseudoLabel.setWrap(true);

                Stack profileStack = new Stack();
                profileStack.setTransform(true);
                profileStack.add(circleImg);
                Table labelContainer = new Table();
                labelContainer.add(pseudoLabel).width(circleSize * 0.75f).center();
                if (locked) {
                    Label.LabelStyle lockedStyle = new Label.LabelStyle(game.uiFont, new Color(1f, 0.4f, 0.4f, 1f));
                    Label lockedLabel = new Label("En cours\nd'utilisation", lockedStyle);
                    lockedLabel.setAlignment(Align.center);
                    lockedLabel.setFontScale(0.75f);
                    labelContainer.row();
                    labelContainer.add(lockedLabel).width(circleSize * 0.75f).center();
                }
                profileStack.add(labelContainer);
                profileStack.setOrigin(circleSize / 2f, circleSize / 2f);

                final Stack finalStack = profileStack;
                final Image finalImg = circleImg;
                profileStack.addListener(new InputListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        if (locked) return;
                        if (pointer != -1) return;
                        if (fromActor != null && fromActor.isDescendantOf(finalStack)) return;
                        finalStack.clearActions();
                        finalStack.addAction(Actions.scaleTo(1.08f, 1.08f, 0.12f));
                        if (deletionMode) {
                            finalImg.setColor(TINT_HOVER_DEL);
                        } else {
                            finalImg.setColor(new Color(
                                Math.min(baseColor.r + 0.2f, 1f),
                                Math.min(baseColor.g + 0.2f, 1f),
                                Math.min(baseColor.b + 0.2f, 1f), 1f));
                            if (game.hoverSound != null) game.hoverSound.play(game.uiSoundVolume);
                        }
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        if (locked) return;
                        if (pointer != -1) return;
                        if (toActor != null && toActor.isDescendantOf(finalStack)) return;
                        finalStack.clearActions();
                        finalStack.addAction(Actions.scaleTo(1.0f, 1.0f, 0.12f));
                        finalImg.setColor(deletionMode ? TINT_DELETE : baseColor);
                    }

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                        return !locked;
                    }

                    @Override
                    public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                        if (locked) return;
                        if (game.clickSound != null) game.clickSound.play(game.uiSoundVolume * 0.15f);
                        if (deletionMode) {
                            game.deleteProfile(pseudo);
                            game.setScreen(new ProfileSelectionScreen(game));
                        } else {
                            game.loadProfile(pseudo);
                            game.setScreen(new MainScreen(game));
                        }
                    }
                });

                circlesRow.add(profileStack).size(circleSize).pad(20);
                if ((i + 1) % 4 == 0) circlesRow.row();
            }
            root.add(circlesRow).padBottom(20).row();
        }

        // Bouton nouveau profil (masque si 4 profils)
        if (profiles.size() < MAX_PROFILES) {
            TextButton btnNew = new TextButton("+ Nouveau profil", skin);
            game.soundifyButton(btnNew);
            btnNew.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    showPseudoDialog();
                }
            });
            root.add(btnNew).width(220).height(50).padTop(10).row();
        }

        // Bouton supprimer profil (seulement s'il y a des profils)
        if (!profiles.isEmpty()) {
            btnDeleteToggle = new TextButton("Supprimer un profil", skin);
            game.soundifyButton(btnDeleteToggle);
            btnDeleteToggle.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    toggleDeletionMode();
                }
            });
            root.add(btnDeleteToggle).width(220).height(50).padTop(8).row();

            Label.LabelStyle redStyle = new Label.LabelStyle(skin.get("default", Label.LabelStyle.class));
            redStyle.fontColor = new Color(1f, 0.25f, 0.25f, 1f);
            hintLabel = new Label("Cliquez sur un profil pour le supprimer", redStyle);
            hintLabel.setAlignment(Align.center);
            hintLabel.setVisible(false);
            root.add(hintLabel).padTop(6).row();
        }
    }

    private void toggleDeletionMode() {
        deletionMode = !deletionMode;
        btnDeleteToggle.setText(deletionMode ? "Annuler la suppression" : "Supprimer un profil");
        hintLabel.setVisible(deletionMode);
        for (int i = 0; i < circleImages.size; i++) {
            circleImages.get(i).setColor(deletionMode ? TINT_DELETE : originalColors.get(i));
        }
    }

    private void showPseudoDialog() {
        Skin skin = game.skin;
        pseudoDialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {}
        };

        pseudoDialog.getContentTable()
            .add(new Label("Choisissez un pseudo :", skin))
            .width(320).pad(10).row();

        TextField pseudoField = new TextField("", skin);
        pseudoField.setMaxLength(18);
        pseudoDialog.getContentTable().add(pseudoField).width(320).pad(10).row();

        Label errorLbl = new Label("", skin);
        errorLbl.setColor(Color.RED);
        pseudoDialog.getContentTable().add(errorLbl).pad(5).row();

        TextButton btnValider = new TextButton("Valider", skin);
        game.soundifyButton(btnValider);
        TextButton btnCancel = new TextButton("Annuler", skin);
        game.soundifyButton(btnCancel);

        Runnable valider = () -> {
            String pseudo = pseudoField.getText().trim();
            if (pseudo.isEmpty()) {
                errorLbl.setText("Le pseudo ne peut pas etre vide.");
                return;
            }
            if (game.getSavedProfiles().contains(pseudo)) {
                errorLbl.setText("Ce pseudo existe deja.");
                return;
            }
            game.playerPseudo = pseudo;
            game.saveProfile();
            game.lockProfile(pseudo);
            pseudoDialog.hide();
            game.setScreen(new MainScreen(game));
        };

        btnValider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                valider.run();
            }
        });
        btnCancel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                pseudoDialog.hide();
            }
        });
        pseudoField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    valider.run();
                    return true;
                }
                return false;
            }
        });
        pseudoDialog.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (x < 0 || x > pseudoDialog.getWidth() || y < 0 || y > pseudoDialog.getHeight()) {
                    pseudoDialog.hide();
                }
                return false;
            }
        });

        pseudoDialog.getButtonTable().defaults().width(130).height(40).pad(10);
        pseudoDialog.getButtonTable().add(btnCancel);
        pseudoDialog.getButtonTable().add(btnValider);
        pseudoDialog.setResizable(true);
        pseudoDialog.show(stage);
        stage.setKeyboardFocus(pseudoField);
    }

    private Texture createCircleTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0);
        pixmap.fill();
        pixmap.setColor(0.55f, 0.55f, 0.55f, 1f);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 1);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fillCircle(size / 2, size / 2, size / 2 - 5);
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        return tex;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.18f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (backgroundTexture != null) {
            batch.setProjectionMatrix(stage.getCamera().combined);
            batch.begin();
            float sw = stage.getViewport().getWorldWidth();
            float sh = stage.getViewport().getWorldHeight();
            float tw = backgroundTexture.getWidth();
            float th = backgroundTexture.getHeight();
            float scale = Math.max(sw / tw, sh / th);
            float dw = tw * scale;
            float dh = th * scale;
            batch.draw(backgroundTexture, (sw - dw) / 2f, (sh - dh) / 2f, dw, dh);
            if (darkOverlayTexture != null) batch.draw(darkOverlayTexture, 0, 0, sw, sh);
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
        if (darkOverlayTexture != null) darkOverlayTexture.dispose();
        if (batch != null) batch.dispose();
        for (Texture t : circleTextures) t.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
