package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.model.CardsStackData;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class LobbyScreen implements Screen {

    private final GenialTCG game;
    private final boolean isHost;

    private Stage stage;
    private Skin skin;

    // composants UI
    private Label codeLabel;
    private List<String> playerList;
    private List<String> deckList;
    private TextButton launchButton;
    private Label statusLabel;

    // données
    private Array<String> connectedPlayers = new Array<>();
    private String selectedDeck = null;
    private String lobbyCode = "";

    public LobbyScreen(GenialTCG game, boolean isHost) {
        this.game = game;
        this.isHost = isHost;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport()); // comme NewDeckScreen
        Gdx.input.setInputProcessor(stage);
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);

        // ── BARRE DU HAUT ──
        Table topTable = new Table();

        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new FirstScreen(game));
            }
        });

        // code au centre
        lobbyCode = isHost ? generateCode() : "";
        codeLabel = new Label(
            isHost ? "Code : " + lobbyCode + "  (cliquez pour copier)" : "En attente du code...",
            skin
        );
        codeLabel.setColor(Color.YELLOW);
        if (isHost) {
            codeLabel.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.getClipboard().setContents(lobbyCode);
                    codeLabel.setText("Code : " + lobbyCode + "  ✓ Copié !");
                }
            });
        }

        launchButton = new TextButton("Lancer la partie", skin);
        launchButton.setDisabled(true);
        launchButton.setColor(Color.GRAY);
        launchButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!launchButton.isDisabled()) launchGame();
            }
        });

        topTable.add(btnBack).width(200).height(50).pad(10).left();
        topTable.add(codeLabel).expandX().center(); // code centré
        topTable.add(launchButton).width(200).height(50).pad(10).right();

        root.add(topTable).expandX().fillX().colspan(2).row();

        // ── COLONNE GAUCHE : joueurs ──
        Table leftCol = new Table();
        leftCol.setBackground(skin.newDrawable("white", new Color(0.15f, 0.18f, 0.25f, 1f)));
        leftCol.pad(15);
        leftCol.add(new Label("Joueurs", skin)).padBottom(10).row();

        playerList = new List<>(skin);
        connectedPlayers.add("Joueur 1 (vous)");
        playerList.setItems(connectedPlayers);

        ScrollPane playerScroll = new ScrollPane(playerList, skin);
        playerScroll.setScrollingDisabled(true, false);
        leftCol.add(playerScroll).width(500).height(500).row();

        statusLabel = new Label("En attente d'un joueur...", skin);
        statusLabel.setColor(Color.LIGHT_GRAY);
        leftCol.add(statusLabel).padTop(10);

        // ── COLONNE DROITE : deck ──
        Table rightCol = new Table();
        rightCol.setBackground(skin.newDrawable("white", new Color(0.15f, 0.18f, 0.25f, 1f)));
        rightCol.pad(15);
        rightCol.add(new Label("Choisir un deck", skin)).padBottom(10).row();

        deckList = new List<>(skin);
        Array<String> deckNames = getDeckNames();
        deckList.setItems(deckNames);

        if (deckNames.size > 0) {
            deckList.setSelectedIndex(0);
            selectedDeck = deckNames.get(0);
        }

        deckList.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedDeck = deckList.getSelected();
                updateLaunchButton();
            }
        });

        ScrollPane deckScroll = new ScrollPane(deckList, skin);
        deckScroll.setScrollingDisabled(true, false);
        rightCol.add(deckScroll).width(500).height(500);

        root.add(leftCol).expandY().padTop(60).padRight(20).padLeft(20);
        root.add(rightCol).expandY().padTop(60).padRight(20);

        stage.addActor(root);
        updateLaunchButton();
    }

    // ── génère le code depuis l'IP locale ──
    private String generateCode() {
        try {
            // cherche une IP non-loopback
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(":")) continue; // skip IPv6
                    String ip = addr.getHostAddress();
                    return ipToBase36(ip);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "XXXXXX";
    }

    private String ipToBase36(String ip) {
        String[] parts = ip.split("\\.");
        long ipLong = (Long.parseLong(parts[0]) << 24)
            | (Long.parseLong(parts[1]) << 16)
            | (Long.parseLong(parts[2]) << 8)
            |  Long.parseLong(parts[3]);
        return Long.toString(ipLong, 36).toUpperCase();
    }

    // ── récupère les noms des decks sauvegardés ──
    private Array<String> getDeckNames() {
        Array<String> names = new Array<>();
        if (game.savedDecks != null) {
            for (CardsStackData deck : game.savedDecks) {
                names.add(deck.getName()); // adapte selon ta structure
            }
        }
        if (names.size == 0) {
            names.add("Aucun deck disponible");
        }
        return names;
    }

    // ── active le bouton Lancer seulement si 2 joueurs + deck sélectionné ──
    private void updateLaunchButton() {
        boolean ready = connectedPlayers.size >= 2 && selectedDeck != null;
        launchButton.setDisabled(!ready);
        launchButton.getLabel().setColor(ready ? Color.WHITE : Color.GRAY);
    }

    // ── simule l'arrivée du joueur 2 (à remplacer par callback réseau) ──
    public void onPlayer2Connected(String name) {
        connectedPlayers.add(name);
        playerList.setItems(connectedPlayers);
        statusLabel.setText("Joueur connecté !");
        statusLabel.setColor(Color.GREEN);
        updateLaunchButton();
    }

    private void launchGame() {
        // récupère le deck sélectionné et lance le jeu
        CardsStackData chosenDeck = null;
        if (game.savedDecks != null) {
            for (CardsStackData deck : game.savedDecks) {
                if (deck.getName().equals(selectedDeck)) {
                    chosenDeck = deck;
                    break;
                }
            }
        }
        // game.setScreen(new GameView(game, new GameModel(game, chosenDeck)));
        // décommente quand tu intègres le réseau
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.18f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() { stage.dispose(); skin.dispose(); }
}
