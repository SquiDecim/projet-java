package io.github.squidecim.genialtcg.deckMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import java.text.Collator;
import java.util.Locale;

public class NewDeckScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private TextureAtlas atlas;

    private final int MAX_CARDS = 2;
    private Label counterLabel;
    private TextButton btnValidate;
    private TextField searchField;

    private Table gridTable;
    private Array<AtlasRegion> allCardsSorted;
    private Array<String> selectedCards;
    private Deck editingDeck = null;

    public NewDeckScreen(GenialTCG game) {
        this(game, null);
    }

    public NewDeckScreen(GenialTCG game, Deck deckToEdit) {
        this.game = game;
        this.editingDeck = deckToEdit;
        this.selectedCards = (deckToEdit != null)
            ? new Array<>(deckToEdit.cardNames)
            : new Array<>();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        atlas = new TextureAtlas(
            Gdx.files.internal("cards/full/country_cards.atlas")
        );
        for (Texture texture : atlas.getTextures()) {
            texture.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );
        }

        allCardsSorted = new Array<>(atlas.getRegions());
        final Collator collator = Collator.getInstance(Locale.FRENCH);
        collator.setStrength(Collator.PRIMARY);
        allCardsSorted.sort((r1, r2) -> collator.compare(r1.name, r2.name));

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Table topBar = new Table();
        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new DeckScreen(game));
                }
            }
        );

        counterLabel = new Label("", skin);
        btnValidate = new TextButton("Valider", skin);
        btnValidate.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    showSaveDialog();
                }
            }
        );

        topBar.add(btnBack).width(200).height(50).pad(10);
        topBar.add(counterLabel).expandX().center();
        topBar.add(btnValidate).width(200).height(50).pad(10);
        root.add(topBar).expandX().fillX().row();

        searchField = new TextField("", skin);
        searchField.setMessageText("Rechercher un pays...");
        searchField.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    updateGrid(searchField.getText());
                }
            }
        );

        Table searchBarTable = new Table();
        searchBarTable.add(new Label("Recherche : ", skin)).padLeft(10);
        searchBarTable.add(searchField).expandX().fillX().pad(10);
        root.add(searchBarTable).expandX().fillX().row();

        gridTable = new Table();
        updateGrid("");

        ScrollPane scroll = new ScrollPane(gridTable, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollBarPositions(false, true);
        scroll.getStyle().vScroll = null;
        scroll.getStyle().vScrollKnob = null;

        root.add(scroll).expand().fill().pad(10);
        updateUI();
    }

    private boolean hasMatchingCard(String query) {
        if (query.isEmpty()) return true;
        String q = query.toLowerCase().trim();
        for (AtlasRegion region : allCardsSorted) {
            if (region.name.toLowerCase().contains(q)) return true;
        }
        return false;
    }

    private void showSaveDialog() {
        Dialog dialog = new Dialog("", skin);
        TextField nameInput = new TextField(
            editingDeck != null ? editingDeck.name : "",
            skin
        );
        dialog
            .getContentTable()
            .add(new Label("Entrez le nom de votre deck :", skin))
            .pad(10)
            .row();
        dialog.getContentTable().add(nameInput).width(300).pad(10);

        Runnable confirmAction = () -> {
            String deckName = nameInput.getText().trim();
            if (!deckName.isEmpty()) {
                if (editingDeck != null) {
                    editingDeck.name = deckName;
                    editingDeck.cardNames = new Array<>(selectedCards);
                } else {
                    game.savedDecks.add(
                        new Deck(deckName, new Array<>(selectedCards))
                    );
                }
                dialog.hide();
                game.setScreen(new DeckScreen(game));
            }
        };

        nameInput.addListener(
            new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ENTER) {
                        confirmAction.run();
                        return true;
                    }
                    if (keycode == Input.Keys.ESCAPE) {
                        dialog.hide();
                        return true;
                    }
                    return false;
                }
            }
        );

        TextButton btnOk = new TextButton("Valider", skin);
        btnOk.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    confirmAction.run();
                }
            }
        );

        TextButton btnCancel = new TextButton("Annuler", skin);
        btnCancel.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    dialog.hide();
                }
            }
        );

        dialog.getButtonTable().add(btnOk).width(120).height(40).pad(10);
        dialog.getButtonTable().add(btnCancel).width(120).height(40).pad(10);
        dialog.show(stage);
        stage.setKeyboardFocus(nameInput);
    }

    private void updateGrid(String filter) {
        gridTable.clearChildren();
        float displayScale = 0.9f;
        Drawable selectionDrawable = skin.newDrawable(
            "white",
            new Color(0, 1, 0, 0.4f)
        );
        String query = filter.toLowerCase().trim();

        // --- METHODE ROBUSTE POUR LE ROUGE ---
        if (!hasMatchingCard(query)) {
            // Teinte l'ensemble du champ en rouge (curseur, fond et texte)
            searchField.setColor(Color.RED);
        } else {
            // Remet la couleur par défaut
            searchField.setColor(Color.WHITE);
        }

        int visibleCount = 0;
        for (int i = 0; i < allCardsSorted.size; i++) {
            final AtlasRegion region = allCardsSorted.get(i);
            if (
                !query.isEmpty() && !region.name.toLowerCase().contains(query)
            ) continue;

            Stack slot = new Stack();
            Image cardImg = new Image(region);
            final Image selectionOverlay = new Image(selectionDrawable);
            selectionOverlay.setVisible(
                selectedCards.contains(region.name, false)
            );

            slot.add(cardImg);
            slot.add(selectionOverlay);
            slot.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (selectedCards.contains(region.name, false)) {
                            selectedCards.removeValue(region.name, false);
                            selectionOverlay.setVisible(false);
                        } else if (selectedCards.size < MAX_CARDS) {
                            selectedCards.add(region.name);
                            selectionOverlay.setVisible(true);
                        }
                        updateUI();
                    }
                }
            );
            gridTable
                .add(slot)
                .size(320 * displayScale, 448 * displayScale)
                .pad(10);
            visibleCount++;
            if (visibleCount % 6 == 0) gridTable.row();
        }
    }

    private void updateUI() {
        counterLabel.setText(
            "Cartes : " + selectedCards.size + " / " + MAX_CARDS
        );
        boolean isFull = (selectedCards.size == MAX_CARDS);
        btnValidate.setDisabled(!isFull);
        btnValidate.setColor(isFull ? Color.GREEN : Color.GRAY);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.16f, 1f);
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
        atlas.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
