package io.github.squidecim.genialtcg;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.squidecim.genialtcg.mainMenu.FirstScreen;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.CardsStackData;
import java.util.HashMap;
import java.util.Map;

public class GenialTCG extends Game {

    public Array<CardsStackData> savedDecks = new Array<>();
    public Map<String, CardData> allCardsMap = new HashMap<>();

    @Override
    public void create() {
        loadCardsFromJson();
        loadDecks();
        setScreen(new FirstScreen(this));
    }

    private void loadCardsFromJson() {
        loadPaysJson();
        loadSimpleCardsJson("JSON/actions.json");
        loadSimpleCardsJson("JSON/outils.json");
    }

    private void loadPaysJson() {
        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal("JSON/pays.json"));

            for (JsonValue entry : root) {
                JsonValue s = entry.get("statistiques");
                int[] statsArray = (s != null)
                    ? new int[] {
                          s.getInt("puissance", 0),
                          s.getInt("economie", 0),
                          s.getInt("ressources", 0),
                          s.getInt("technologie", 0),
                          s.getInt("stabilite", 0),
                      }
                    : new int[5];

                JsonValue spec = entry.get("special");
                int sCout = 0;
                String[] sCibles = new String[0];
                String[] sVars = new String[0];
                Object[] sVals = new Object[0];

                if (spec != null) {
                    sCout = spec.getInt("cout", 0);
                    if (spec.has("cibles")) sCibles = spec.get("cibles").asStringArray();
                    if (spec.has("variables")) sVars = spec.get("variables").asStringArray();
                    if (spec.has("valeurs")) {
                        JsonValue vArr = spec.get("valeurs");
                        sVals = new Object[vArr.size];
                        for (int i = 0; i < vArr.size; i++) {
                            JsonValue v = vArr.get(i);
                            sVals[i] = v.isArray() ? v.asStringArray() : v.asInt();
                        }
                    }
                }

                CardData card = new CardData(
                    entry.getString("nom", "Inconnu"),
                    entry.getString("id", "N/A"),
                    entry.getString("rang", "Inconnu"),
                    entry.getString("type", "Inconnu"),
                    entry.getInt("cout", 0),
                    entry.getInt("etat", 0),
                    statsArray,
                    sCout,
                    sCibles,
                    sVars,
                    sVals
                );
                allCardsMap.put(card.getAtlasRegionName(), card);
            }
        } catch (Exception e) {
            Gdx.app.error("GenialTCG", "Erreur lecture JSON pays : " + e.getMessage());
        }
    }

    private void loadSimpleCardsJson(String path) {
        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal(path));

            for (JsonValue entry : root) {
                String nom = entry.getString("nom", "Inconnu");
                String id = entry.getString("id", "N/A");

                String type = "N/A";
                String condStr = "—";
                JsonValue condEntry = entry.get("cond");
                if (condEntry != null && !condEntry.isNull()) {
                    if (condEntry.has("type")) {
                        JsonValue tv = condEntry.get("type");
                        String val = tv.isArray() ? tv.getString(0) : tv.asString();
                        type = val;
                        condStr = "type : " + val;
                    } else if (condEntry.has("terrain")) {
                        JsonValue tv = condEntry.get("terrain");
                        condStr = "terrain : " + (tv.isArray() ? tv.getString(0) : tv.asString());
                    }
                }

                String[] cibles = new String[0];
                String[] variables = new String[0];
                Object[] valeurs = new Object[0];

                if (entry.has("cibles")) cibles = entry.get("cibles").asStringArray();

                if (entry.has("variables")) {
                    JsonValue varsArr = entry.get("variables");
                    variables = new String[varsArr.size];
                    for (int i = 0; i < varsArr.size; i++) {
                        JsonValue v = varsArr.get(i);
                        variables[i] = (v == null || v.isNull()) ? null : v.asString();
                    }
                }

                if (entry.has("valeurs")) {
                    JsonValue vArr = entry.get("valeurs");
                    valeurs = new Object[vArr.size];
                    for (int i = 0; i < vArr.size; i++) {
                        JsonValue v = vArr.get(i);
                        valeurs[i] = v.isArray() ? v.asStringArray() : v.asInt();
                    }
                }

                CardData card = new CardData(
                    nom, id, "N/A", type, 0, 0, new int[5], 0, cibles, variables, valeurs
                );
                card.cond = condStr;
                allCardsMap.put(card.getAtlasRegionName(), card);
            }
        } catch (Exception e) {
            Gdx.app.error("GenialTCG", "Erreur lecture JSON " + path + " : " + e.getMessage());
        }
    }

    public void saveDecks() {
        try {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < savedDecks.size; i++) {
                CardsStackData deck = savedDecks.get(i);
                sb.append("  {\"name\":\"")
                  .append(deck.name.replace("\\", "\\\\").replace("\"", "\\\""))
                  .append("\",\"cards\":[");
                java.util.List<CardData> cards = deck.getCards();
                for (int j = 0; j < cards.size(); j++) {
                    sb.append("\"").append(cards.get(j).getAtlasRegionName()).append("\"");
                    if (j < cards.size() - 1) sb.append(",");
                }
                sb.append("]}");
                if (i < savedDecks.size - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Gdx.files.local("decks.json").writeString(sb.toString(), false);
        } catch (Exception e) {
            Gdx.app.error("GenialTCG", "Erreur sauvegarde decks : " + e.getMessage());
        }
    }

    public void loadDecks() {
        try {
            if (!Gdx.files.local("decks.json").exists()) return;
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.local("decks.json"));
            for (JsonValue deckEntry : root) {
                String name = deckEntry.getString("name", "Sans nom");
                java.util.List<CardData> cards = new java.util.ArrayList<>();
                if (deckEntry.has("cards")) {
                    for (JsonValue cardVal : deckEntry.get("cards")) {
                        CardData card = allCardsMap.get(cardVal.asString());
                        if (card != null) cards.add(card);
                    }
                }
                if (!cards.isEmpty()) savedDecks.add(new CardsStackData(name, cards));
            }
        } catch (Exception e) {
            Gdx.app.error("GenialTCG", "Erreur chargement decks : " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        super.dispose();
    }
}
