package io.github.squidecim.genialtcg.mainMenu;

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

        // --- Bouton Retour ---
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
        topTable.add(btnBack).pad(20);
        root.add(topTable).expandX().fillX().top().row();

        // --- Titre ---
        Label title = new Label("Mes Decks", skin);
        title.setFontScale(1.5f);
        root.add(title).padBottom(30).row();

        // --- Conteneur des Decks (Zone centrale) ---
        Table deckListTable = new Table();
        deckListTable.center();

        // Simulation de decks existants (à remplacer par ta logique de sauvegarde plus tard)
        for (int i = 1; i <= 8; i++) {
            deckListTable.add(createDeckSlot("Deck " + i, false)).pad(15);
        }

        // Le slot "+" pour créer un nouveau deck
        deckListTable.add(createDeckSlot("+", true)).pad(15);

        // ScrollPane pour permettre le défilement horizontal si trop de decks
        ScrollPane scroll = new ScrollPane(deckListTable, skin);
        scroll.setFadeScrollBars(false);

        // On place le rectangle au milieu
        root.add(scroll).expand().fillX().center();
    }

    /**
     * Crée un visuel de "carte" pour représenter un deck
     */
    private Button createDeckSlot(String text, boolean isNewDeckButton) {
        // On utilise un TextButton comme base pour le rectangle
        TextButton slot = new TextButton(text, skin);

        // On force une taille de rectangle
        slot.getLabel().setFontScale(1.2f);

        if (isNewDeckButton) {
            slot.setColor(Color.LIGHT_GRAY); // Teinte différente pour le "+"
            slot.addListener(
                new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        // Redirection vers l'écran de création
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
                        // Ici, tu pourrais ouvrir l'édition du deck existant
                    }
                }
            );
        }

        return slot;
    }

    @Override
    public void render(float delta) {
        // Fond sombre et épuré
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
