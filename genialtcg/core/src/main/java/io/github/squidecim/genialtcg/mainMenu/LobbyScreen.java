package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;
import io.github.squidecim.genialtcg.controller.GameController;
import io.github.squidecim.genialtcg.model.CardsStackData;
import io.github.squidecim.genialtcg.model.GameModel;
import io.github.squidecim.genialtcg.network.GameClient;
import io.github.squidecim.genialtcg.network.GameServer;
import io.github.squidecim.genialtcg.network.NetworkMessages;
import io.github.squidecim.genialtcg.view.GameView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class LobbyScreen implements Screen, GameClient.NetworkListener {

    private final GenialTCG game;
    private final boolean isHost;
    private final String hostIp;

    private GameServer server;
    private GameClient client;

    private Stage stage;
    private Skin skin;

    // composants UI
    private Label codeLabel;
    private List<String> playerList;
    private TextButton launchButton;
    private Label statusLabel;
    private Texture backTexture;

    private boolean launching = false;
    private Drawable silverBorder;
    private Table deckGrid;

    private boolean intentionalDisconnect = false;


    // données
    private Array<String> connectedPlayers = new Array<>();
    private String selectedDeck = null;
    private String lobbyCode = "";
    private String myPlayerId = null;

    public LobbyScreen(GenialTCG game, boolean isHost) {
        this(game, isHost, "localhost", null);
    }

    public LobbyScreen(GenialTCG game, boolean isHost, String hostIp, GameClient existingClient) {
        this.game = game;
        this.isHost = isHost;
        this.hostIp = hostIp;
        this.client = existingClient; // client déjà connecté, show() ne recrée pas
    }

    @Override
    public void show() {

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = game.skin;
        backTexture = new Texture(Gdx.files.internal("cards/backCardTexture.png"));

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.9f, 0.9f, 0.9f, 1f));
        pixmap.fill();
        silverBorder = new TextureRegionDrawable(new Texture(pixmap));
        pixmap.dispose();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // ── BARRE DU HAUT (Identique à DeckScreen) ──
        Table topTable = new Table();

        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new FirstScreen(game));
                }
            }
        );

        // Logique du Code + Copier-Coller
        lobbyCode = isHost ? generateCode() : "";
        codeLabel = new Label(
            isHost
                ? "Code : " + lobbyCode + " (cliquez pour copier)"
                : "En attente du code...",
            skin
        );
        codeLabel.setAlignment(Align.center);
        codeLabel.setColor(Color.YELLOW);

        codeLabel.addListener(
            new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.getClipboard().setContents(lobbyCode);
                    codeLabel.setText("Code : " + lobbyCode + " ✓ Copié !");
                }
            }
        );

        launchButton = new TextButton("Lancer la partie", skin);
        launchButton.setDisabled(true);
        launchButton.getLabel().setColor(Color.GRAY);
        launchButton.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (!launchButton.isDisabled()) launchGame();
                }
            }
        );

        game.soundifyButton(btnBack);
        topTable.add(btnBack).width(200).height(50).pad(10).left();
        topTable.add(codeLabel).expandX().center();
        game.soundifyButton(launchButton);
        if (isHost) {
            topTable.add(launchButton).width(200).height(50).pad(10).right();
        } else {
            Label waitLabel = new Label("En attente du lancement...", skin);
            waitLabel.setColor(Color.LIGHT_GRAY);
            waitLabel.setAlignment(Align.center);
            topTable.add(waitLabel).width(200).height(50).pad(10).right();
        }

        // Ajout de la barre en haut du root (colspan 2 car on a 2 colonnes en dessous)
        root.add(topTable).expandX().fillX().top().colspan(2).row();

        // ── CONTENU (Colonnes Joueurs et Decks) ──

        // Colonne Gauche : Joueurs
        Table leftCol = new Table();
        leftCol.setBackground(
            skin.newDrawable("white", new Color(0.15f, 0.18f, 0.25f, 1f))
        );
        leftCol.pad(15);
        Label joueursTitre = new Label("Joueurs", skin, "title");
        joueursTitre.setFontScale(0.35f);
        leftCol.add(joueursTitre).padBottom(10).row();

        playerList = new List<>(skin);
        connectedPlayers.add("Joueur 1 (vous)");
        playerList.setItems(connectedPlayers);

        ScrollPane playerScroll = new ScrollPane(playerList, skin);
        leftCol.add(playerScroll).expand().fill().row();

        statusLabel = new Label(
            isHost ? "En attente d'un joueur..." : "Connecté au salon",
            skin
        );
        statusLabel.setColor(Color.LIGHT_GRAY);
        leftCol.add(statusLabel).padTop(10);

        // Colonne Droite : Decks
        Table rightCol = new Table();
        rightCol.setBackground(
            skin.newDrawable("white", new Color(0.15f, 0.18f, 0.25f, 1f))
        );
        rightCol.pad(15);
        Label deckTitre = new Label("Choisir un deck", skin, "title");
        deckTitre.setFontScale(0.35f);
        rightCol.add(deckTitre).padBottom(10).row();

        deckGrid = new Table();
        buildDeckGrid();
        rightCol.add(deckGrid).expand();

        root.add(leftCol).expand().fill().pad(20);
        root.add(rightCol).expand().fill().pad(20);

        updateLaunchButton();

        if (isHost) {
            try {
                server = new GameServer(lobbyCode);
                server.setOnBothConnected(() -> Gdx.app.postRunnable(this::updateLaunchButton));
                if (client == null) client = new GameClient("localhost", this);
            } catch (IOException e) {
                statusLabel.setText("Erreur serveur : " + e.getMessage());
            }
        } else {
            if (client == null) {
                try {
                    client = new GameClient(hostIp, this);
                } catch (IOException e) {
                    statusLabel.setText("Ce code ne mène à aucun lobby");
                    statusLabel.setColor(Color.RED);
                }
            } else {
                client.setListener(this);
            }
        }
        if (client != null) {
            client.setOnDisconnected(() -> {
                if (!isHost && !intentionalDisconnect) {
                    Gdx.app.postRunnable(() -> {
                        dispose();
                        game.setScreen(new FirstScreen(game, "Connexion perdue !"));
                    });
                }
            });
        }

    }

    private void buildDeckGrid() {
        deckGrid.clearChildren();
        final float W = 250, H = 350;
        if (selectedDeck == null && game.savedDecks.size > 0) {
            selectedDeck = game.savedDecks.get(0).name;
        }
        for (int i = 0; i < 4; i++) {
            if (i < game.savedDecks.size) {
                final CardsStackData deck = game.savedDecks.get(i);
                final boolean isSelected = deck.name.equals(selectedDeck);
                final Stack stack = new Stack();
                stack.setTransform(true);
                stack.setOrigin(Align.center);

                if (isSelected) {
                    stack.setScale(1.05f);
                    Image border = new Image(silverBorder);
                    Table borderWrapper = new Table();
                    borderWrapper.add(border).size(W + 6f, H + 6f);
                    stack.add(borderWrapper);
                }

                Image cardImage = new Image(backTexture);
                Table cardWrapper = new Table();
                cardWrapper.add(cardImage).size(W, H);

                Label deckName = new Label(deck.name, skin, "title");
                deckName.setFontScale(0.20f);
                deckName.setColor(Color.WHITE);
                Table nameOverlay = new Table();
                nameOverlay.add(deckName).expandY().bottom().padBottom(22);

                stack.add(cardWrapper);
                stack.add(nameOverlay);
                stack.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        selectedDeck = deck.name;
                        buildDeckGrid();
                        updateLaunchButton();
                    }

                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        if (pointer == -1 && !isSelected) {
                            if (game.overpassCardsSound != null) game.overpassCardsSound.play(game.uiSoundVolume);
                            stack.addAction(Actions.scaleTo(1.05f, 1.05f, 0.1f));
                        }
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        if (pointer == -1 && !isSelected)
                            stack.addAction(Actions.scaleTo(1f, 1f, 0.1f));
                    }
                });

                deckGrid.add(stack).size(W * 1.1f, H * 1.1f).pad(5);
            } else {
                Table emptySlot = new Table();
                emptySlot.setBackground(
                    skin.newDrawable("white", new Color(0.12f, 0.15f, 0.22f, 1f))
                );
                deckGrid.add(emptySlot).size(W, H).pad(10);
            }
            if (i % 2 == 1) deckGrid.row();
        }
    }

    private String generateCode() {
        try {
            Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr.getHostAddress().contains(":")) continue;
                    return ipToBase36(addr.getHostAddress());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "XXXXXX";
    }

    private String ipToBase36(String ip) {
        String[] parts = ip.split("\\.");
        long ipLong =
            (Long.parseLong(parts[0]) << 24) |
            (Long.parseLong(parts[1]) << 16) |
            (Long.parseLong(parts[2]) << 8) |
            Long.parseLong(parts[3]);
        return Long.toString(ipLong, 36).toUpperCase();
    }

    private void updateLaunchButton() {
        if (!isHost) return;
        boolean ready = connectedPlayers.size >= 2 &&
                        selectedDeck != null &&
                        !selectedDeck.equals("Aucun deck disponible");
        launchButton.setDisabled(!ready);
        launchButton.getLabel().setColor(ready ? Color.WHITE : Color.GRAY);
    }

    public void onPlayer2Connected(String name) {
        connectedPlayers.add(name);
        playerList.setItems(connectedPlayers);
        statusLabel.setText("Joueur connecté !");
        statusLabel.setColor(Color.GREEN);
        updateLaunchButton();
    }

    private void launchGame() {
        CardsStackData chosenDeck = game.savedDecks.get(0);
        NetworkMessages.GameStart start = new NetworkMessages.GameStart();
        start.firstPlayerId = myPlayerId;
        client.sendGameStart(start);

        launching = true;
        for (CardsStackData deck : game.savedDecks) {
            if (deck.name.equals(selectedDeck)) {
                chosenDeck = deck;
                break;
            }
        }
        if (chosenDeck == null) return;

        GameModel model = new GameModel(game, chosenDeck);
        GameView view = new GameView(game, model, client, chosenDeck.getSize());
        GameController controller = new GameController(view, model, client, myPlayerId, game);
        view.setController(controller);
        client.setListener(controller);
        client.sendDeckSize(model.deckSize());
        game.setScreen(view);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.18f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        stage.getViewport().update(w, h, true);
    }

    @Override
    public void hide() {
        intentionalDisconnect = true;
        if (launching) return;
        if (client != null) client.disconnect();
        if (server != null) server.stop();
        client = null;
        server = null;
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        stage.dispose();
        if (backTexture != null) backTexture.dispose();
    }

    @Override
    public void onAssignId(NetworkMessages.AssignId msg) {
        myPlayerId = msg.playerId;
    }

    @Override
    public void onGameStart(NetworkMessages.GameStart msg) {
        CardsStackData chosenDeck = game.savedDecks.get(0);
        for (CardsStackData deck : game.savedDecks) {
            if (deck.name.equals(selectedDeck)) {
                chosenDeck = deck;
                break;
            }
        }
        if (chosenDeck == null) return;

        GameModel model = new GameModel(game, chosenDeck);
        GameView view = new GameView(game, model, client, chosenDeck.getSize());
        GameController controller = new GameController(view, model, client, myPlayerId, game);
        view.setController(controller);
        launching = true;
        client.setListener(controller);
        client.sendDeckSize(model.deckSize());
        game.setScreen(view);
    }

    @Override
    public void onCardDrawn(NetworkMessages.CardDrawn msg) {

    }

    @Override
    public void onCardPlayed(NetworkMessages.CardPlayed msg) {

    }

    @Override
    public void onTurnChanged(NetworkMessages.TurnChanged msg) {

    }

    @Override
    public void onPlayerJoined(NetworkMessages.PlayerJoined msg) {
        connectedPlayers.clear();
        for (int i = 0; i < msg.playerCount; i++) {
            String pid = i == 0 ? "player1" : "player2";
            String name = "Joueur " + (i + 1);
            if (pid.equals(myPlayerId)) name += " (vous)";
            else if (i == 0 && isHost) name += " (hôte)";
            connectedPlayers.add(name);
        }
        playerList.setItems(connectedPlayers);
        statusLabel.setText(msg.playerCount < 2 ? "En attente d'un joueur..." : "Joueur connecté !");
        statusLabel.setColor(msg.playerCount < 2 ? Color.LIGHT_GRAY : Color.GREEN);
        updateLaunchButton();
    }

    @Override
    public void onLobbyInfo(NetworkMessages.LobbyInfo msg) {
        lobbyCode = msg.lobbyCode;
        codeLabel.setText("Code : " + lobbyCode + " (cliquez pour copier)");
    }

    @Override
    public void onCreditsUpdate(NetworkMessages.CreditsUpdate obj) {

    }

    @Override
    public void onNormalAttack(NetworkMessages.NormalAttack msg) {

    }

    @Override
    public void onRetreat(NetworkMessages.Retreat msg) {

    }

    @Override
    public void onCardDied(NetworkMessages.CardDied msg) {

    }

}
