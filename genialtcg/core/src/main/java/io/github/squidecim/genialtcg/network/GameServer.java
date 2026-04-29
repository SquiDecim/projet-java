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

    private final String lobbyCodeForClients;
    private Server server;
    private Map<Connection, String> playerIds = new HashMap<>();
    private int connectedCount = 0;
    private Runnable onBothConnected;
    private Map<Connection, Integer> deckSizes = new HashMap<>();


    public GameServer(String lobbyCode) throws IOException {
        this.lobbyCodeForClients = lobbyCode;
        server = new Server();
        registerClasses(server.getKryo());


        server.addListener(new Listener() {

            @Override
            public void connected(Connection conn) {
                connectedCount++;
                String id = "player" + connectedCount;
                playerIds.put(conn, id);


                NetworkMessages.AssignId msg = new NetworkMessages.AssignId();
                msg.playerId = id;
                conn.sendTCP(msg);

                if (connectedCount == 2) {
                    NetworkMessages.LobbyInfo info = new NetworkMessages.LobbyInfo();
                    info.lobbyCode = lobbyCodeForClients;
                    conn.sendTCP(info);
                }

                NetworkMessages.PlayerJoined joined = new NetworkMessages.PlayerJoined();
                joined.playerName = "Joueur " + connectedCount;
                joined.playerCount = connectedCount;
                server.sendToAllTCP(joined);

                if (connectedCount == 2 && onBothConnected != null) {
                    onBothConnected.run();
                }


            }

            @Override
            public void disconnected(Connection conn) {
                String id = playerIds.remove(conn);
                if (id != null) {
                    connectedCount--;
                    NetworkMessages.PlayerJoined update = new NetworkMessages.PlayerJoined();
                    update.playerCount = connectedCount;
                    update.playerName = "";
                    server.sendToAllTCP(update);
                }
            }

            @Override
            public void received(Connection conn, Object obj) {
                String playerId = playerIds.get(conn);
                if (playerId == null) return;
                if (obj instanceof NetworkMessages.DrawCard) {
                    String pid = playerIds.get(conn);
                    int size = deckSizes.getOrDefault(conn, 0);
                    if (size > 0) {
                        deckSizes.put(conn, size - 1);
                        NetworkMessages.CardDrawn drawn = new NetworkMessages.CardDrawn();
                        drawn.playerId = pid;
                        drawn.newDeckSize = size - 1;
                        server.sendToAllTCP(drawn);
                    }
                } else if (obj instanceof NetworkMessages.GameStart) {
                    server.sendToAllTCP(obj);
                } else if (obj instanceof NetworkMessages.DeckSize) {
                    deckSizes.put(conn, ((NetworkMessages.DeckSize) obj).size);
                }
            }
        });

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
        kryo.register(NetworkMessages.LobbyInfo.class);
        kryo.register(NetworkMessages.DeckSize.class);
    }
}
