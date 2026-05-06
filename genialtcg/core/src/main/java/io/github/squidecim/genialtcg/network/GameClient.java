package io.github.squidecim.genialtcg.network;

import com.badlogic.gdx.Gdx;
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

    public void disconnect() {
        client.stop();
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

    public void sendNormalAttack(int damage) {
        NetworkMessages.NormalAttack msg = new NetworkMessages.NormalAttack();
        msg.damage = damage;
        client.sendTCP(msg);
    }

}
