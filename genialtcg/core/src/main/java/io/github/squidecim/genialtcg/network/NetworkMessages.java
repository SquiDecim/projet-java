package io.github.squidecim.genialtcg.network;

public class NetworkMessages {

    // Client → Serveur : je veux piocher une carte
    public static class DrawCard {}

    // Client → Serveur : je pose une carte
    public static class PlayCard {
        public String cardId;
        public String zone;     // "bench" ou "table"
        public int slotIndex;
        public String targetBenchCardId;
    }

    // Client → Serveur : je finis mon tour
    public static class EndTurn {}

    // Serveur → Client : voici ton ID de joueur
    public static class AssignId {
        public String playerId; // "player1" ou "player2"
    }

    // Serveur → tous : la partie commence
    public static class GameStart {
        public String firstPlayerId; // qui joue en premier
        public int deckSize;         // taille du deck du joueur qui lance
    }

    // Serveur → tous : quelqu'un a pioché
    public static class CardDrawn {
        public String playerId;   // qui a pioché
        public String cardId;     // quelle carte ("" si c'est l'adversaire)
        public int newDeckSize;   // nouvelle taille du deck
    }

    // Serveur → tous : quelqu'un a joué une carte
    public static class CardPlayed {
        public String playerId;
        public String cardId;
        public String zone;
        public int slotIndex;
        public String targetBenchCardId;
    }

    // Serveur → tous : changement de tour
    public static class TurnChanged {
        public String currentPlayerId;
    }

    // Serveur → tous : un joueur a rejoint le lobby
    public static class PlayerJoined {
        public String playerName;
        public int playerCount; // 1 ou 2
        public String[] playerNames; // noms réels de tous les joueurs connectés
    }

    // Client → Serveur : voici mon pseudo
    public static class SetPlayerName {
        public String name;
    }

    public static class LobbyInfo {
        public String lobbyCode;
    }

    public static class DeckSize {
        public int size;
    }

    public static class CreditsUpdate {
        public String playerId;
        public int credits;
    }

    public static class ReadyToStart {
        public String playerId;
    }

    public static class NormalAttack {
        public int damage;
        public int statIndex;
    }

    public static class Retreat {
        public String playerId;
        public String benchCardId;
    }

    public static class CardDied {
        public String playerId;
        public String cardId;
        public String zone;
        public boolean isOpponent;
    }

    public static class PlayerQuit {}

    public static class SpecialAttack {
        public String[] effectTypes;
        public int[]    effectValues;
        public int      newDeckSize;
        public String targetBenchCardId;
    }

    public static class Field {
        public String field;
    }

    // Client → Serveur : message de chat dans le lobby
    public static class ChatMessage {
        public String senderId;
        public String senderName;
        public String text;
    }

    public static class PointsUpdate {
        public String playerId;
        public int points;
    }

    public static class Win {
        public String winner;
    }

    public static class Lose {
        public String loser;
    }
}
