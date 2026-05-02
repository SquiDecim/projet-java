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
    private TextureAtlas atlas_actions;
    private TextureAtlas atlas_outils;

    private final int MAX_COUNTRY = 40;
    private final int MAX_ACTION = 10;
    private final int MAX_TOOL = 10;

    private Label counterLabel;
    private Label messageLabel;
    private TextButton btnValidate;

    private TextField searchField;
    private SelectBox<String> typeSelect;
    private SelectBox<String> rankSelect;
    private SelectBox<String> categorySelect;

    private final String[] categoryOptions = {
        "Toutes",
        "Pays",
        "Actions",
        "Outils",
    };
    private final String[] types = {
        "Tous",
        "Diplomatique",
        "Économique",
        "Isolationniste",
        "Militaire",
        "Renseignement",
    };
    private final String[] ranks = {
        "Tous",
        "Marginal",
        "Émergent",
        "Établi",
        "Dominant",
        "Hégémonie",
    };
    private Table gridTable;
    private Array<AtlasRegion> allCardsSorted;
    private Array<String> selectedCards;

    private CardsStackData editingDeck = null;
    private Container<Stack> zoomContainer;
    private Drawable silverBorder;

    private static final float EFFECT_SCALE = 1.05f;
    private static final float ANIM_DURATION = 0.1f;
    private Dialog dialog;

    public NewDeckScreen(GenialTCG game) {
        this(game, null);
    }

    public NewDeckScreen(GenialTCG game, CardsStackData deckToEdit) {
        this.game = game;
        this.editingDeck = deckToEdit;
        this.selectedCards = new Array<>();

        if (deckToEdit != null) {
            for (CardData cd : deckToEdit.getCards()) {
                this.selectedCards.add(cd.getAtlasRegionName());
            }
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
        atlas_actions = new TextureAtlas(
            Gdx.files.internal("cards/action/cards_actions.atlas")
        );
        atlas_outils = new TextureAtlas(
            Gdx.files.internal("cards/outils/cards_outils.atlas")
        );

        setLinearFilter(atlas);
        setLinearFilter(atlas_actions);
        setLinearFilter(atlas_outils);

        allCardsSorted = new Array<>();
        final Collator collator = Collator.getInstance(Locale.FRENCH);
        collator.setStrength(Collator.PRIMARY);

        Array<AtlasRegion> countries = new Array<>(atlas.getRegions());
        countries.sort((r1, r2) -> collator.compare(r1.name, r2.name));

        Array<AtlasRegion> actions = new Array<>(atlas_actions.getRegions());
        actions.sort((r1, r2) -> collator.compare(r1.name, r2.name));

        Array<AtlasRegion> outils = new Array<>(atlas_outils.getRegions());
        outils.sort((r1, r2) -> collator.compare(r1.name, r2.name));

        allCardsSorted.addAll(countries);
        allCardsSorted.addAll(actions);
        allCardsSorted.addAll(outils);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        stage.addListener(
            new InputListener() {
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (
                        keycode == Input.Keys.ESCAPE &&
                        zoomContainer != null &&
                        zoomContainer.hasParent()
                    ) {
                        closeZoom();
                        return true;
                    }
                    return false;
                }
            }
        );

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

        TextButton btnRandom = new TextButton("Aléatoire", skin);
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

        searchField = new TextField("", skin);
        searchField.setMessageText("Rechercher...");

        categorySelect = new SelectBox<>(skin);
        categorySelect.setItems(categoryOptions);

        typeSelect = new SelectBox<>(skin);
        typeSelect.setItems(types);

        rankSelect = new SelectBox<>(skin);
        rankSelect.setItems(ranks);

        ChangeListener filterListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateGrid(searchField.getText());
            }
        };
        searchField.addListener(filterListener);
        categorySelect.addListener(filterListener);
        typeSelect.addListener(filterListener);
        rankSelect.addListener(filterListener);

        Table searchBarTable = new Table();
        searchBarTable.add(new Label("Recherche : ", skin)).padLeft(10);
        searchBarTable.add(searchField).width(180).pad(10);
        searchBarTable.add(new Label("Catégorie : ", skin)).padLeft(10);
        searchBarTable.add(categorySelect).width(140).pad(10);
        searchBarTable.add(new Label("Type : ", skin)).padLeft(10);
        searchBarTable.add(typeSelect).width(140).pad(10);
        searchBarTable.add(new Label("Rang : ", skin)).padLeft(10);
        searchBarTable.add(rankSelect).width(120).pad(10);
        root.add(searchBarTable).expandX().fillX().row();

        gridTable = new Table();
        updateGrid("");
        ScrollPane scroll = new ScrollPane(gridTable, skin);
        root.add(scroll).expand().fill().pad(10);
        stage.setScrollFocus(scroll);

        updateUI();
    }

    private void setLinearFilter(TextureAtlas atlas) {
        for (Texture t : atlas.getTextures())
            t.setFilter(
                Texture.TextureFilter.Linear,
                Texture.TextureFilter.Linear
            );
    }

    private String getCardCategory(String cardName) {
        if (atlas.findRegion(cardName) != null) return "PAYS";
        if (atlas_actions.findRegion(cardName) != null) return "ACTION";
        if (atlas_outils.findRegion(cardName) != null) return "OUTIL";
        return "INCONNU";
    }

    private int getCountInCategory(String category) {
        int count = 0;
        for (String name : selectedCards) {
            if (getCardCategory(name).equals(category)) count++;
        }
        return count;
    }

    private void selectRandomCards() {
        selectedCards.clear();
        Array<AtlasRegion> c = new Array<>(atlas.getRegions());
        c.shuffle();
        for (
            int i = 0;
            i < Math.min(MAX_COUNTRY, c.size);
            i++
        ) selectedCards.add(c.get(i).name);

        Array<AtlasRegion> a = new Array<>(atlas_actions.getRegions());
        a.shuffle();
        for (
            int i = 0;
            i < Math.min(MAX_ACTION, a.size);
            i++
        ) selectedCards.add(a.get(i).name);

        Array<AtlasRegion> o = new Array<>(atlas_outils.getRegions());
        o.shuffle();
        for (int i = 0; i < Math.min(MAX_TOOL, o.size); i++) selectedCards.add(
            o.get(i).name
        );

        updateUI();
        updateGrid(searchField.getText());
    }

    private void toggleCardSelection(String cardName) {
        if (selectedCards.contains(cardName, false)) {
            selectedCards.removeValue(cardName, false);
        } else {
            String category = getCardCategory(cardName);
            int current = getCountInCategory(category);
            if (
                category.equals("PAYS") && current >= MAX_COUNTRY
            ) showEphemeralMessage("Limite de 40 Pays atteinte !");
            else if (
                category.equals("ACTION") && current >= MAX_ACTION
            ) showEphemeralMessage("Limite de 10 Actions atteinte !");
            else if (
                category.equals("OUTIL") && current >= MAX_TOOL
            ) showEphemeralMessage("Limite de 10 Outils atteinte !");
            else selectedCards.add(cardName);
        }
        updateUI();
    }

    private void updateUI() {
        int c = getCountInCategory("PAYS"),
            a = getCountInCategory("ACTION"),
            o = getCountInCategory("OUTIL");
        counterLabel.setText(
            String.format(
                "Pays: %d/%d | Actions: %d/%d | Outils: %d/%d",
                c,
                MAX_COUNTRY,
                a,
                MAX_ACTION,
                o,
                MAX_TOOL
            )
        );
        boolean isFull = (c == MAX_COUNTRY && a == MAX_ACTION && o == MAX_TOOL);
        btnValidate.setDisabled(!isFull);
        btnValidate.setColor(isFull ? Color.GREEN : Color.GRAY);
    }

    private void updateGrid(String filter) {
        gridTable.clearChildren();
        float baseW = 320 * 0.85f,
            baseH = 448 * 0.85f;
        String query = filter.toLowerCase().trim();
        String selType = typeSelect.getSelected();
        String selRank = rankSelect.getSelected();
        String selCat = categorySelect.getSelected();

        int visibleCount = 0;
        for (final AtlasRegion region : allCardsSorted) {
            // Filtrage Catégorie
            String category = getCardCategory(region.name);
            if (!selCat.equals("Toutes")) {
                if (selCat.equals("Pays") && !category.equals("PAYS")) continue;
                if (
                    selCat.equals("Actions") && !category.equals("ACTION")
                ) continue;
                if (
                    selCat.equals("Outils") && !category.equals("OUTIL")
                ) continue;
            }

            // Type et Rang sont exclusivement des attributs Pays
            if (!selType.equals("Tous") && !category.equals("PAYS")) continue;
            if (!selRank.equals("Tous") && !category.equals("PAYS")) continue;

            // Filtrage Recherche & Type/Rang
            CardData data = game.allCardsMap.get(region.name);

            if (
                !query.isEmpty() && !region.name.toLowerCase().contains(query)
            ) continue;
            if (data != null) {
                if (
                    !selType.equals("Tous") &&
                    !data.type.equalsIgnoreCase(selType)
                ) continue;
                if (
                    !selRank.equals("Tous") &&
                    !data.rank.equalsIgnoreCase(selRank)
                ) continue;
            } else if (
                !selType.equals("Tous") || !selRank.equals("Tous")
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
                borderWrapper.add(border).size(baseW + 4f, baseH + 4f);
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
                        ) slot.addAction(
                            Actions.scaleTo(
                                EFFECT_SCALE,
                                EFFECT_SCALE,
                                ANIM_DURATION
                            )
                        );
                    }

                    @Override
                    public void exit(
                        InputEvent event,
                        float x,
                        float y,
                        int pointer,
                        Actor toActor
                    ) {
                        if (pointer == -1 && !isSelected) slot.addAction(
                            Actions.scaleTo(1f, 1f, ANIM_DURATION)
                        );
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

    private void showZoom(AtlasRegion region) {
        if (zoomContainer != null) return;
        boolean isSelected = selectedCards.contains(region.name, false);
        float zoomHeight = stage.getHeight() * 0.85f;
        float cardW =
            zoomHeight *
            (region.getRegionWidth() / (float) region.getRegionHeight());

        Stack zoomStack = new Stack();
        zoomStack.setTransform(true);
        zoomStack.setScale(1f);

        if (isSelected) {
            Image border = new Image(silverBorder);
            Table borderWrapper = new Table();
            borderWrapper.add(border).size(cardW + 10, zoomHeight + 10);
            zoomStack.add(borderWrapper);
        }

        Table imgWrapper = new Table();
        imgWrapper.add(new Image(region)).size(cardW, zoomHeight);
        zoomStack.add(imgWrapper);

        zoomContainer = new Container<>(zoomStack);
        zoomContainer.setFillParent(true);
        zoomContainer.setBackground(
            skin.newDrawable("white", new Color(0, 0, 0, 0.85f))
        );
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

    private void showSaveDialog() {
        dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                if (object.equals(true)) {
                    TextField nameInput = (TextField) getContentTable()
                        .getChildren()
                        .get(1);
                    String name = nameInput.getText().trim();

                    if (!name.isEmpty()) {
                        java.util.List<CardData> list =
                            new java.util.ArrayList<>();
                        for (String s : selectedCards) {
                            CardData d = game.allCardsMap.get(s);
                            if (d == null) {
                                d = new CardData(
                                    s,
                                    "N/A",
                                    "N/A",
                                    "N/A",
                                    0,
                                    0,
                                    new int[5],
                                    0,
                                    new String[0],
                                    new String[0],
                                    new Object[0]
                                );
                            }
                            list.add(d);
                        }

                        System.out.println(
                            "============================================================"
                        );
                        System.out.println("DECK : " + name);
                        System.out.println(
                            "Nombre total de cartes : " + list.size()
                        );
                        System.out.println(
                            "============================================================"
                        );
                        for (CardData d : list) {
                            System.out.println(d.toString());
                            System.out.println(
                                "------------------------------------------------------------"
                            );
                        }
                        System.out.println(
                            "============================================================"
                        );

                        if (editingDeck != null) {
                            editingDeck.name = name;
                            editingDeck.clearCards();
                            for (CardData cd : list) editingDeck.addCard(cd);
                        } else {
                            game.savedDecks.add(new CardsStackData(name, list));
                        }
                        game.saveDecks();
                        game.setScreen(new DeckScreen(game));
                    }
                }
            }
        };

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

        dialog.getButtonTable().defaults().width(120).height(40).pad(10);
        dialog.button("Annuler", false);
        dialog.button("Valider", true);

        dialog.key(Input.Keys.ENTER, true);
        dialog.key(Input.Keys.ESCAPE, false);

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
        if (dialog != null && dialog.hasParent()) {
            float dx = (stage.getWidth() - dialog.getWidth()) / 2f;
            float dy = (stage.getHeight() - dialog.getHeight()) / 2f;
            dialog.setPosition(dx, dy);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        if (atlas != null) atlas.dispose();
        if (atlas_actions != null) atlas_actions.dispose();
        if (atlas_outils != null) atlas_outils.dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}
