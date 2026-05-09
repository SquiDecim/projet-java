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
    private static final int MIN_CHEAP = 5;
    private static final int MAX_CHEAP_COST = 200;

    private int currentStep = 1;

    private Label counterLabel;
    private Label messageLabel;
    private TextButton btnBack;
    private TextButton btnNext;

    private Table filterBar;
    private Table gridTable;

    private TextField searchField;
    private SelectBox<String> typeSelect;
    private SelectBox<String> rankSelect;
    private SelectBox<String> categorySelect;
    private SelectBox<String> sortSelect;

    private final String[] sortOptionsStep1 = {
        "Alphabétique",
        "Coût croissant",
        "Coût décroissant",
        "État croissant",
        "État décroissant",
    };
    private final String[] sortOptionsStep2 = {
        "Toutes",
        "Avec condition",
        "Sans condition",
    };

    private final String[] categoryOptionsStep2 = {
        "Toutes",
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

    private Array<AtlasRegion> allCardsSorted;
    private Array<String> selectedCards;

    private CardsStackData editingDeck = null;
    private Container<Stack> zoomContainer;
    private Drawable silverBorder;
    private ScrollPane scroll;
    private float zoomW, zoomH;
    private InputListener zoomCloseListener;

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
        skin = game.skin;

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

        final Collator collator = Collator.getInstance(Locale.FRENCH);
        collator.setStrength(Collator.PRIMARY);

        allCardsSorted = new Array<>();
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

        // --- Barre du haut ---
        counterLabel = new Label("", skin);
        messageLabel = new Label("", skin);
        messageLabel.setColor(Color.ORANGE);

        Table centerGroup = new Table();
        centerGroup.add(counterLabel).row();
        centerGroup.add(messageLabel).height(20);

        btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (currentStep == 1) {
                        game.setScreen(new DeckScreen(game));
                    } else {
                        switchStep(1);
                    }
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

        btnNext = new TextButton("Suivant", skin);
        btnNext.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (currentStep == 1) {
                        switchStep(2);
                    } else {
                        showSaveDialog();
                    }
                }
            }
        );

        game.soundifyButton(btnBack);
        game.soundifyButton(btnRandom);
        game.soundifyButton(btnNext);

        Table topBar = new Table();
        topBar.add(btnBack).width(200).height(50).pad(10);
        topBar.add(btnRandom).width(200).height(50).pad(10);
        topBar.add(centerGroup).expandX().center();
        topBar.add(btnNext).width(200).height(50).pad(10);
        root.add(topBar).expandX().fillX().row();

        // --- Widgets de filtre (partagés entre les deux étapes) ---
        ChangeListener filterListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                updateGrid(searchField.getText());
            }
        };

        searchField = new TextField("", skin);
        searchField.setMessageText("Rechercher...");
        searchField.addListener(filterListener);

        typeSelect = new SelectBox<>(skin);
        typeSelect.setItems(types);
        typeSelect.addListener(filterListener);

        rankSelect = new SelectBox<>(skin);
        rankSelect.setItems(ranks);
        rankSelect.addListener(filterListener);

        categorySelect = new SelectBox<>(skin);
        categorySelect.setItems(categoryOptionsStep2);
        categorySelect.addListener(filterListener);

        sortSelect = new SelectBox<>(skin);
        sortSelect.setItems(sortOptionsStep1);
        sortSelect.addListener(filterListener);

        // --- Barre de filtre (reconstruite à chaque étape) ---
        filterBar = new Table();
        root.add(filterBar).expandX().fillX().row();

        // --- Grille ---
        gridTable = new Table();
        scroll = new ScrollPane(gridTable, skin);
        ScrollPane.ScrollPaneStyle noBarStyle = new ScrollPane.ScrollPaneStyle(scroll.getStyle());
        noBarStyle.hScroll = null;
        noBarStyle.hScrollKnob = null;
        noBarStyle.vScroll = null;
        noBarStyle.vScrollKnob = null;
        scroll.setStyle(noBarStyle);
        root.add(scroll).expand().fill().pad(10);
        stage.setScrollFocus(scroll);

        buildFilterBar();
        updateGrid("");
        updateUI();
    }

    private void switchStep(int step) {
        currentStep = step;
        searchField.setText("");
        if (step == 1) {
            sortSelect.setItems(sortOptionsStep1);
            typeSelect.setSelectedIndex(0);
            rankSelect.setSelectedIndex(0);
            btnBack.setText("Retour");
            btnNext.setText("Suivant");
        } else {
            sortSelect.setItems(sortOptionsStep2);
            categorySelect.setSelectedIndex(0);
            btnBack.setText("Retour");
            btnNext.setText("Valider");
        }
        buildFilterBar();
        updateGrid("");
        updateUI();
    }

    private void buildFilterBar() {
        filterBar.clearChildren();
        filterBar.add(new Label("Recherche : ", skin)).padLeft(10);
        filterBar.add(searchField).width(180).pad(10);
        if (currentStep == 1) {
            filterBar.add(new Label("Type : ", skin)).padLeft(10);
            filterBar.add(typeSelect).width(140).pad(10);
            filterBar.add(new Label("Rang : ", skin)).padLeft(10);
            filterBar.add(rankSelect).width(120).pad(10);
        } else {
            filterBar.add(new Label("Catégorie : ", skin)).padLeft(10);
            filterBar.add(categorySelect).width(140).pad(10);
            filterBar.add(new Label("Contrainte : ", skin)).padLeft(10);
            filterBar.add(sortSelect).width(160).pad(10);
            return;
        }
        filterBar.add(new Label("Tri : ", skin)).padLeft(10);
        filterBar.add(sortSelect).width(160).pad(10);
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

    private int countCheapCountryCards() {
        int count = 0;
        for (String name : selectedCards) {
            if (!getCardCategory(name).equals("PAYS")) continue;
            CardData data = game.allCardsMap.get(name);
            if (data != null && data.cost < MAX_CHEAP_COST) count++;
        }
        return count;
    }

    private boolean hasCond(CardData d) {
        return (
            d != null &&
            d.cond != null &&
            !d.cond.isEmpty() &&
            !d.cond.equals("—")
        );
    }

    private void selectRandomCards() {
        if (currentStep == 1) {
            Array<String> toRemove = new Array<>();
            for (String name : selectedCards) {
                if (getCardCategory(name).equals("PAYS")) toRemove.add(name);
            }
            for (String name : toRemove) selectedCards.removeValue(name, false);

            Array<AtlasRegion> cheap = new Array<>();
            Array<AtlasRegion> expensive = new Array<>();
            for (AtlasRegion r : atlas.getRegions()) {
                CardData d = game.allCardsMap.get(r.name);
                if (d != null && d.cost < MAX_CHEAP_COST) cheap.add(r);
                else expensive.add(r);
            }
            cheap.shuffle();
            expensive.shuffle();

            int added = 0;
            for (int i = 0; i < Math.min(MIN_CHEAP, cheap.size); i++) {
                selectedCards.add(cheap.get(i).name);
                added++;
            }
            Array<AtlasRegion> rest = new Array<>();
            for (int i = MIN_CHEAP; i < cheap.size; i++) rest.add(cheap.get(i));
            rest.addAll(expensive);
            rest.shuffle();
            for (
                int i = 0;
                added < MAX_COUNTRY && i < rest.size;
                i++, added++
            ) {
                selectedCards.add(rest.get(i).name);
            }
        } else {
            Array<String> toRemove = new Array<>();
            for (String name : selectedCards) {
                String cat = getCardCategory(name);
                if (cat.equals("ACTION") || cat.equals("OUTIL")) toRemove.add(
                    name
                );
            }
            for (String name : toRemove) selectedCards.removeValue(name, false);

            Array<AtlasRegion> a = new Array<>(atlas_actions.getRegions());
            a.shuffle();
            for (
                int i = 0;
                i < Math.min(MAX_ACTION, a.size);
                i++
            ) selectedCards.add(a.get(i).name);

            Array<AtlasRegion> o = new Array<>(atlas_outils.getRegions());
            o.shuffle();
            for (
                int i = 0;
                i < Math.min(MAX_TOOL, o.size);
                i++
            ) selectedCards.add(o.get(i).name);
        }

        updateUI();
        updateGrid(searchField.getText());
    }

    private boolean toggleCardSelection(String cardName) {
        if (selectedCards.contains(cardName, false)) {
            selectedCards.removeValue(cardName, false);
        } else {
            String category = getCardCategory(cardName);
            int current = getCountInCategory(category);
            if (category.equals("PAYS") && current >= MAX_COUNTRY) {
                showEphemeralMessage("Limite de 40 Pays atteinte !");
                updateUI();
                return false;
            } else if (category.equals("ACTION") && current >= MAX_ACTION) {
                showEphemeralMessage("Limite de 10 Actions atteinte !");
                updateUI();
                return false;
            } else if (category.equals("OUTIL") && current >= MAX_TOOL) {
                showEphemeralMessage("Limite de 10 Outils atteinte !");
                updateUI();
                return false;
            } else if (category.equals("PAYS")) {
                CardData data = game.allCardsMap.get(cardName);
                boolean isCheap = data == null || data.cost < MAX_CHEAP_COST;
                if (!isCheap) {
                    int cheap = countCheapCountryCards();
                    int remainingAfterAdd = MAX_COUNTRY - current - 1;
                    if (remainingAfterAdd < MIN_CHEAP - cheap) {
                        showEphemeralMessage(
                            "Votre deck doit contenir au minimum 5 cartes avec cout <200"
                        );
                        updateUI();
                        return false;
                    } else {
                        selectedCards.add(cardName);
                    }
                } else {
                    selectedCards.add(cardName);
                }
            } else {
                selectedCards.add(cardName);
            }
        }
        updateUI();
        return true;
    }

    private void updateUI() {
        int c = getCountInCategory("PAYS");
        int a = getCountInCategory("ACTION");
        int o = getCountInCategory("OUTIL");
        int cheap = countCheapCountryCards();

        if (currentStep == 1) {
            counterLabel.setText(
                String.format(
                    "Pays : %d/%d | Posables (<200c) : %d/%d",
                    c,
                    MAX_COUNTRY,
                    cheap,
                    MIN_CHEAP
                )
            );
            boolean canAdvance = (c == MAX_COUNTRY && cheap >= MIN_CHEAP);
            btnNext.setDisabled(!canAdvance);
            btnNext.setColor(canAdvance ? Color.GREEN : Color.GRAY);
        } else {
            counterLabel.setText(
                String.format(
                    "Actions : %d/%d | Outils : %d/%d",
                    a,
                    MAX_ACTION,
                    o,
                    MAX_TOOL
                )
            );
            boolean valid = (a == MAX_ACTION && o == MAX_TOOL);
            btnNext.setDisabled(!valid);
            btnNext.setColor(valid ? Color.GREEN : Color.GRAY);
        }
    }

    private void updateGrid(String filter) {
        gridTable.clearChildren();
        float baseW = 320 * 0.85f,
            baseH = 448 * 0.85f;
        String query = filter.toLowerCase().trim();
        String selSort = sortSelect.getSelected();

        Array<AtlasRegion> filtered = new Array<>();
        for (AtlasRegion region : allCardsSorted) {
            String category = getCardCategory(region.name);

            if (currentStep == 1 && !category.equals("PAYS")) continue;
            if (currentStep == 2 && category.equals("PAYS")) continue;

            if (currentStep == 2) {
                String selCat = categorySelect.getSelected();
                if (
                    selCat.equals("Actions") && !category.equals("ACTION")
                ) continue;
                if (
                    selCat.equals("Outils") && !category.equals("OUTIL")
                ) continue;

                String selCont = sortSelect.getSelected();
                if (!selCont.equals("Toutes")) {
                    CardData data = game.allCardsMap.get(region.name);
                    if (
                        selCont.equals("Avec condition") && !hasCond(data)
                    ) continue;
                    if (
                        selCont.equals("Sans condition") && hasCond(data)
                    ) continue;
                }
            }

            if (
                !query.isEmpty() && !region.name.toLowerCase().contains(query)
            ) continue;

            if (currentStep == 1) {
                CardData data = game.allCardsMap.get(region.name);
                String selType = typeSelect.getSelected();
                String selRank = rankSelect.getSelected();
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
            }

            filtered.add(region);
        }

        if (currentStep == 2) {
            final Collator col = Collator.getInstance(Locale.FRENCH);
            col.setStrength(Collator.PRIMARY);
            filtered.sort((r1, r2) -> {
                CardData d1 = game.allCardsMap.get(r1.name);
                CardData d2 = game.allCardsMap.get(r2.name);
                String n1 = d1 != null ? d1.country : r1.name;
                String n2 = d2 != null ? d2.country : r2.name;
                return col.compare(n1, n2);
            });
        } else if (selSort.equals("Coût croissant")) {
            filtered.sort((r1, r2) -> {
                CardData d1 = game.allCardsMap.get(r1.name);
                CardData d2 = game.allCardsMap.get(r2.name);
                return Integer.compare(
                    d1 != null ? d1.cost : 0,
                    d2 != null ? d2.cost : 0
                );
            });
        } else if (selSort.equals("Coût décroissant")) {
            filtered.sort((r1, r2) -> {
                CardData d1 = game.allCardsMap.get(r1.name);
                CardData d2 = game.allCardsMap.get(r2.name);
                return Integer.compare(
                    d2 != null ? d2.cost : 0,
                    d1 != null ? d1.cost : 0
                );
            });
        } else if (selSort.equals("État croissant")) {
            filtered.sort((r1, r2) -> {
                CardData d1 = game.allCardsMap.get(r1.name);
                CardData d2 = game.allCardsMap.get(r2.name);
                return Integer.compare(
                    d1 != null ? d1.pv : 0,
                    d2 != null ? d2.pv : 0
                );
            });
        } else if (selSort.equals("État décroissant")) {
            filtered.sort((r1, r2) -> {
                CardData d1 = game.allCardsMap.get(r1.name);
                CardData d2 = game.allCardsMap.get(r2.name);
                return Integer.compare(
                    d2 != null ? d2.pv : 0,
                    d1 != null ? d1.pv : 0
                );
            });
        }

        int visibleCount = 0;
        for (final AtlasRegion region : filtered) {
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
                        if (toggleCardSelection(region.name)) {
                            updateGrid(searchField.getText());
                        }
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
                            if (game.overpassCardsSound != null) game.overpassCardsSound.play(game.uiSoundVolume);
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
        zoomH = stage.getHeight() * 0.85f;
        zoomW = zoomH * (region.getRegionWidth() / (float) region.getRegionHeight());

        Stack zoomStack = new Stack();

        if (isSelected) {
            Image border = new Image(silverBorder);
            Table borderWrapper = new Table();
            borderWrapper.add(border).size(zoomW + 10, zoomH + 10);
            zoomStack.add(borderWrapper);
        }

        Table imgWrapper = new Table();
        imgWrapper.add(new Image(region)).size(zoomW, zoomH);
        zoomStack.add(imgWrapper);

        zoomContainer = new Container<>(zoomStack);
        zoomContainer.setFillParent(true);
        zoomContainer.setBackground(
            skin.newDrawable("white", new Color(0, 0, 0, 0.85f))
        );
        stage.addActor(zoomContainer);
        stage.setScrollFocus(null);

        zoomCloseListener = new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                float sw = stage.getWidth();
                float sh = stage.getHeight();
                float cardLeft   = (sw - zoomW) / 2f;
                float cardBottom = (sh - zoomH) / 2f;
                boolean onCard = x >= cardLeft && x <= cardLeft + zoomW
                              && y >= cardBottom && y <= cardBottom + zoomH;
                if (!onCard) {
                    closeZoom();
                    event.cancel();
                }
                return true;
            }
        };
        stage.addCaptureListener(zoomCloseListener);
    }

    private void closeZoom() {
        if (zoomContainer != null) {
            zoomContainer.remove();
            zoomContainer = null;
            if (zoomCloseListener != null) {
                stage.removeCaptureListener(zoomCloseListener);
                zoomCloseListener = null;
            }
            stage.setScrollFocus(scroll);
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
                                    s, "N/A", "N/A", "N/A",
                                    0, 0, new int[5], 0, "N/A", "N/A"
                                );
                            }
                            list.add(d);
                        }


                        if (editingDeck != null) {
                            editingDeck.name = name;
                            editingDeck.clearCards();
                            for (CardData cd : list) editingDeck.addCard(cd);
                        } else {
                            game.savedDecks.add(new CardsStackData(name, list));
                        }
                        game.setScreen(new DeckScreen(game));
                    }
                }
            }
        };

        TextField nameInput = new TextField(
            editingDeck != null ? editingDeck.name : "",
            skin
        );
        nameInput.setMaxLength(15);
        nameInput.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    if (game.clickSound != null) game.clickSound.play(game.uiSoundVolume);
                }
                return false;
            }
        });
        dialog
            .getContentTable()
            .add(new Label("Nom du deck :", skin))
            .pad(10)
            .row();
        dialog.getContentTable().add(nameInput).width(300).pad(10);

        dialog.getButtonTable().defaults().width(120).height(40).pad(10);
        dialog.button("Annuler", false);
        dialog.button("Valider", true);
        for (Cell<?> cell : dialog.getButtonTable().getCells()) {
            if (cell.getActor() instanceof TextButton) {
                game.soundifyButton((TextButton) cell.getActor());
            }
        }

        dialog.key(Input.Keys.ENTER, true);
        dialog.setResizable(true);

        dialog.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (x < 0 || x > dialog.getWidth() || y < 0 || y > dialog.getHeight()) {
                    dialog.hide();
                }
                return false;
            }
        });
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
