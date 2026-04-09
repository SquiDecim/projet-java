package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.math.Vector3;

public class CardSlot {
    Vector3 position;
    float yaw, pitch, roll;
    private CardDecal card;

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

    public CardDecal getCard() {
        return this.card;
    }
}
