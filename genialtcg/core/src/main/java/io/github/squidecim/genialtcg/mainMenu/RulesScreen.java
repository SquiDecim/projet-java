package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;

public class RulesScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;
    private Texture blackTexture;

    public RulesScreen(GenialTCG game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        skin = game.skin;

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        // 1. LE TEXTE DES RÈGLES
        String rulesText =
            "RÈGLES OFFICIELLES : GÉNIAL TCG\n\n" +
            "1. CRÉATION DE DECK\n" +
            "Un deck doit comporter exactement 50 cartes. Il doit inclure :\n" +
            "- Au moins 1 carte Pays dont le coût est inférieur à 200 crédits.\n" +
            "- Au maximum 20 cartes Action ou Outil.\n\n" +
            "2. CONDITIONS DE VICTOIRE\n" +
            "Pour gagner une partie, il existe deux solutions :\n" +
            "- Atteindre 6 points de victoire en éliminant les pays adverses.\n" +
            "- Remporter la victoire par forfait si l'adversaire ne peut plus poser de carte Pays sur le poste actif.\n\n" +
            "3. LES NATIONS ET LE COMBAT\n" +
            "Il existe plusieurs manières d'éliminer un pays adverse :\n" +
            "- Les attaques de base (Puissance, Technologie, Ressource, Stabilité) : Le jeu calcule la différence entre la statistique choisie de votre pays et celle du pays adverse. L'attaque inflige des dégâts équivalents à cette différence.\n" +
            "- Les attaques spéciales : Elles sont propres à chaque pays et permettent d'infliger des dégâts et / ou donner des bonus ou des malus à l'adversaire.\n\n" +
            "Rangs des nations :\n" +
            "Chaque nation possède un rang qui détermine les points gagnés par l'adversaire en cas d'élimination :\n" +
            "- Marginal : 1 point\n" +
            "- Émergent : 2 points\n" +
            "- Établi : 3 points\n" +
            "- Dominant : 4 points\n" +
            "- Hégémonie : 5 points\n" +
            "Généralement, plus le rang d'une carte est élevé, plus ses statistiques sont puissantes, mais plus la perte du pays rapportera de points à l'adversaire.\n\n" +
            "Types de nations :\n" +
            "Une nation appartient à l'un des types suivants : Économique | Renseignement | Isolationniste | Militaire | Diplomatique.\n\n" +
            "4. SYSTÈME D'ARGENT ET D'ÉCONOMIE\n" +
            "Chaque pays possède une statistique d'Économie qui rapporte des crédits à chaque début de tour, en fonction de sa position :\n" +
            "- Dans la main : La carte ne rapporte rien (0 %).\n" +
            "- Sur le banc : La carte rapporte 20 % de sa statistique d'Économie.\n" +
            "- Sur le poste actif : La carte rapporte 100 % de sa statistique d'Économie.\n\n" +
            "Plusieurs actions dans le jeu nécessitent de dépenser des crédits :\n" +
            "- Poser des cartes : Payer le coût de pose pour placer une carte sur le banc ou sur le terrain.\n" +
            "- Battre en retraite : Payer pour échanger la carte du poste actif avec une carte du banc.\n\n" +
            "5. LES CARTES ACTION ET OUTIL\n" +
            "En plus des cartes Pays, il existe deux autres types de cartes :\n" +
            "- Les cartes Outil : Elles s'attachent à un pays positionné sur le poste actif ou sur le banc. Elles permettent d'augmenter une ou plusieurs statistiques de la nation à laquelle elles sont liées.\n" +
            "- Les cartes Action : Ce sont des consommables offrant diverses possibilités, comme piocher de nouvelles cartes, soigner une ou plusieurs nations, ou changer le type de terrain.\n\n" +
            "6. LE TERRAIN\n" +
            "Au début du jeu, le terrain est de type \"Tempéré\". Le terrain reste fixe pour toute la durée de la partie, sauf si un joueur utilise une carte Action de changement de terrain. Les terrains appliquent des effets spécifiques en fonction du type des nations en jeu.";

        Label textLabel = new Label(rulesText, skin);
        textLabel.setWrap(true);
        textLabel.setAlignment(Align.topLeft);

        // zone de texte defillable
        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(new Color(0f, 0f, 0f, 0.85f)); // Noir avec 85% d'opacité
        bgPixmap.fill();
        blackTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        Table innerTable = new Table();
        innerTable.setBackground(new TextureRegionDrawable(blackTexture));
        // On ajoute le texte dans cette boîte avec des marges (pad) de 20 pixels
        innerTable.add(textLabel).width(600).pad(20);

        // 4. le scrollPane permettant de faire defiller
        ScrollPane scrollPane = new ScrollPane(innerTable, skin);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setFadeScrollBars(false);

        // 5. bouton retour
        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new FirstScreen(game));
                }
            }
        );

        // 6. mise en page
        Label title = new Label("REGLES DU JEU", skin, "title");

        mainTable.add(title).padBottom(20).row();
        mainTable.add(scrollPane).width(650).height(400).padBottom(20).row(); // Taille de la fenêtre déroulante
        mainTable.add(btnBack).width(220).height(50);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.18f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
        if (blackTexture != null) blackTexture.dispose();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}
}
