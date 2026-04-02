package io.github.squidecim.genialtcg;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class CardsStackDecal {

    private boolean hovered = false;
    private float currentY;
    private float baseY;
    private float thickness = 0.035f;
    private Model model;
    private ModelInstance instance;

    public CardsStackDecal(TextureRegion cardTexture, float width, float height, float depth, int nbrCards) {
        ModelBuilder builder = new ModelBuilder();
        builder.begin();

        MeshPartBuilder mpb;

        mpb = builder.part("top", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
            new Material(TextureAttribute.createDiffuse(cardTexture)));
        mpb.rect(
            -0.5f, thickness * nbrCards,  0.7f,
            0.5f, thickness * nbrCards,  0.7f,
            0.5f, thickness * nbrCards, -0.7f,
            -0.5f, thickness * nbrCards, -0.7f,
            0, 1, 0
        );

        model = builder.end();
        instance = new ModelInstance(model);
    }

    public void setPosition(float x, float y, float z) {
        instance.transform.setToTranslation(x, y, z);
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void update(float delta) {
        //Méthode permettant de diminuer l'épaisseur de la pile quand les cartes sont tirés ou posés
    }

    public void setRotation(float yaw, float pitch, float roll) {

    }

    public void addToBatch(DecalBatch batch) {
        batch.add(instance);
    }

    public boolean intersects(Ray ray) {

        //Potentiellement reprendre la logique de la méthode de CardDecal mais rajouter la profondeur (depth) en paramètre de détection jsp encore
        return false;
    }


}

