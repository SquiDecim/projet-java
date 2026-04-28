package io.github.squidecim.genialtcg.network;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameServer {

    private Server server;
    private Map<Connection, String> playerIds = new HashMap<>();
    private int connectedCount = 0;
    private Runnable onBothConnected; // callback quand les 2 joueurs sont là

    public GameServer() throws IOException {
        server = new Server();
        registerClasses(server.getKryo());

        server.addListener(new Listener() {

            @Override
            public void connected(Connection conn) {
                connectedCount++;
                String id = "player" + connectedCount;
                playerIds.put(conn, id);

                // dit au joueur qui vient de se connecter quel est son ID
                NetworkMessages.AssignId msg = new NetworkMessages.AssignId();
                msg.playerId = id;
                conn.sendTCP(msg);

                // prévient tout le monde qu'un joueur a rejoint
                NetworkMessages.PlayerJoined joined = new NetworkMessages.PlayerJoined();
                joined.playerName = "Joueur " + connectedCount;
                joined.playerCount = connectedCount;
                server.sendToAllTCP(joined);

                // si les 2 joueurs sont là
                if (connectedCount == 2 && onBothConnected != null) {
                    onBothConnected.run();
                }
            }

            @Override
            public void disconnected(Connection conn) {
                playerIds.remove(conn);
                connectedCount--;
            }

            @Override
            public void received(Connection conn, Object obj) {
                String playerId = playerIds.get(conn);
                if (playerId == null) return;

                // pour l'instant on retransmet juste à tout le monde
                // plus tard tu ajouteras la validation ici
                if (obj instanceof NetworkMessages.DrawCard
                    || obj instanceof NetworkMessages.PlayCard
                    || obj instanceof NetworkMessages.EndTurn) {
                    server.sendToAllTCP(obj);
                }
            }
        });

        // démarre le serveur sur les ports 54555 (TCP) et 54777 (UDP)
        server.bind(54555, 54777);
        server.start(); // lance le thread serveur en arrière-plan
    }

    public void setOnBothConnected(Runnable r) {
        this.onBothConnected = r;
    }

    public void stop() {
        server.stop();
    }

    // IMPORTANT : les deux côtés (serveur ET client) doivent
    // enregistrer exactement les mêmes classes dans le même ordre
    public static void registerClasses(Kryo kryo) {
        kryo.register(NetworkMessages.DrawCard.class);
        kryo.register(NetworkMessages.PlayCard.class);
        kryo.register(NetworkMessages.EndTurn.class);
        kryo.register(NetworkMessages.AssignId.class);
        kryo.register(NetworkMessages.GameStart.class);
        kryo.register(NetworkMessages.CardDrawn.class);
        kryo.register(NetworkMessages.CardPlayed.class);
        kryo.register(NetworkMessages.TurnChanged.class);
        kryo.register(NetworkMessages.PlayerJoined.class);
    }
}
