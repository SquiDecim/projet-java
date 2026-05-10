package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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

        // creation du bouton retour
        TextButton btnBack = new TextButton("Retour", skin);
        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new MainScreen(game));
                }
            }
        );

        game.soundifyButton(btnBack);

        // table reservee uniquement au bouton retour
        Table topBar = new Table();
        topBar.setFillParent(true);
        topBar.top().left();
        topBar.add(btnBack).width(200).height(50).pad(10);
        stage.addActor(topBar);

        // table principale pour le titre et le texte
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.top();
        stage.addActor(mainTable);

        // le texte des regles
        // le texte des regles
        String rulesText =
            "RÈGLES OFFICIELLES : GÉNIAL TCG\n\n" +
            "1. CONSTRUCTION DU DECK\n" +
            "Chaque joueur doit construire un deck contenant exactement 60 cartes.\n\n" +
            "Le deck doit respecter les contraintes suivantes :\n" +
            "- Il doit contenir au moins 5 cartes Pays dont le coût est inférieur à 200 crédits.\n" +
            "- Il doit contenir exactement 10 cartes Outil.\n" +
            "- Il doit contenir exactement 10 cartes Action.\n\n" +
            "2. CONDITIONS DE VICTOIRE\n" +
            "Il existe deux façons de gagner une partie :\n" +
            "- Victoire aux points : atteindre 6 points de victoire en éliminant des pays adverses.\n" +
            "- Victoire par forfait : gagner si l'adversaire ne peut plus placer de carte Pays en jeu.\n\n" +
            "3. LES NATIONS ET LE SYSTÈME DE COMBAT\n" +
            "Attaques de base :\n" +
            "Chaque nation possède plusieurs statistiques offensives :\n" +
            "- Puissance\n" +
            "- Technologie\n" +
            "- Ressource\n" +
            "- Stabilité\n\n" +
            "Lorsqu'une attaque de base est utilisée, le jeu compare la statistique choisie entre la nation attaquante et la nation adverse. Les dégâts infligés correspondent à la différence entre ces deux valeurs.\n\n" +
            "Attaques spéciales :\n" +
            "Chaque pays possède des attaques spéciales uniques permettant de :\n" +
            "- Infliger des dégâts au pays adverse et/ou à une carte du banc.\n" +
            "- Soigner le pays actif et/ou une carte du banc.\n" +
            "- Voler des crédits au joueur adverse.\n" +
            "- Piocher des cartes dans le deck.\n\n" +
            "4. RANG DES NATIONS ET POINTS DE VICTOIRE\n" +
            "Chaque nation possède un rang qui détermine le nombre de points accordés à l'adversaire lorsqu'elle est éliminée :\n" +
            "- Marginal : 1 point\n" +
            "- Émergent : 2 points\n" +
            "- Établi : 3 points\n" +
            "- Dominant : 4 points\n" +
            "- Hégémonie : 5 points\n\n" +
            "En règle générale, les nations de rang élevé possèdent de meilleures statistiques mais donnent davantage de points lorsqu'elles sont vaincues.\n\n" +
            "5. TYPES DE NATIONS\n" +
            "Chaque nation appartient à l'une des catégories suivantes :\n" +
            "- Économique\n" +
            "- Renseignement\n" +
            "- Isolationniste\n" +
            "- Militaire\n" +
            "- Diplomatique\n\n" +
            "Le type d'une nation peut interagir avec certains effets de cartes ou avec le terrain.\n\n" +
            "6. SYSTÈME D'ÉCONOMIE ET CRÉDITS\n" +
            "Chaque pays possède une statistique d'Économie qui génère des crédits au début de chaque tour.\n\n" +
            "Le montant dépend de l'emplacement de la carte :\n" +
            "- Dans la main : 0 %\n" +
            "- Sur le banc : 20 % de la statistique d'Économie\n" +
            "- Sur le poste actif : 100 % de la statistique d'Économie\n\n" +
            "Les crédits servent notamment à :\n" +
            "- Poser des cartes sur le terrain.\n" +
            "- Placer des cartes sur le banc.\n" +
            "- Effectuer une retraite.\n" +
            "- Payer un effet spécial.\n\n" +
            "Retraite :\n" +
            "Battre en retraite consiste à échanger la nation située sur le poste actif avec une nation du banc en payant le coût requis.\n\n" +
            "7. LES CARTES OUTIL ET ACTION\n" +
            "Cartes Outil :\n" +
            "Les cartes Outil peuvent être attachées à une nation sur le poste actif ou sur le banc.\n" +
            "Elles permettent d'augmenter certaines statistiques du pays équipé ou d'appliquer des malus au pays adverse.\n" +
            "Les cartes Outil n'activent leur effet qu'au moment où elles entrent en jeu.\n\n" +
            "Cartes Action :\n" +
            "Les cartes Action sont des consommables à effet immédiat permettant par exemple de :\n" +
            "- Piocher des cartes.\n" +
            "- Soigner des nations en jeu ou sur le banc.\n" +
            "- Modifier le terrain.\n" +
            "- Bloquer les outils adverses pendant plusieurs tours.\n" +
            "- Effectuer des échanges gratuits, aléatoires ou ciblés selon la carte.\n\n" +
            "8. LE TERRAIN\n" +
            "Au début d'une partie, le terrain actif est de type \"Tempéré\".\n\n" +
            "Le terrain reste identique pendant toute la partie sauf si une carte Action modifie son type.\n\n" +
            "Chaque terrain applique des modificateurs de statistiques de combat selon le type de la nation présente.\n\n" +
            "Tempéré : aucun modificateur.\n\n" +
            "Désertique :\n" +
            "  - Militaire : +10 puissance\n" +
            "  - Économique : −10 ressources\n" +
            "  - Diplomatique : −10 stabilité\n" +
            "  - Renseignement : +10 technologie\n" +
            "  - Isolationniste : +10 stabilité\n" +
            "  - Technologique : −10 économie\n\n" +
            "Tropical :\n" +
            "  - Militaire : −10 puissance\n" +
            "  - Économique : +15 ressources\n" +
            "  - Diplomatique : +10 stabilité\n" +
            "  - Renseignement : +10 puissance\n" +
            "  - Isolationniste : +10 stabilité\n" +
            "  - Technologique : −10 technologie\n\n" +
            "Montagneux :\n" +
            "  - Militaire : +15 puissance\n" +
            "  - Économique : −10 économie\n" +
            "  - Diplomatique : −10 économie\n" +
            "  - Renseignement : +10 puissance\n" +
            "  - Isolationniste : +15 stabilité\n" +
            "  - Technologique : +10 technologie\n\n" +
            "Glacial :\n" +
            "  - Militaire : −10 stabilité\n" +
            "  - Économique : −10 ressources\n" +
            "  - Diplomatique : −15 économie\n" +
            "  - Renseignement : −10 puissance\n" +
            "  - Isolationniste : +15 stabilité\n" +
            "  - Technologique : +15 technologie\n\n" +
            "Océanique :\n" +
            "  - Militaire : +10 puissance\n" +
            "  - Économique : +15 économie\n" +
            "  - Diplomatique : +10 économie\n" +
            "  - Renseignement : +10 puissance\n" +
            "  - Isolationniste : −15 stabilité\n" +
            "  - Technologique : +10 technologie";

        Label.LabelStyle rulesStyle = new Label.LabelStyle(
            game.uiFont,
            Color.WHITE
        );
        Label textLabel = new Label(rulesText, rulesStyle);
        textLabel.setWrap(true);
        textLabel.setAlignment(Align.topLeft);

        // creation de la boite noire rectangulaire
        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(new Color(0f, 0f, 0f, 0.85f));
        bgPixmap.fill();
        blackTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        // table interne avec fond noir et texte
        Table innerTable = new Table();
        innerTable.setBackground(new TextureRegionDrawable(blackTexture));
        innerTable.add(textLabel).width(850).pad(30);

        // scrollpane sans barre de defilement visible
        ScrollPane scrollPane = new ScrollPane(
            innerTable,
            new ScrollPane.ScrollPaneStyle()
        );
        scrollPane.setScrollingDisabled(true, false);

        Label title = new Label("RÈGLES DU JEU", skin, "title");
        title.setFontScale(0.45f);

        // mise en page : titre puis boite noire centree
        mainTable.add(title).padBottom(20).padTop(40).row();
        mainTable.add(scrollPane).width(900).height(950).row();

        // focus molette/souris actif des le debut
        stage.setScrollFocus(scrollPane);
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
