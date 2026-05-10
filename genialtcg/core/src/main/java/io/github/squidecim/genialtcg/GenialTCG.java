package io.github.squidecim.genialtcg;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.github.squidecim.genialtcg.mainMenu.MainScreen;
import io.github.squidecim.genialtcg.mainMenu.ProfileSelectionScreen;
import io.github.squidecim.genialtcg.model.CardData;
import io.github.squidecim.genialtcg.model.CardsStackData;
import io.github.squidecim.genialtcg.network.GameClient;
import io.github.squidecim.genialtcg.network.GameServer;
import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenialTCG extends Game {

    public GameServer currentGameServer;
    public GameClient currentGameClient;

    public Array<CardsStackData> savedDecks = new Array<>();
    public Map<String, CardData> allCardsMap = new HashMap<>();
    public Skin skin;
    public com.badlogic.gdx.audio.Music menuMusic;
    public com.badlogic.gdx.audio.Music gameMusic;
    public com.badlogic.gdx.audio.Music terrainMusic;
    private String currentTerrain = null;
    public BitmapFont uiFont;
    public Sound hoverSound;
    public Sound clickSound;
    public Sound impossibleSound;
    public Sound posingCardsSound;
    public Sound takingCardsSound;
    public Sound overpassCardsSound;
    public Sound switchSound;
    public Sound damageSound;
    public Sound specialEffectSound;
    public Sound terrainChangeSound;
    public float uiSoundVolume = 0.5f;
    public float gameSoundVolume = 0.5f;

    public String playerPseudo = "";

    private FileChannel profileLockChannel;
    private FileLock profileFileLock;

    public float globalBrightness = 1.0f;
    public SpriteBatch overlayBatch;
    public Texture blackOverlay;

    private boolean suppressClickSound = false;

    @Override
    public void create() {
        skin = buildSkin();
        loadCardsFromJson();

        overlayBatch = new SpriteBatch();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        blackOverlay = new Texture(pixmap);
        pixmap.dispose();

        try {
            hoverSound = Gdx.audio.newSound(
                Gdx.files.internal("audio/UI/overpass_button.mp3")
            );
            clickSound = Gdx.audio.newSound(
                Gdx.files.internal("audio/UI/pressed_button.mp3")
            );
            impossibleSound = Gdx.audio.newSound(
                Gdx.files.internal("audio/UI/impossible_action.mp3")
            );
            posingCardsSound = Gdx.audio.newSound(
                Gdx.files.internal("audio/game_effect/cards/posing_cards.mp3")
            );
            takingCardsSound = Gdx.audio.newSound(
                Gdx.files.internal("audio/game_effect/cards/taking_cards.mp3")
            );
            overpassCardsSound = Gdx.audio.newSound(
                Gdx.files.internal("audio/game_effect/cards/overpasscards.mp3")
            );
            switchSound = Gdx.audio.newSound(
                Gdx.files.internal("audio/game_effect/cards/switch.mp3")
            );
            damageSound = Gdx.audio.newSound(
                Gdx.files.internal("audio/game_effect/damage/unique.mp3")
            );
            specialEffectSound = Gdx.audio.newSound(
                Gdx.files.internal(
                    "audio/game_effect/damage/special_effect.mp3"
                )
            );
            terrainChangeSound = Gdx.audio.newSound(
                Gdx.files.internal(
                    "audio/game_effect/terrains/terrain_change.mp3"
                )
            );
        } catch (Exception e) {
            Gdx.app.log("Audio", "Erreur chargement sons boutons");
        }

        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                unlockCurrentProfile();
                if (currentGameClient != null) {
                    currentGameClient.disconnect();
                }
                if (currentGameServer != null) {
                    currentGameServer.stop();
                }
            })
        );

        setScreen(new ProfileSelectionScreen(this));
    }

    @Override
    public void render() {
        super.render();

        if (globalBrightness < 1.0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            overlayBatch.begin();
            float darkness = 1.0f - globalBrightness;
            overlayBatch.setColor(0f, 0f, 0f, darkness);
            overlayBatch.draw(
                blackOverlay,
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
            );
            overlayBatch.end();

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    public static float curveVolume(float v) {
        return v * v;
    }

    public void playImpossibleSound() {
        suppressClickSound = true;
        if (impossibleSound != null) impossibleSound.play(uiSoundVolume);
    }

    public void soundifyButton(TextButton btn) {
        btn.addListener(
            new InputListener() {
                @Override
                public void enter(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    Actor fromActor
                ) {
                    if (pointer == -1 && hoverSound != null) hoverSound.play(
                        uiSoundVolume
                    );
                }

                @Override
                public boolean touchDown(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    return true;
                }

                @Override
                public void touchUp(
                    InputEvent event,
                    float x,
                    float y,
                    int pointer,
                    int button
                ) {
                    if (
                        !suppressClickSound && clickSound != null
                    ) clickSound.play(uiSoundVolume * 0.15f);
                    suppressClickSound = false;
                }
            }
        );
    }

    private static File getLockFile(String pseudo) {
        File dir = new File(
            System.getProperty("user.home"),
            ".genialtcg/locks"
        );
        dir.mkdirs();
        return new File(dir, "profile_" + pseudo + ".lock");
    }

    public void lockProfile(String pseudo) {
        unlockCurrentProfile();
        try {
            FileChannel ch = FileChannel.open(
                getLockFile(pseudo).toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            );
            FileLock fl = ch.tryLock();
            if (fl != null) {
                profileLockChannel = ch;
                profileFileLock = fl;
            } else {
                ch.close();
            }
        } catch (Exception e) {
            Gdx.app.log("Lock", "Erreur verrou profil : " + e.getMessage());
        }
    }

    public void unlockCurrentProfile() {
        try {
            if (profileFileLock != null) {
                profileFileLock.release();
                profileFileLock = null;
            }
            if (profileLockChannel != null) {
                profileLockChannel.close();
                profileLockChannel = null;
            }
        } catch (Exception e) {
            Gdx.app.log("Lock", "Erreur libération verrou : " + e.getMessage());
        }
    }

    public boolean isProfileLocked(String pseudo) {
        File f = getLockFile(pseudo);
        if (!f.exists()) return false;
        try (
            FileChannel ch = FileChannel.open(
                f.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            );
            FileLock fl = ch.tryLock()
        ) {
            return fl == null;
        } catch (Exception e) {
            return false;
        }
    }

    public List<String> getSavedProfiles() {
        Preferences index = Gdx.app.getPreferences("GenialTCG_Profiles");
        String raw = index.getString("profiles", "");
        List<String> list = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String p : raw.split(",")) {
                if (!p.trim().isEmpty()) list.add(p.trim());
            }
        }
        return list;
    }

    public void loadProfile(String pseudo) {
        playerPseudo = pseudo;
        lockProfile(pseudo);
        savedDecks.clear();
        Preferences prefs = Gdx.app.getPreferences(
            "GenialTCG_Profile_" + pseudo
        );
        int count = prefs.getInteger("deck_count", 0);
        for (int i = 0; i < count; i++) {
            String name = prefs.getString("deck_" + i + "_name", "");
            String cardsStr = prefs.getString("deck_" + i + "_cards", "");
            if (name.isEmpty()) continue;
            List<CardData> cards = new ArrayList<>();
            if (!cardsStr.isEmpty()) {
                for (String regionName : cardsStr.split(",")) {
                    CardData card = allCardsMap.get(regionName.trim());
                    if (card != null) cards.add(card);
                }
            }
            savedDecks.add(new CardsStackData(name, cards));
        }

        uiSoundVolume = curveVolume(prefs.getFloat("ui_sound_volume", 0.5f));
        gameSoundVolume = curveVolume(prefs.getFloat("game_sound_volume", 0.5f));
        globalBrightness = prefs.getFloat("brightness", 1.0f);

        String displayMode = prefs.getString("display_mode", "Plein ecran");
        if ("Plein ecran".equals(displayMode)) {
            Gdx.graphics.setUndecorated(false);
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else if ("Fenetre sans bordure".equals(displayMode)) {
            Gdx.graphics.setUndecorated(true);
            Gdx.graphics.setWindowedMode(
                Gdx.graphics.getDisplayMode().width,
                Gdx.graphics.getDisplayMode().height
            );
        } else {
            Gdx.graphics.setUndecorated(false);
            Gdx.graphics.setWindowedMode(1280, 720);
        }

        float musicVol = curveVolume(prefs.getFloat("music_volume", 0.3f));
        if (menuMusic != null) menuMusic.setVolume(musicVol);
    }

    public void saveProfile() {
        if (playerPseudo.isEmpty()) return;

        Preferences index = Gdx.app.getPreferences("GenialTCG_Profiles");
        String raw = index.getString("profiles", "");
        boolean found = false;
        for (String p : raw.split(",")) {
            if (p.trim().equals(playerPseudo)) {
                found = true;
                break;
            }
        }
        if (!found) {
            index.putString(
                "profiles",
                raw.isEmpty() ? playerPseudo : raw + "," + playerPseudo
            );
            index.flush();
        }

        Preferences prefs = Gdx.app.getPreferences(
            "GenialTCG_Profile_" + playerPseudo
        );
        prefs.putString("pseudo", playerPseudo);
        prefs.putInteger("deck_count", savedDecks.size);
        for (int i = 0; i < savedDecks.size; i++) {
            CardsStackData deck = savedDecks.get(i);
            prefs.putString(
                "deck_" + i + "_name",
                deck.name != null ? deck.name : ""
            );
            StringBuilder sb = new StringBuilder();
            for (CardData card : deck.getCards()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(card.getAtlasRegionName());
            }
            prefs.putString("deck_" + i + "_cards", sb.toString());
        }
        prefs.flush();
    }

    public void renameProfile(String oldPseudo, String newPseudo) {
        Preferences oldPrefs = Gdx.app.getPreferences(
            "GenialTCG_Profile_" + oldPseudo
        );
        Preferences newPrefs = Gdx.app.getPreferences(
            "GenialTCG_Profile_" + newPseudo
        );
        int count = oldPrefs.getInteger("deck_count", 0);
        newPrefs.putString("pseudo", newPseudo);
        newPrefs.putInteger("deck_count", count);
        for (int i = 0; i < count; i++) {
            newPrefs.putString(
                "deck_" + i + "_name",
                oldPrefs.getString("deck_" + i + "_name", "")
            );
            newPrefs.putString(
                "deck_" + i + "_cards",
                oldPrefs.getString("deck_" + i + "_cards", "")
            );
        }
        newPrefs.putString("display_mode",       oldPrefs.getString("display_mode", "Plein ecran"));
        newPrefs.putFloat("music_volume",        oldPrefs.getFloat("music_volume", 0.3f));
        newPrefs.putFloat("ui_sound_volume",     oldPrefs.getFloat("ui_sound_volume", 0.5f));
        newPrefs.putFloat("game_sound_volume",   oldPrefs.getFloat("game_sound_volume", 0.5f));
        newPrefs.putFloat("brightness",          oldPrefs.getFloat("brightness", 1.0f));
        newPrefs.flush();
        oldPrefs.clear();
        oldPrefs.flush();

        Preferences index = Gdx.app.getPreferences("GenialTCG_Profiles");
        String raw = index.getString("profiles", "");
        StringBuilder sb = new StringBuilder();
        for (String p : raw.split(",")) {
            if (!p.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(",");
                sb.append(p.trim().equals(oldPseudo) ? newPseudo : p.trim());
            }
        }
        index.putString("profiles", sb.toString());
        index.flush();

        playerPseudo = newPseudo;
    }

    public void deleteProfile(String pseudo) {
        Preferences index = Gdx.app.getPreferences("GenialTCG_Profiles");
        String raw = index.getString("profiles", "");
        StringBuilder sb = new StringBuilder();
        for (String p : raw.split(",")) {
            if (!p.trim().isEmpty() && !p.trim().equals(pseudo)) {
                if (sb.length() > 0) sb.append(",");
                sb.append(p.trim());
            }
        }
        index.putString("profiles", sb.toString());
        index.flush();

        Preferences prefs = Gdx.app.getPreferences(
            "GenialTCG_Profile_" + pseudo
        );
        prefs.clear();
        prefs.flush();
    }

    private Skin buildSkin() {
        Skin skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        FreeTypeFontGenerator.FreeTypeFontParameter parameter =
            new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.characters =
            FreeTypeFontGenerator.DEFAULT_CHARS +
            "àâäéèêëîïôùûüçœÀÂÄÉÈÊËÎÏÔÙÛÜÇŒæÆ«»€°•";

        FreeTypeFontGenerator generatorRegular = new FreeTypeFontGenerator(
            Gdx.files.internal("ui/dejavu-sans/DejaVuSans.ttf")
        );
        parameter.size = 16;
        uiFont = generatorRegular.generateFont(parameter);
        generatorRegular.dispose();

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
        loadCountryJson();
        loadSimpleCardsJson("JSON/actions.json");
        loadSimpleCardsJson("JSON/outils.json");
    }

    private void loadCountryJson() {
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
                String sNom = "";
                String sDesc = "";
                String[] effectTypes = new String[0];
                int[] effectValues = new int[0];

                if (spec != null) {
                    sCout = spec.getInt("cout", 0);
                    sNom = spec.getString("nom", "");
                    sDesc = spec.getString("description", "");

                    if (spec.has("effets")) {
                        JsonValue effetsArr = spec.get("effets");
                        effectTypes = new String[effetsArr.size];
                        effectValues = new int[effetsArr.size];
                        for (int i = 0; i < effetsArr.size; i++) {
                            JsonValue pair = effetsArr.get(i);
                            effectTypes[i] = pair.getString(0);
                            effectValues[i] = pair.getInt(1);
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
                    sNom,
                    sDesc
                );
                card.specialEffectTypes = effectTypes;
                card.specialEffectValues = effectValues;
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

                String type = "N/A";
                String[] condTypes = null;
                String[] condTerrains = null;
                String[] condRangs = null;
                int condEtatMin = 0;
                int condEtatMax = 0;
                String condStatMinKey = null;
                int condStatMinVal = 0;
                StringBuilder condBuf = new StringBuilder();

                JsonValue condEntry = entry.get("cond");
                if (condEntry != null && !condEntry.isNull()) {
                    if (condEntry.has("type")) {
                        JsonValue tv = condEntry.get("type");
                        condTypes = tv.isArray()
                            ? tv.asStringArray()
                            : new String[] { tv.asString() };
                        type = condTypes[0];
                        appendCond(condBuf, "type : " + join(condTypes));
                    }
                    if (condEntry.has("terrain")) {
                        JsonValue tv = condEntry.get("terrain");
                        condTerrains = tv.isArray()
                            ? tv.asStringArray()
                            : new String[] { tv.asString() };
                        appendCond(condBuf, "terrain : " + join(condTerrains));
                    }
                    if (condEntry.has("rang")) {
                        JsonValue rv = condEntry.get("rang");
                        condRangs = rv.isArray()
                            ? rv.asStringArray()
                            : new String[] { rv.asString() };
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
                        appendCond(
                            condBuf,
                            condStatMinKey + " ≥ " + condStatMinVal
                        );
                    }
                }
                String condStr =
                    condBuf.length() > 0 ? condBuf.toString() : "—";

                String[] effectTypes = new String[0];
                int[] effectValues = new int[0];

                if (entry.has("effets")) {
                    JsonValue effetsArr = entry.get("effets");
                    effectTypes = new String[effetsArr.size];
                    effectValues = new int[effetsArr.size];
                    for (int i = 0; i < effetsArr.size; i++) {
                        JsonValue pair = effetsArr.get(i);
                        effectTypes[i] = pair.getString(0);
                        effectValues[i] =
                            pair.size > 1 && pair.get(1).isNumber()
                                ? pair.get(1).asInt()
                                : 0;
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
                    "",
                    ""
                );
                card.specialEffectTypes = effectTypes;
                card.specialEffectValues = effectValues;
                card.cond = condStr;
                card.condTypes = condTypes;
                card.condTerrains = condTerrains;
                card.condRangs = condRangs;
                card.condEtatMin = condEtatMin;
                card.condEtatMax = condEtatMax;
                card.condStatMinKey = condStatMinKey;
                card.condStatMinVal = condStatMinVal;
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

    public static String getTerrainAudioPath(String terrain) {
        if (terrain == null) return null;
        switch (terrain) {
            case "Désertique":
                return "audio/game_effect/terrains/desertique.mp3";
            case "Glacial":
                return "audio/game_effect/terrains/glacial.mp3";
            case "Montagneux":
                return "audio/game_effect/terrains/montagneux.mp3";
            case "Océanique":
                return "audio/game_effect/terrains/oceanique.mp3";
            case "Tropical":
                return "audio/game_effect/terrains/tropical.mp3";
            default:
                return null;
        }
    }

    public static float getTerrainVolumeScale(String terrain) {
        if (terrain == null) return 1f;
        switch (terrain) {
            case "Désertique":
                return 0.5f;
            case "Glacial":
                return 0.25f;
            case "Montagneux":
                return 0.7f;
            case "Océanique":
                return 0.12f;
            case "Tropical":
                return 0.1f;
            default:
                return 1.0f;
        }
    }

    public void playTerrainMusic(String terrain) {
        stopTerrainMusic();
        String path = getTerrainAudioPath(terrain);
        if (path == null) return;
        try {
            currentTerrain = terrain;
            terrainMusic = Gdx.audio.newMusic(Gdx.files.internal(path));
            terrainMusic.setLooping(true);
            terrainMusic.setVolume(
                gameSoundVolume * getTerrainVolumeScale(terrain)
            );
            terrainMusic.play();
        } catch (Exception e) {
            Gdx.app.log("Audio", "Erreur son terrain : " + terrain);
        }
    }

    public void updateTerrainMusicVolume() {
        if (
            terrainMusic != null && currentTerrain != null
        ) terrainMusic.setVolume(
            gameSoundVolume * getTerrainVolumeScale(currentTerrain)
        );
    }

    public void stopTerrainMusic() {
        if (terrainMusic != null) {
            terrainMusic.stop();
            terrainMusic.dispose();
            terrainMusic = null;
        }
        currentTerrain = null;
    }

    public void playGameMusic() {
        if (menuMusic != null) menuMusic.pause();
        if (gameMusic != null) {
            if (!gameMusic.isPlaying()) gameMusic.play();
            return;
        }
        try {
            Preferences prefs = Gdx.app.getPreferences("GenialTCG_Profile_" + playerPseudo);
            gameMusic = Gdx.audio.newMusic(
                Gdx.files.internal("audio/music/game_theme.mp3")
            );
            gameMusic.setLooping(true);
            gameMusic.setVolume(
                curveVolume(prefs.getFloat("music_volume", 0.3f))
            );
            gameMusic.play();
        } catch (Exception e) {
            Gdx.app.log("Audio", "Erreur lecture game theme");
        }
    }

    public void stopGameMusic() {
        if (gameMusic != null) {
            gameMusic.stop();
            gameMusic.dispose();
            gameMusic = null;
        }
        if (menuMusic != null && !menuMusic.isPlaying()) menuMusic.play();
    }

    public void cleanupCurrentGame() {
        if (currentGameClient != null) {
            currentGameClient.disconnect();
            currentGameClient = null;
        }
        if (currentGameServer != null) {
            currentGameServer.stop();
            currentGameServer = null;
        }
    }

    @Override
    public void dispose() {
        unlockCurrentProfile();
        cleanupCurrentGame();

        if (getScreen() != null) getScreen().dispose();
        if (skin != null) skin.dispose();
        if (hoverSound != null) hoverSound.dispose();
        if (clickSound != null) clickSound.dispose();
        if (impossibleSound != null) impossibleSound.dispose();
        if (posingCardsSound != null) posingCardsSound.dispose();
        if (takingCardsSound != null) takingCardsSound.dispose();
        if (overpassCardsSound != null) overpassCardsSound.dispose();
        if (switchSound != null) switchSound.dispose();
        if (damageSound != null) damageSound.dispose();
        if (specialEffectSound != null) specialEffectSound.dispose();
        if (terrainChangeSound != null) terrainChangeSound.dispose();

        if (blackOverlay != null) blackOverlay.dispose();
        if (overlayBatch != null) overlayBatch.dispose();
        if (uiFont != null) uiFont.dispose();
        if (menuMusic != null) menuMusic.dispose();
        if (gameMusic != null) gameMusic.dispose();
        stopTerrainMusic();

        super.dispose();
    }
}
