package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import static io.github.squidecim.genialtcg.view.GameView.*;

public class CardSlot {
    public Vector3 position;

    public float yaw;
    public float pitch;
    public  float roll;
    private CardDecal card = null;

    private boolean highlighted = false;

    private Model highlightModel;
    private ModelInstance highlightInstance;

    public String type;

    public CardSlot(Vector3 position, float yaw, float pitch, float roll, String type) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
        this.type = type;
        buildHighlight();
    }

    public boolean isEmpty() {
        return card == null;
    }

    public void applyTo(CardDecal card) {
        card.setPosition(this.position.x, this.position.y, this.position.z);
        card.setRotation(this.yaw, this.pitch, this.roll);
    }

    private void buildHighlight() {
        ModelBuilder builder = new ModelBuilder();
        float width = BENCH_CARD_W;
        float height = BENCH_CARD_H;
        if (type.equals("table")) {
            width = TABLE_CARD_W;
            height = TABLE_CARD_H;
        }
        highlightModel = builder.createBox(
            width, 0.01f, height,
            new Material(
                ColorAttribute.createDiffuse(0.2f, 0.5f, 1f, 0.5f),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.5f),
                IntAttribute.createCullFace(GL20.GL_NONE)
            ),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
        highlightInstance = new ModelInstance(highlightModel);
        highlightInstance.transform.setToTranslation(position.x, position.y + 0.005f, position.z);
    }

    public void setCard(CardDecal card) {
        if (type.equals("table")) {
            this.card = card;
            this.card.buildModel(this.card.frontRegion, this.getCard().backRegion, TABLE_CARD_W, TABLE_CARD_H);
            this.card.setPosition(position.x, position.y, position.z);
        } else {
            this.card = card;
            this.card.setPosition(position.x, position.y, position.z);
        }
    }
    public CardDecal getCard() {
        return this.card;
    }

    public void setHighlighted(boolean h) { this.highlighted = h; }

    public boolean isHighlighted() { return highlighted; }

    public void renderHighlight(ModelBatch batch, Environment env) {
        if (highlighted) batch.render(highlightInstance, env);
    }

    public boolean intersects(Ray ray) {
        Plane plane = new Plane(new Vector3(0, 1, 0), position);
        Vector3 intersection = new Vector3();
        if (!Intersector.intersectRayPlane(ray, plane, intersection)) return false;
        float hw = BENCH_CARD_W / 2f; // à adapter
        float hh = BENCH_CARD_H / 2f;
        return Math.abs(intersection.x - position.x) <= hw
            && Math.abs(intersection.z - position.z) <= hh;
    }

    public Vector3 getPosition() { return position.cpy(); }

    public void dispose() {
        highlightModel.dispose();
    }
}
