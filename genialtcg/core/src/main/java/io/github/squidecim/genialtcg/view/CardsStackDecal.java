package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

public class CardsStackDecal {

    private float thickness = 0.007f;
    private Model model;
    private ModelInstance instance;
    public int nbrCards;
    private float width;
    private float height;
    private TextureRegion cardTexture;

    public CardsStackDecal(TextureRegion cardTexture, float width, float height, int nbrCards) {
        this.width = width;
        this.height = height;
        this.nbrCards = nbrCards;
        this.cardTexture = cardTexture;
        this.thickness = 0.007f;
        buildModel(cardTexture, width, height, nbrCards);
    }

    private void buildModel(TextureRegion cardTexture, float width, float height, int nbrCards){
        ModelBuilder builder = new ModelBuilder();
        builder.begin();

        MeshPartBuilder mpb;
        if (nbrCards != 0) {

            mpb = builder.part("top", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                new Material(
                    TextureAttribute.createDiffuse(cardTexture),
                    IntAttribute.createCullFace(GL20.GL_BACK)
                ));
            mpb.rect(
                -width / 2, thickness * nbrCards, height / 2,
                width / 2, thickness * nbrCards, height / 2,
                width / 2, thickness * nbrCards, -height / 2,
                -width / 2, thickness * nbrCards, -height / 2,
                0, 1, 0
            );

            mpb = builder.part("front side", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                new Material(
                    ColorAttribute.createDiffuse(0.26f, 0.17f, 0.09f, 1),
                    IntAttribute.createCullFace(GL20.GL_BACK)
                ));
            mpb.rect(
                width / 2, thickness * nbrCards, height / 2,
                -width / 2, thickness * nbrCards, height / 2,
                -width / 2, 0, height / 2,
                width / 2, 0, height / 2,
                0, 0, 1
            );

            mpb = builder.part("left side", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                new Material(
                    ColorAttribute.createDiffuse(0.26f, 0.17f, 0.09f, 1),
                    IntAttribute.createCullFace(GL20.GL_BACK)
                ));
            mpb.rect(
                -width / 2, thickness * nbrCards, height / 2,
                -width / 2, thickness * nbrCards, -height / 2,
                -width / 2, 0, -height / 2,
                -width / 2, 0, height / 2,
                1, 0, 0
            );

            mpb = builder.part("right side", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                new Material(
                    ColorAttribute.createDiffuse(0.26f, 0.17f, 0.09f, 1),
                    IntAttribute.createCullFace(GL20.GL_BACK)
                ));
            mpb.rect(
                width / 2, thickness * nbrCards, -height / 2,
                width / 2, thickness * nbrCards, height / 2,
                width / 2, 0, height / 2,
                width / 2, 0, -height / 2,
                1, 0, 0
            );
        }

        model = builder.end();
        instance = new ModelInstance(model);
    }

    public void setPosition(float x, float y, float z) {
        instance.transform.setToTranslation(x, y, z);
    }

    public void updateCards(int newNbrCards) {

        this.nbrCards = newNbrCards;
        Vector3 pos = instance.transform.getTranslation(new Vector3());
        buildModel(cardTexture, width, height, nbrCards);
        setPosition(pos.x, pos.y, pos.z);
    }

    public void render(ModelBatch batch, Environment env) {
        batch.render(instance, env);
    }



}

