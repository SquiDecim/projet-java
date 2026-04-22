package io.github.squidecim.genialtcg.deckMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
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
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
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

    private final int MAX_CARDS = 40;
    private Label counterLabel;
    private TextButton btnValidate;
    private TextField searchField;

    private Table gridTable;
    private Array<AtlasRegion> allCardsSorted;
    private Array<String> selectedCards;
    private Deck editingDeck = null;

    private Container<Stack> zoomContainer; // Changé en Stack pour inclure la bordure
    private String zoomedCardName = "";
    private Drawable silverBorder;

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

        // Texture pour la bordure (Blanc Argenté)
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.9f, 0.9f, 0.9f, 1f));
        pixmap.fill();
        silverBorder = new TextureRegionDrawable(new Texture(pixmap));
        pixmap.dispose();

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

        // Gestion de la touche Espace
        stage.addListener(
            new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.SPACE) {
                        if (
                            zoomContainer != null && zoomContainer.hasParent()
                        ) {
                            closeZoom();
                        } else {
                            Actor hit = stage.hit(
                                Gdx.input.getX(),
                                Gdx.graphics.getHeight() - Gdx.input.getY(),
                                true
                            );
                            while (hit != null && !(hit instanceof Stack)) hit =
                                hit.getParent();
                            if (hit instanceof Stack) {
                                Table wrapper = (Table) ((Stack) hit).findActor(
                                    "cardWrapper"
                                );
                                Image img = (Image) wrapper
                                    .getChildren()
                                    .first();
                                showZoom(
                                    (AtlasRegion) (
                                        (TextureRegionDrawable) img.getDrawable()
                                    ).getRegion()
                                );
                            }
                        }
                        return true;
                    }
                    return false;
                }
            }
        );

        // Top Bar
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
        searchField.setMessageText("Rechercher...");
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
        scroll.setScrollingDisabled(true, false);
        scroll.getStyle().vScroll = null;
        scroll.getStyle().vScrollKnob = null;

        root.add(scroll).expand().fill().pad(10);
        stage.setScrollFocus(scroll);

        updateUI();
    }

    private void showZoom(AtlasRegion region) {
        zoomedCardName = region.name;
        updateZoomContent(region); // Centralisation de la logique d'affichage
    }

    // Nouvelle méthode pour gérer l'affichage du zoom (initial et mise à jour)
    private void updateZoomContent(AtlasRegion region) {
        boolean isSelected = selectedCards.contains(region.name, false);
        float zoomHeight = stage.getHeight() * 0.85f;
        float ratio =
            region.getRegionWidth() / (float) region.getRegionHeight();
        float cardW = zoomHeight * ratio;
        float cardH = zoomHeight;
        float borderThickness = 6f; // Épaisseur uniforme pour le zoom

        // Création du Stack principal qui contient la carte et sa bordure
        Stack zoomStack = new Stack();

        // 1. La Bordure (si sélectionnée)
        if (isSelected) {
            Image border = new Image(silverBorder);
            Table borderWrapper = new Table();
            // On centre la bordure et on lui donne la taille de la carte + l'épaisseur
            borderWrapper
                .add(border)
                .size(cardW + borderThickness * 2, cardH + borderThickness * 2);
            zoomStack.add(borderWrapper);
        }

        // 2. L'Image de la carte
        Image zoomedImg = new Image(region);
        Table cardWrapper = new Table();
        cardWrapper.add(zoomedImg).size(cardW, cardH);
        zoomStack.add(cardWrapper);

        // Gestion du conteneur global (initialisation ou mise à jour)
        if (zoomContainer == null) {
            zoomContainer = new Container<>();
            zoomContainer.setFillParent(true);
            // Fond sombre derrière le zoom
            zoomContainer.setBackground(
                skin.newDrawable("white", new Color(0, 0, 0, 0.85f))
            );
            stage.addActor(zoomContainer);
        }

        zoomContainer.setActor(zoomStack);

        // Nettoyage des listeners précédents pour éviter les doublons
        zoomContainer.clearListeners();

        // Clic sur le zoom pour sélectionner/désélectionner
        zoomContainer.addListener(
            new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    toggleCardSelection(region.name);
                    updateGrid(searchField.getText()); // Met à jour la grille en fond
                    updateZoomContent(region); // Met à jour l'affichage du zoom (bordure)
                }
            }
        );
    }

    private void closeZoom() {
        if (zoomContainer != null) {
            zoomContainer.remove();
            zoomContainer = null; // Important pour forcer la recréation
            zoomedCardName = "";
        }
    }

    private void toggleCardSelection(String cardName) {
        if (selectedCards.contains(cardName, false)) {
            selectedCards.removeValue(cardName, false);
        } else if (selectedCards.size < MAX_CARDS) {
            selectedCards.add(cardName);
        }
        updateUI();
    }

    private void updateGrid(String filter) {
        gridTable.clearChildren();
        float baseW = 320 * 0.85f;
        float baseH = 448 * 0.85f;
        float borderThickness = 4f;
        String query = filter.toLowerCase().trim();

        searchField.setColor(hasMatchingCard(query) ? Color.WHITE : Color.RED);

        int visibleCount = 0;
        for (final AtlasRegion region : allCardsSorted) {
            if (
                !query.isEmpty() && !region.name.toLowerCase().contains(query)
            ) continue;

            boolean isSelected = selectedCards.contains(region.name, false);
            Stack slot = new Stack();

            if (isSelected) {
                Image border = new Image(silverBorder);
                Table borderWrapper = new Table();
                borderWrapper
                    .add(border)
                    .size(
                        baseW * 1.05f + borderThickness,
                        baseH * 1.05f + borderThickness
                    );
                slot.add(borderWrapper);
            }

            Image cardImg = new Image(region);
            Table wrapper = new Table();
            wrapper.setName("cardWrapper");

            float finalW = isSelected ? baseW * 1.05f : baseW;
            float finalH = isSelected ? baseH * 1.05f : baseH;

            wrapper.add(cardImg).size(finalW, finalH);
            slot.add(wrapper);

            slot.addListener(
                new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        toggleCardSelection(region.name);
                        updateGrid(searchField.getText());
                    }
                }
            );

            gridTable.add(slot).size(baseW * 1.1f, baseH * 1.1f).pad(5);
            visibleCount++;
            if (visibleCount % 6 == 0) gridTable.row();
        }
    }

    private boolean hasMatchingCard(String query) {
        if (query.isEmpty()) return true;
        for (AtlasRegion region : allCardsSorted) {
            if (region.name.toLowerCase().contains(query)) return true;
        }
        return false;
    }

    private void updateUI() {
        counterLabel.setText(
            "Cartes : " + selectedCards.size + " / " + MAX_CARDS
        );
        boolean isFull = (selectedCards.size == MAX_CARDS);
        btnValidate.setDisabled(!isFull);
        btnValidate.setColor(isFull ? Color.GREEN : Color.GRAY);
    }

    private void showSaveDialog() {
        Dialog dialog = new Dialog("", skin);
        TextField nameInput = new TextField(
            editingDeck != null ? editingDeck.name : "",
            skin
        );
        dialog
            .getContentTable()
            .add(new Label("Nom du deck :", skin))
            .pad(10)
            .row();
        dialog.getContentTable().add(nameInput).width(300).pad(10);

        Runnable confirmAction = () -> {
            String name = nameInput.getText().trim();
            if (!name.isEmpty()) {
                if (editingDeck != null) {
                    editingDeck.name = name;
                    editingDeck.cardNames = new Array<>(selectedCards);
                } else {
                    game.savedDecks.add(
                        new Deck(name, new Array<>(selectedCards))
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
