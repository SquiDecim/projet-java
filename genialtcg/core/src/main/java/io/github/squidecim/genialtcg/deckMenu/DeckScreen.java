package io.github.squidecim.genialtcg.deckMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
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

        // --- BARRE DU HAUT ---
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

        topTable.add(btnBack).width(200).height(50).pad(10);
        root.add(topTable).expandX().fillX().top().row();

        Label title = new Label("Mes Decks", skin);
        title.setFontScale(1.5f);
        root.add(title).padBottom(30).row();

        // --- CONTENEUR DES DECKS ---
        Table deckListTable = new Table();
        for (Deck deck : game.savedDecks) {
            deckListTable
                .add(createDeckSlot(deck.name, false))
                .width(320)
                .height(448)
                .pad(15);
        }

        // Le bouton + est TOUJOURS ajouté
        Button plusButton = createDeckSlot("+", true);

        // Mais il est grisé et désactivé si on a déjà 4 decks ou plus
        if (game.savedDecks.size >= 4) {
            plusButton.setDisabled(true);
            plusButton.setColor(Color.GRAY);
        }

        deckListTable.add(plusButton).width(320).height(448).pad(15);

        ScrollPane scroll = new ScrollPane(deckListTable, skin);
        scroll.setScrollingDisabled(false, true);
        root.add(scroll).expand().fillX().center();
    }

    private Button createDeckSlot(final String text, boolean isNewDeckButton) {
        TextButton slot = new TextButton(text, skin);
        slot.getLabel().setFontScale(1.2f);

        if (isNewDeckButton) {
            slot.setColor(Color.LIGHT_GRAY);
            slot.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        // On vérifie quand même avant de changer d'écran
                        if (game.savedDecks.size < 4) {
                            game.setScreen(new NewDeckScreen(game));
                        }
                    }
                }
            );
        } else {
            slot.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        showDeckOptions(text);
                    }
                }
            );
        }
        return slot;
    }

    private void showDeckOptions(final String currentName) {
        final Dialog dialog = new Dialog("Options", skin);
        TextButton btnRename = new TextButton("Renommer", skin);
        TextButton btnDelete = new TextButton("Supprimer", skin);

        btnDelete.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    for (int i = 0; i < game.savedDecks.size; i++) {
                        if (game.savedDecks.get(i).name.equals(currentName)) {
                            game.savedDecks.removeIndex(i);
                            break;
                        }
                    }
                    game.setScreen(new DeckScreen(game));
                }
            }
        );

        btnRename.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    dialog.hide();
                    showRenameDialog(currentName);
                }
            }
        );

        dialog
            .getContentTable()
            .add(new Label("Deck : " + currentName, skin))
            .pad(20);
        dialog.getButtonTable().add(btnRename).pad(10);
        dialog.getButtonTable().add(btnDelete).pad(10);
        dialog.button("Annuler");
        dialog.show(stage);
    }

    private void showRenameDialog(final String oldName) {
        final Dialog renameDialog = new Dialog("Renommer", skin);
        final TextField input = new TextField(oldName, skin);
        input.setMaxLength(20);

        TextButton btnConfirm = new TextButton("OK", skin);
        btnConfirm.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    String newName = input.getText().trim();
                    if (!newName.isEmpty()) {
                        for (Deck d : game.savedDecks) {
                            if (d.name.equals(oldName)) {
                                d.name = newName;
                                break;
                            }
                        }
                        game.setScreen(new DeckScreen(game));
                    }
                }
            }
        );

        renameDialog
            .getContentTable()
            .add(new Label("Nouveau nom :", skin))
            .pad(10)
            .row();
        renameDialog.getContentTable().add(input).width(300).pad(20);
        renameDialog.getButtonTable().add(btnConfirm).pad(10);
        renameDialog.show(stage);
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
        skin.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
