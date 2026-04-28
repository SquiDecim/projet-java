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
        setScreen(new FirstScreen(this));
    }

    private void loadCardsFromJson() {
        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal("JSON/pays.json"));

            for (JsonValue entry : root) {
                // Lecture des stats standards
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

                // Lecture du bloc Spécial
                JsonValue spec = entry.get("special");
                int sCout = 0;
                String[] sCibles = new String[0];
                String[] sVars = new String[0];
                Object[] sVals = new Object[0];

                if (spec != null) {
                    sCout = spec.getInt("cout", 0);
                    if (spec.has("cibles")) sCibles = spec
                        .get("cibles")
                        .asStringArray();
                    if (spec.has("variables")) sVars = spec
                        .get("variables")
                        .asStringArray();

                    // Extraction intelligente des valeurs mixtes
                    if (spec.has("valeurs")) {
                        JsonValue vArr = spec.get("valeurs");
                        sVals = new Object[vArr.size];
                        for (int i = 0; i < vArr.size; i++) {
                            JsonValue v = vArr.get(i);
                            // Si c'est un tableau (ex: [2, "main"]), on le stocke en String[]
                            sVals[i] = v.isArray()
                                ? v.asStringArray()
                                : v.asInt();
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
                allCardsMap.put(card.country, card);
            }
        } catch (Exception e) {
            Gdx.app.error(
                "GenialTCG",
                "Erreur lecture JSON : " + e.getMessage()
            );
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        super.dispose();
    }
}
