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
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import io.github.squidecim.genialtcg.model.CardData;

public class CardsStackDecal {

    private Model model;
    private ModelInstance instance;

    public float thickness = 0.007f;
    private TextureRegion cardTexture;
    public int nbrCards;

    private float width;
    private float height;
    private Vector3 position = new Vector3();

    private TextureRegion topTexture = null;
    private CardDecal topCard;


    public CardsStackDecal(TextureRegion cardTexture, float width, float height, int nbrCards) {
        this.width = width;
        this.height = height;
        this.nbrCards = nbrCards;
        this.cardTexture = cardTexture;
        this.thickness = 0.007f;

        buildModel(width, height, nbrCards);
    }

    private void buildModel(float width, float height, int nbrCards) {
        if (model != null) model.dispose();
        ModelBuilder builder = new ModelBuilder();
        builder.begin();

        MeshPartBuilder mpb;
        if (nbrCards != 0) {
            TextureRegion tex = (topTexture != null) ? topTexture : cardTexture;

            mpb = builder.part("top", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
                new Material(
                    TextureAttribute.createDiffuse(tex),
                    IntAttribute.createCullFace(GL20.GL_BACK)
                ));
            mpb.rect(
                -width / 2, thickness * nbrCards, height / 2,
                width / 2,  thickness * nbrCards, height / 2,
                width / 2,  thickness * nbrCards, -height / 2,
                -width / 2, thickness * nbrCards, -height / 2,
                0, 1, 0
            );

            mpb = builder.part("front side", GL20.GL_TRIANGLES,
                VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
                new Material(
                    ColorAttribute.createDiffuse(0.29f, 0.45f, 0.74f, 1),
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
                    ColorAttribute.createDiffuse(0.29f, 0.45f, 0.74f, 1),
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
                    ColorAttribute.createDiffuse(0.29f, 0.45f, 0.74f, 1),
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

    public Vector3 getPosition() {
        return position.cpy();
    }

    public void setCardOnTop(CardDecal card){
        this.topCard = card;
    }

    public CardDecal getCardOnTop(){
        return this.topCard;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        instance.transform.setToTranslation(x, y, z);
    }

    public void updateSize(int newNbrCards) {
        this.nbrCards = newNbrCards;
        Vector3 pos = instance.transform.getTranslation(new Vector3());
        buildModel(width, height, nbrCards);
        setPosition(pos.x, pos.y, pos.z);
    }

    public void render(ModelBatch batch, Environment env) {
        batch.render(instance, env);
    }

    public boolean intersects(Ray ray) {
        float hw = width / 2f;
        float hh = height / 2f;
        float top = position.y + thickness * nbrCards;

        float minX = position.x - hw;
        float maxX = position.x + hw;
        float minZ = position.z - hh;
        float maxZ = position.z + hh;
        float minY = position.y;
        float maxY = top;

        Plane[] planes = {
            new Plane(new Vector3(0, 1, 0),  new Vector3(position.x, maxY, position.z)), // dessus
            new Plane(new Vector3(0, 0, 1),  new Vector3(position.x, position.y, maxZ)), // face avant
            new Plane(new Vector3(1, 0, 0),  new Vector3(maxX, position.y, position.z)), // droite
            new Plane(new Vector3(-1, 0, 0), new Vector3(minX, position.y, position.z)), // gauche
        };

        Vector3 intersection = new Vector3();
        for (Plane plane : planes) {
            if (!Intersector.intersectRayPlane(ray, plane, intersection)) continue;
            if (intersection.x >= minX - 0.001f && intersection.x <= maxX + 0.001f
                && intersection.y >= minY - 0.001f && intersection.y <= maxY + 0.001f
                && intersection.z >= minZ - 0.001f && intersection.z <= maxZ + 0.001f) {
                return true;
            }
        }
        return false;
    }

    public void setTopTexture(TextureRegion region) {
        this.topTexture = region;
        Vector3 pos = instance.transform.getTranslation(new Vector3());
        buildModel(width, height, nbrCards);
        setPosition(pos.x, pos.y, pos.z);
    }
}

