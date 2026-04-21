package io.github.squidecim.genialtcg.deckMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.deckMenu.Deck;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public class NewDeckScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private TextureAtlas atlas;

    private int cardCount = 0;
    private final int MAX_CARDS = 2;
    private Label counterLabel;
    private TextButton btnValidate;
    private TextField searchField;

    private Table gridTable;
    private Array<AtlasRegion> allCardsSorted;
    private Array<String> selectedCards;

    public NewDeckScreen(GenialTCG game) {
        this.game = game;
        this.selectedCards = new Array<>();
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

        allCardsSorted.sort(
            new Comparator<AtlasRegion>() {
                @Override
                public int compare(AtlasRegion r1, AtlasRegion r2) {
                    return collator.compare(r1.name, r2.name);
                }
            }
        );

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

        counterLabel = new Label("Cartes : 0 / " + MAX_CARDS, skin);
        btnValidate = new TextButton("Valider le Deck", skin);
        btnValidate.setDisabled(true);
        btnValidate.setColor(Color.GRAY);

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

        Table searchBarTable = new Table();
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

        searchBarTable.add(new Label("Recherche : ", skin)).padLeft(10);
        searchBarTable.add(searchField).expandX().fillX().pad(10);
        root.add(searchBarTable).expandX().fillX().row();

        gridTable = new Table();
        updateGrid("");

        ScrollPane scroll = new ScrollPane(gridTable, skin);
        scroll.setFadeScrollBars(false);
        root.add(scroll).expand().fill().pad(10);
    }

    private void showSaveDialog() {
        final int MAX_CHARACTERS = 20;
        final int MAX_DECKS = 4;

        Dialog dialog = new Dialog("", skin);
        TextField nameInput = new TextField("", skin);
        nameInput.setMaxLength(MAX_CHARACTERS);
        nameInput.setMessageText("Nom (max " + MAX_CHARACTERS + " car.)");

        dialog
            .getContentTable()
            .add(new Label("Entrez un nom pour votre deck :", skin))
            .pad(10);
        dialog.getContentTable().row();
        dialog.getContentTable().add(nameInput).width(300).pad(10);

        TextButton btnSave = new TextButton("Enregistrer", skin);
        btnSave.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    String deckName = nameInput.getText().trim();
                    if (deckName.isEmpty()) return;

                    if (game.savedDecks.size < MAX_DECKS) {
                        game.savedDecks.add(new Deck(deckName, selectedCards));
                        game.setScreen(new DeckScreen(game));
                    } else {
                        nameInput.setText("");
                        nameInput.setMessageText(
                            "Limite de 4 decks atteinte !"
                        );
                    }
                }
            }
        );

        dialog.getButtonTable().add(btnSave).width(120).height(40).pad(10);
        dialog.show(stage);
    }

    private void updateGrid(String filter) {
        gridTable.clearChildren();
        float displayScale = 0.9f;
        Drawable selectionDrawable = skin.newDrawable(
            "white",
            new Color(0, 1, 0, 0.4f)
        );
        String query = filter.toLowerCase().trim();

        int visibleCount = 0;
        for (int i = 0; i < allCardsSorted.size; i++) {
            final AtlasRegion region = allCardsSorted.get(i);
            String displayName = region.name.replace("_", " ").toLowerCase();

            if (!query.isEmpty() && !displayName.contains(query)) continue;

            final String countryName = region.name;
            Stack slot = new Stack();
            Image cardImg = new Image(region);
            final Image selectionOverlay = new Image(selectionDrawable);
            selectionOverlay.setVisible(
                selectedCards.contains(countryName, false)
            );

            slot.add(cardImg);
            slot.add(selectionOverlay);
            slot.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(
                        com.badlogic.gdx.scenes.scene2d.InputEvent event,
                        float x,
                        float y
                    ) {
                        toggleCardSelection(countryName, selectionOverlay);
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

    private void toggleCardSelection(String countryId, Image overlay) {
        if (selectedCards.contains(countryId, false)) {
            selectedCards.removeValue(countryId, false);
            overlay.setVisible(false);
        } else if (selectedCards.size < MAX_CARDS) {
            selectedCards.add(countryId);
            overlay.setVisible(true);
        }
        updateUI();
    }

    private void updateUI() {
        cardCount = selectedCards.size;
        counterLabel.setText("Cartes : " + cardCount + " / " + MAX_CARDS);
        boolean isFull = (cardCount == MAX_CARDS);
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
        if (atlas != null) atlas.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
