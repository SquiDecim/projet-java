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
        // Initialisation de la base de données de cartes au démarrage
        loadCardsFromJson();

        // Une fois les données chargées, on lance le premier écran
        setScreen(new FirstScreen(this));
    }

    private void loadCardsFromJson() {
        try {
            JsonReader reader = new JsonReader();
            // Assurez-vous que le chemin vers votre fichier JSON est correct (dans assets/)
            JsonValue root = reader.parse(
                Gdx.files.internal("data/cards.json")
            );

            for (JsonValue entry : root) {
                // Extraction des données du JSON selon votre format
                String country = entry.getString("nom");
                String id = entry.getString("id");
                String rank = entry.getString("rang");
                String type = entry.getString("type");
                int cost = entry.getInt("cout");
                int pv = entry.getInt("etat");

                // Récupération des statistiques
                JsonValue statsJson = entry.get("statistiques");
                int[] stats = new int[] {
                    statsJson.getInt("puissance"),
                    statsJson.getInt("economie"),
                    statsJson.getInt("ressources"),
                    statsJson.getInt("technologie"),
                    statsJson.getInt("stabilite"),
                };

                // Création et stockage de l'objet CardData complet
                CardData card = new CardData(
                    country,
                    id,
                    rank,
                    type,
                    cost,
                    pv,
                    stats
                );
                allCardsMap.put(country, card);
            }
            Gdx.app.log(
                "GenialTCG",
                allCardsMap.size() + " cartes chargées avec succès."
            );
        } catch (Exception e) {
            Gdx.app.error(
                "GenialTCG",
                "Erreur lors du chargement du JSON : " + e.getMessage()
            );
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
