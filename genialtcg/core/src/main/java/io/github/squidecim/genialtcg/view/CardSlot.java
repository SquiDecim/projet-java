package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

import static io.github.squidecim.genialtcg.view.GameView.BENCH_CARD_H;
import static io.github.squidecim.genialtcg.view.GameView.BENCH_CARD_W;

public class CardSlot {
    public Vector3 position;

    public float yaw;
    public float pitch;
    public  float roll;
    private CardDecal card;

    private boolean highlighted = false;

    public CardSlot(Vector3 position, float yaw, float pitch, float roll) {
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    public boolean isEmpty() {
        return card == null;
    }

    public void applyTo(CardDecal card) {
        card.setPosition(this.position.x, this.position.y, this.position.z);
        card.setRotation(this.yaw, this.pitch, this.roll);
    }

    public void setCard(CardDecal card) {
        this.card = card;
    }

    public CardDecal getCard() {
        return this.card;
    }

    public void setHighlighted(boolean h) { this.highlighted = h; }

    public boolean isHighlighted() { return highlighted; }

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
}
