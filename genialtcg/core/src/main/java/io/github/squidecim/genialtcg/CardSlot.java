package io.github.squidecim.genialtcg;

import com.badlogic.gdx.math.Vector3;

class CardSlot {
    Vector3 position;
    float yaw, pitch, roll;
    CardDecal card;

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
        card.setPosition(position.x, position.y, position.z);
        card.setRotation(yaw, pitch, roll);
    }
}
