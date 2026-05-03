package io.github.squidecim.genialtcg;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
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
    public Skin skin;
    public BitmapFont uiFont;

    @Override
    public void create() {
        skin = buildSkin();
        loadCardsFromJson();
        setScreen(new FirstScreen(this));
    }

    private Skin buildSkin() {
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();

        // Font UI — DejaVuSans Regular 16pt (utilisée directement, sans passer par le skin)
        FreeTypeFontGenerator generatorRegular = new FreeTypeFontGenerator(
            Gdx.files.internal("ui/dejavu-sans/DejaVuSans.ttf")
        );
        parameter.size = 16;
        uiFont = generatorRegular.generateFont(parameter);
        generatorRegular.dispose();

        // Font titres — DejaVuSans Bold 100pt
        FreeTypeFontGenerator generatorBold = new FreeTypeFontGenerator(
            Gdx.files.internal("ui/dejavu-sans/DejaVuSans-Bold.ttf")
        );
        parameter.size = 100;
        BitmapFont titleFont = generatorBold.generateFont(parameter);
        generatorBold.dispose();

        skin.add("title-font", titleFont, BitmapFont.class);
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = titleFont;
        titleStyle.fontColor = Color.WHITE;
        skin.add("title", titleStyle, Label.LabelStyle.class);

        return skin;
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
                    if (spec.has("cibles")) sCibles = spec
                        .get("cibles")
                        .asStringArray();
                    if (spec.has("variables")) sVars = spec
                        .get("variables")
                        .asStringArray();
                    if (spec.has("valeurs")) {
                        JsonValue vArr = spec.get("valeurs");
                        sVals = new Object[vArr.size];
                        for (int i = 0; i < vArr.size; i++) {
                            JsonValue v = vArr.get(i);
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
                allCardsMap.put(card.getAtlasRegionName(), card);
            }
        } catch (Exception e) {
            Gdx.app.error(
                "GenialTCG",
                "Erreur lecture JSON pays : " + e.getMessage()
            );
        }
    }

    private void loadSimpleCardsJson(String path) {
        try {
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal(path));

            for (JsonValue entry : root) {
                String nom = entry.getString("nom", "Inconnu");
                String id = entry.getString("id", "N/A");

                // --- Parsing des conditions ---
                String type = "N/A";
                String[] condTypes    = null;
                String[] condTerrains = null;
                String[] condRangs    = null;
                int    condEtatMin    = 0;
                int    condEtatMax    = 0;
                String condStatMinKey = null;
                int    condStatMinVal = 0;
                String condStatMaxKey = null;
                int    condStatMaxVal = 0;
                StringBuilder condBuf = new StringBuilder();

                JsonValue condEntry = entry.get("cond");
                if (condEntry != null && !condEntry.isNull()) {
                    if (condEntry.has("type")) {
                        JsonValue tv = condEntry.get("type");
                        condTypes = tv.isArray() ? tv.asStringArray() : new String[]{ tv.asString() };
                        type = condTypes[0];
                        appendCond(condBuf, "type : " + join(condTypes));
                    }
                    if (condEntry.has("terrain")) {
                        JsonValue tv = condEntry.get("terrain");
                        condTerrains = tv.isArray() ? tv.asStringArray() : new String[]{ tv.asString() };
                        appendCond(condBuf, "terrain : " + join(condTerrains));
                    }
                    if (condEntry.has("rang")) {
                        JsonValue rv = condEntry.get("rang");
                        condRangs = rv.isArray() ? rv.asStringArray() : new String[]{ rv.asString() };
                        appendCond(condBuf, "rang : " + join(condRangs));
                    }
                    if (condEntry.has("etatMin")) {
                        condEtatMin = condEntry.getInt("etatMin");
                        appendCond(condBuf, "≥ " + condEtatMin + " PV");
                    }
                    if (condEntry.has("etatMax")) {
                        condEtatMax = condEntry.getInt("etatMax");
                        appendCond(condBuf, "≤ " + condEtatMax + " PV");
                    }
                    if (condEntry.has("statMin")) {
                        JsonValue sm = condEntry.get("statMin").child();
                        condStatMinKey = sm.name();
                        condStatMinVal = sm.asInt();
                        appendCond(condBuf, condStatMinKey + " ≥ " + condStatMinVal);
                    }
                    if (condEntry.has("statMax")) {
                        JsonValue sm = condEntry.get("statMax").child();
                        condStatMaxKey = sm.name();
                        condStatMaxVal = sm.asInt();
                        appendCond(condBuf, condStatMaxKey + " ≤ " + condStatMaxVal);
                    }
                }
                String condStr = condBuf.length() > 0 ? condBuf.toString() : "—";

                String[] cibles = new String[0];
                String[] variables = new String[0];
                Object[] valeurs = new Object[0];

                if (entry.has("cibles")) cibles = entry
                    .get("cibles")
                    .asStringArray();

                if (entry.has("variables")) {
                    JsonValue varsArr = entry.get("variables");
                    variables = new String[varsArr.size];
                    for (int i = 0; i < varsArr.size; i++) {
                        JsonValue v = varsArr.get(i);
                        variables[i] = (v == null || v.isNull())
                            ? null
                            : v.asString();
                    }
                }

                if (entry.has("valeurs")) {
                    JsonValue vArr = entry.get("valeurs");
                    valeurs = new Object[vArr.size];
                    for (int i = 0; i < vArr.size; i++) {
                        JsonValue v = vArr.get(i);
                        valeurs[i] = v.isArray()
                            ? v.asStringArray()
                            : v.asInt();
                    }
                }

                CardData card = new CardData(
                    nom,
                    id,
                    "N/A",
                    type,
                    0,
                    0,
                    new int[5],
                    0,
                    cibles,
                    variables,
                    valeurs
                );
                card.cond         = condStr;
                card.condTypes    = condTypes;
                card.condTerrains = condTerrains;
                card.condRangs    = condRangs;
                card.condEtatMin  = condEtatMin;
                card.condEtatMax  = condEtatMax;
                card.condStatMinKey = condStatMinKey;
                card.condStatMinVal = condStatMinVal;
                card.condStatMaxKey = condStatMaxKey;
                card.condStatMaxVal = condStatMaxVal;
                allCardsMap.put(card.getAtlasRegionName(), card);
            }
        } catch (Exception e) {
            Gdx.app.error(
                "GenialTCG",
                "Erreur lecture JSON " + path + " : " + e.getMessage()
            );
        }
    }

    private static void appendCond(StringBuilder sb, String part) {
        if (sb.length() > 0) sb.append(" + ");
        sb.append(part);
    }

    private static String join(String[] arr) {
        if (arr == null || arr.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    @Override
    public void dispose() {
        if (getScreen() != null) getScreen().dispose();
        if (skin != null) skin.dispose();
        super.dispose();
    }
}
