package io.github.squidecim.genialtcg.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.squidecim.genialtcg.GenialTCG;

public class RulesScreen implements Screen {

    private final GenialTCG game;
    private Stage stage;
    private Skin skin;

    public RulesScreen(GenialTCG game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage); // Très important pour que le bouton clique !

        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Titre et texte des règles
        Label title = new Label("REGLES DU JEU", skin);
        Label rulesContent = new Label("1.", skin);

        // --- LE BOUTON RETOUR ---
        TextButton btnBack = new TextButton("Retour au Menu", skin);

        btnBack.addListener(
            new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    // Cette ligne dit au jeu de changer l'écran actuel pour le menu
                    game.setScreen(new FirstScreen(game));
                }
            }
        );

        // Organisation dans la table
        table.add(title).padBottom(30).row();
        table.add(rulesContent).padBottom(40).row();
        table.add(btnBack).width(200).height(50);
    }

    @Override
    public void render(float delta) {
        // On garde le même fond que le menu pour la cohérence
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
        skin.dispose();
    }

    // Méthodes obligatoires de l'interface Screen (vides si inutilisées)
    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        dispose(); // On libère les ressources quand on quitte l'écran
    }
}
