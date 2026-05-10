package io.github.squidecim.genialtcg.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;

public class GameClient {

    private Client client;
    private NetworkListener listener;
    private Runnable onDisconnected;

    public interface NetworkListener {
        void onAssignId(NetworkMessages.AssignId msg);
        void onGameStart(NetworkMessages.GameStart msg);
        void onCardDrawn(NetworkMessages.CardDrawn msg);
        void onCardPlayed(NetworkMessages.CardPlayed msg);
        void onTurnChanged(NetworkMessages.TurnChanged msg);
        void onPlayerJoined(NetworkMessages.PlayerJoined msg);
        void onLobbyInfo(NetworkMessages.LobbyInfo msg);
        void onCreditsUpdate(NetworkMessages.CreditsUpdate obj);
        void onNormalAttack(NetworkMessages.NormalAttack msg);
        void onRetreat(NetworkMessages.Retreat msg);
        void onCardDied(NetworkMessages.CardDied msg);
        void onSpecialAttack(NetworkMessages.SpecialAttack msg);
        void onPlayerQuit();
        void onField(NetworkMessages.Field msg);
        void onChatMessage(NetworkMessages.ChatMessage msg);
    }

    public GameClient(String ip, NetworkListener listener) throws IOException {
        this.listener = listener;
        client = new Client();
        GameServer.registerClasses(client.getKryo());

        client.addListener(new Listener() {
            @Override
            public void received(Connection conn, Object obj) {
                Gdx.app.postRunnable(() -> {
                    NetworkListener current = GameClient.this.listener;
                    if (current == null) return;
                    if (obj instanceof NetworkMessages.AssignId)
                        current.onAssignId((NetworkMessages.AssignId) obj);
                    else if (obj instanceof NetworkMessages.GameStart)
                        current.onGameStart((NetworkMessages.GameStart) obj);
                    else if (obj instanceof NetworkMessages.CardDrawn)
                        current.onCardDrawn((NetworkMessages.CardDrawn) obj);
                    else if (obj instanceof NetworkMessages.CardPlayed)
                        current.onCardPlayed((NetworkMessages.CardPlayed) obj);
                    else if (obj instanceof NetworkMessages.TurnChanged)
                        current.onTurnChanged((NetworkMessages.TurnChanged) obj);
                    else if (obj instanceof NetworkMessages.PlayerJoined)
                        current.onPlayerJoined((NetworkMessages.PlayerJoined) obj);
                    else if (obj instanceof NetworkMessages.LobbyInfo)
                        current.onLobbyInfo((NetworkMessages.LobbyInfo) obj);
                    else if (obj instanceof NetworkMessages.CreditsUpdate)
                        current.onCreditsUpdate((NetworkMessages.CreditsUpdate) obj);
                    else if (obj instanceof NetworkMessages.NormalAttack)
                        current.onNormalAttack((NetworkMessages.NormalAttack) obj);
                    else if (obj instanceof NetworkMessages.Retreat)
                        current.onRetreat((NetworkMessages.Retreat) obj);
                    else if (obj instanceof NetworkMessages.CardDied)
                        current.onCardDied((NetworkMessages.CardDied) obj);
                    else if (obj instanceof NetworkMessages.SpecialAttack)
                        current.onSpecialAttack((NetworkMessages.SpecialAttack) obj);
                    else if (obj instanceof NetworkMessages.PlayerQuit)
                        current.onPlayerQuit();
                    else if (obj instanceof NetworkMessages.Field)
                        current.onField((NetworkMessages.Field) obj);
                    else if (obj instanceof NetworkMessages.ChatMessage)
                        current.onChatMessage((NetworkMessages.ChatMessage) obj);
                });
            }


            @Override
            public void disconnected(Connection conn) { // ← méthode séparée
                Gdx.app.postRunnable(() -> {
                    if (onDisconnected != null) onDisconnected.run();
                });
            }
        });

        client.start();
        client.connect(5000, ip, 54555, 54777);
        // 5000 = timeout en ms, si connexion échoue après 5s → exception
    }

    // méthodes pour envoyer des messages au serveur
    public void sendDrawCard() {
        client.sendTCP(new NetworkMessages.DrawCard());
    }

    public void sendPlayCard(String cardId, String zone, int slotIndex) {
        NetworkMessages.PlayCard msg = new NetworkMessages.PlayCard();
        msg.cardId = cardId;
        msg.zone = zone;
        msg.slotIndex = slotIndex;
        client.sendTCP(msg);
    }

    public void sendEndTurn() {
        client.sendTCP(new NetworkMessages.EndTurn());
    }

    public void sendPlayerQuit() {
        client.sendTCP(new NetworkMessages.PlayerQuit());
    }

    public void disconnect() {
        try {
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setListener(NetworkListener listener) {

        this.listener = listener;
    }

    public void setOnDisconnected(Runnable r) {
        this.onDisconnected = r;
    }

    public void sendGameStart(NetworkMessages.GameStart msg) {
        client.sendTCP(msg);
    }

    public void sendDeckSize(int size) {
        NetworkMessages.DeckSize msg = new NetworkMessages.DeckSize();
        msg.size = size;
        client.sendTCP(msg);
    }

    public void sendReady() {
        client.sendTCP(new NetworkMessages.ReadyToStart());
    }

    public void sendNormalAttack(int damage, int statIndex) {
        NetworkMessages.NormalAttack msg = new NetworkMessages.NormalAttack();
        msg.damage = damage;
        msg.statIndex = statIndex;
        client.sendTCP(msg);
    }

    public void sendRetreat(String benchCardId) {
        NetworkMessages.Retreat msg = new NetworkMessages.Retreat();
        msg.benchCardId = benchCardId;
        client.sendTCP(msg);
    }

    public void sendCreditsUpdate(int credits){
        NetworkMessages.CreditsUpdate msg = new NetworkMessages.CreditsUpdate();
        msg.credits = credits;
        client.sendTCP(msg);
    }

    public void sendSpecialAttack(String[] effectTypes, int[] effectValues, int newDeckSize, String targetBenchCardId) {
        NetworkMessages.SpecialAttack msg = new NetworkMessages.SpecialAttack();
        msg.effectTypes = effectTypes;
        msg.effectValues = effectValues;
        msg.newDeckSize = newDeckSize;
        msg.targetBenchCardId = targetBenchCardId;
        client.sendTCP(msg);
    }

    public void sendCardDied(String region, String emplacement, boolean opponentDied) {
        NetworkMessages.CardDied msg = new NetworkMessages.CardDied();
        msg.cardId = region;
        Gdx.app.log("GameClient", "sendCardDied — zone : " + emplacement);
        msg.zone = emplacement;
        msg.isOpponent = opponentDied;
        client.sendTCP(msg);
    }

    public void sendPlayCardWithTarget(String cardId, String zone, int slotIndex, String targetBenchCardId) {
        NetworkMessages.PlayCard msg = new NetworkMessages.PlayCard();
        msg.cardId = cardId;
        msg.zone = zone;
        msg.slotIndex = slotIndex;
        msg.targetBenchCardId = targetBenchCardId;
        client.sendTCP(msg);
    }

    public void sendField(String climat){
        NetworkMessages.Field f = new NetworkMessages.Field();
        f.field = climat;
        client.sendTCP(f);
    }

    public void sendPlayerName(String name) {
        NetworkMessages.SetPlayerName msg = new NetworkMessages.SetPlayerName();
        msg.name = name;
        client.sendTCP(msg);
    }

    public void sendChatMessage(String text) {
        NetworkMessages.ChatMessage msg = new NetworkMessages.ChatMessage();
        msg.text = text;
        client.sendTCP(msg);
    }

}
