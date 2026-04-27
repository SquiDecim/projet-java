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
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.CardsStackData;
import java.text.Collator;
import java.util.Locale;

public class NewDeckScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private TextureAtlas atlas;

    private final int MAX_CARDS = 40;
    private Label counterLabel;
    private Label messageLabel;
    private TextButton btnValidate;
    private TextField searchField;

    private Table gridTable;
    private Array<AtlasRegion> allCardsSorted;
    private Array<String> selectedCards;

    private CardsStackData editingDeck = null;

    private Container<Stack> zoomContainer;
    private Drawable silverBorder;

    private static final float EFFECT_SCALE = 1.05f;
    private static final float ANIM_DURATION = 0.1f;

    public NewDeckScreen(GenialTCG game) {
        this(game, null);
    }

    public NewDeckScreen(GenialTCG game, CardsStackData deckToEdit) {
        this.game = game;
        this.editingDeck = deckToEdit;

        if (deckToEdit != null) {
            this.selectedCards = new Array<>();
            for (CardData cd : deckToEdit.getCards()) {
                this.selectedCards.add(cd.country);
            }
        } else {
            this.selectedCards = new Array<>();
        }
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

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

        stage.addListener(
            new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (keycode == Input.Keys.ESCAPE) {
                        if (
                            zoomContainer != null && zoomContainer.hasParent()
                        ) {
                            closeZoom();
                            return true;
                        }
                    }
                    return false;
                }
            }
        );

        // --- TOP BAR ---
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

        TextButton btnRandom = new TextButton("Aleatoire", skin);
        btnRandom.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    selectRandomCards();
                }
            }
        );

        counterLabel = new Label("", skin);
        messageLabel = new Label("", skin);
        messageLabel.setColor(Color.ORANGE);

        Table centerGroup = new Table();
        centerGroup.add(counterLabel).row();
        centerGroup.add(messageLabel).height(20);

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
        topBar.add(btnRandom).width(200).height(50).pad(10);
        topBar.add(centerGroup).expandX().center();
        topBar.add(btnValidate).width(200).height(50).pad(10);
        root.add(topBar).expandX().fillX().row();

        // --- SEARCH BAR ---
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

        // --- GRID ---
        gridTable = new Table();
        updateGrid("");

        ScrollPane scroll = new ScrollPane(gridTable, skin);
        scroll.setScrollingDisabled(false, false);
        scroll.setScrollBarPositions(false, false);
        scroll.setFadeScrollBars(true);
        scroll.setupFadeScrollBars(0, 0);

        root.add(scroll).expand().fill().pad(10);
        stage.setScrollFocus(scroll);

        updateUI();
    }

    private void selectRandomCards() {
        selectedCards.clear();
        Array<AtlasRegion> shuffled = new Array<>(allCardsSorted);
        shuffled.shuffle();

        int limit = Math.min(MAX_CARDS, shuffled.size);
        for (int i = 0; i < limit; i++) {
            selectedCards.add(shuffled.get(i).name);
        }
        updateUI();
        updateGrid(searchField.getText());
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

    private void showZoom(AtlasRegion region) {
        if (zoomContainer != null) return;
        updateZoomContent(region);
    }

    private void updateZoomContent(AtlasRegion region) {
        boolean isSelected = selectedCards.contains(region.name, false);
        float zoomHeight = stage.getHeight() * 0.85f;
        float ratio =
            region.getRegionWidth() / (float) region.getRegionHeight();
        float cardW = zoomHeight * ratio;
        float cardH = zoomHeight;
        float borderThickness = 6f;

        Stack zoomStack = new Stack();

        if (isSelected) {
            Image border = new Image(silverBorder);
            Table borderWrapper = new Table();
            borderWrapper
                .add(border)
                .size(cardW + borderThickness * 2, cardH + borderThickness * 2);
            zoomStack.add(borderWrapper);
        }

        Image zoomedImg = new Image(region);
        Table cardWrapper = new Table();
        cardWrapper.add(zoomedImg).size(cardW, cardH);
        zoomStack.add(cardWrapper);

        zoomContainer = new Container<>();
        zoomContainer.setFillParent(true);
        zoomContainer.setBackground(
            skin.newDrawable("white", new Color(0, 0, 0, 0.85f))
        );
        zoomContainer.setActor(zoomStack);
        stage.addActor(zoomContainer);

        zoomContainer.addListener(
            new ClickListener(-1) {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    closeZoom();
                }
            }
        );
    }

    private void closeZoom() {
        if (zoomContainer != null) {
            zoomContainer.remove();
            zoomContainer = null;
        }
    }

    private void toggleCardSelection(String cardName) {
        if (selectedCards.contains(cardName, false)) {
            selectedCards.removeValue(cardName, false);
        } else if (selectedCards.size < MAX_CARDS) {
            selectedCards.add(cardName);
        } else {
            showEphemeralMessage("Le deck est plein !");
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

            final boolean isSelected = selectedCards.contains(
                region.name,
                false
            );
            Stack slot = new Stack();
            slot.setTransform(true);
            slot.setOrigin(Align.center);

            if (isSelected) {
                slot.setScale(EFFECT_SCALE);
                Image border = new Image(silverBorder);
                Table borderWrapper = new Table();
                borderWrapper
                    .add(border)
                    .size(baseW + borderThickness, baseH + borderThickness);
                slot.add(borderWrapper);
            }

            Image cardImg = new Image(region);
            Table wrapper = new Table();
            wrapper.add(cardImg).size(baseW, baseH);
            slot.add(wrapper);

            slot.addListener(
                new ClickListener(Input.Buttons.LEFT) {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (zoomContainer != null) return;
                        toggleCardSelection(region.name);
                        updateGrid(searchField.getText());
                    }

                    @Override
                    public void enter(
                        InputEvent event,
                        float x,
                        float y,
                        int pointer,
                        Actor fromActor
                    ) {
                        if (
                            pointer == -1 &&
                            !isSelected &&
                            zoomContainer == null
                        ) {
                            slot.addAction(
                                Actions.scaleTo(
                                    EFFECT_SCALE,
                                    EFFECT_SCALE,
                                    ANIM_DURATION
                                )
                            );
                        }
                    }

                    @Override
                    public void exit(
                        InputEvent event,
                        float x,
                        float y,
                        int pointer,
                        Actor toActor
                    ) {
                        if (pointer == -1 && !isSelected) {
                            slot.addAction(
                                Actions.scaleTo(1f, 1f, ANIM_DURATION)
                            );
                        }
                    }
                }
            );

            slot.addListener(
                new ClickListener(Input.Buttons.RIGHT) {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        showZoom(region);
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
                java.util.List<CardData> cardDataList = new java.util.ArrayList<>();
                for (String cardName : selectedCards) {
                    cardDataList.add(new CardData(cardName, "", "", "", 0, 0, new int[]{}));
                }

                if (editingDeck != null) {
                    editingDeck.name = name;
                    editingDeck.clearCards();
                    for (CardData card : cardDataList) {
                        editingDeck.addCard(card);
                    }
                } else {
                    CardsStackData newDeck = new CardsStackData(name);
                    for (CardData card : cardDataList) {
                        newDeck.addCard(card);
                    }
                    game.savedDecks.add(newDeck);
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
