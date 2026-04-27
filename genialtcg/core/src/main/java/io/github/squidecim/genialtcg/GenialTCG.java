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
                try {
                    String country = entry.getString("nom", "Inconnu");

                    // Lecture sécurisée des statistiques
                    JsonValue s = entry.get("statistiques");
                    int[] statsArray = new int[5];
                    if (s != null) {
                        statsArray[0] = s.getInt("puissance", 0);
                        statsArray[1] = s.getInt("economie", 0);
                        statsArray[2] = s.getInt("ressources", 0);
                        statsArray[3] = s.getInt("technologie", 0);
                        statsArray[4] = s.getInt("stabilite", 0);
                    }

                    // Lecture sécurisée du bloc spécial
                    JsonValue spec = entry.get("special");
                    int sCout = 0;
                    String[] sCibles = new String[0];
                    String[] sVars = new String[0];
                    int[] sVals = new int[0];

                    if (spec != null) {
                        sCout = spec.getInt("cout", 0);

                        // Sécurité CRITIQUE : on vérifie si c'est bien un tableau avant d'extraire
                        if (
                            spec.has("cibles") && spec.get("cibles").isArray()
                        ) sCibles = spec.get("cibles").asStringArray();

                        if (
                            spec.has("variables") &&
                            spec.get("variables").isArray()
                        ) sVars = spec.get("variables").asStringArray();

                        if (
                            spec.has("valeurs") && spec.get("valeurs").isArray()
                        ) sVals = spec.get("valeurs").asIntArray();
                    }

                    CardData card = new CardData(
                        country,
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

                    allCardsMap.put(country, card);
                } catch (Exception e) {
                    Gdx.app.error(
                        "GenialTCG",
                        "Erreur sur une carte spécifique : " + e.getMessage()
                    );
                }
            }
            Gdx.app.log(
                "GenialTCG",
                allCardsMap.size() + " cartes chargées avec succès."
            );
        } catch (Exception e) {
            Gdx.app.error(
                "GenialTCG",
                "Erreur critique de lecture JSON : " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
