package io.github.squidecim.genialtcg.network;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GameServer {

    private final String lobbyCodeForClients;
    private Server server;
    private Map<Connection, String> playerIds = new HashMap<>();
    private int connectedCount = 0;
    private Runnable onBothConnected;
    private Map<Connection, Integer> deckSizes = new HashMap<>();
    private Set<String> readyPlayers = new HashSet<>();



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
                } else if(obj instanceof NetworkMessages.PlayCard) {
                    String pid = playerIds.get(conn);
                    NetworkMessages.CardPlayed played = new NetworkMessages.CardPlayed();
                    played.playerId = pid;
                    played.cardId = ((NetworkMessages.PlayCard) obj).cardId;
                    played.zone = ((NetworkMessages.PlayCard) obj).zone;
                    played.slotIndex = ((NetworkMessages.PlayCard) obj).slotIndex;
                    played.targetBenchCardId = ((NetworkMessages.PlayCard) obj).targetBenchCardId;
                    server.sendToAllTCP(played);
                } else if (obj instanceof NetworkMessages.GameStart) {
                    server.sendToAllTCP(obj);
                } else if (obj instanceof NetworkMessages.DeckSize) {
                    deckSizes.put(conn, ((NetworkMessages.DeckSize) obj).size);
                } else if (obj instanceof NetworkMessages.ReadyToStart) {
                    readyPlayers.add(playerIds.get(conn));
                    if (readyPlayers.size() == 2) {
                        String[] ids = playerIds.values().toArray(new String[0]);
                        String first = ids[new java.util.Random().nextInt(2)];
                        NetworkMessages.TurnChanged turn = new NetworkMessages.TurnChanged();
                        turn.currentPlayerId = first;
                        server.sendToAllTCP(turn);
                    }
                } else if (obj instanceof NetworkMessages.EndTurn) {
                    String pid = playerIds.get(conn);
                    String next = pid.equals("player1") ? "player2" : "player1";
                    NetworkMessages.TurnChanged turn = new NetworkMessages.TurnChanged();
                    turn.currentPlayerId = next;
                    server.sendToAllTCP(turn);
                } else if (obj instanceof NetworkMessages.NormalAttack) {
                    server.sendToAllTCP(obj);
                } else if (obj instanceof NetworkMessages.Retreat) {
                    NetworkMessages.Retreat r = (NetworkMessages.Retreat) obj;
                    r.playerId = playerIds.get(conn);
                    server.sendToAllTCP(r);
                } else if (obj instanceof NetworkMessages.CreditsUpdate){
                    NetworkMessages.CreditsUpdate c = (NetworkMessages.CreditsUpdate) obj;
                    c.playerId = playerIds.get(conn);
                    server.sendToAllTCP(c);
                } else if (obj instanceof NetworkMessages.CardDied) {
                    NetworkMessages.CardDied d = (NetworkMessages.CardDied) obj;
                    String attackerId = playerIds.get(conn);
                    d.playerId = attackerId.equals("player1") ? "player1" : "player2";
                    server.sendToAllTCP(d);
                } else if (obj instanceof NetworkMessages.SpecialAttack) {
                    server.sendToAllTCP(obj);
                } else if (obj instanceof NetworkMessages.PlayerQuit) {
                    server.sendToAllTCP(obj);
                } else if (obj instanceof NetworkMessages.Field) {
                    server.sendToAllTCP(obj);
                }
            }
        });

        server.bind(54555, 54777);
        server.start();
    }

    public void setOnBothConnected(Runnable r) {
        this.onBothConnected = r;
    }

    public void stop() {
        try {
            server.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        kryo.register(NetworkMessages.CreditsUpdate.class);
        kryo.register(NetworkMessages.ReadyToStart.class);
        kryo.register(NetworkMessages.NormalAttack.class);
        kryo.register(NetworkMessages.Retreat.class);
        kryo.register(NetworkMessages.CardDied.class);
        kryo.register(NetworkMessages.PlayerQuit.class);
        kryo.register(String[].class);
        kryo.register(int[].class);
        kryo.register(NetworkMessages.SpecialAttack.class);
        kryo.register(NetworkMessages.Field.class);
    }
}
