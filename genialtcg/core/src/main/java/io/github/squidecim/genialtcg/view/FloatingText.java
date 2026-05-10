package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

public class FloatingText {
    public String text;
    public Vector3 worldPos;
    public float lifetime;
    public float maxLifetime;
    public float scale;
    public float baseScale;
    public Color color;

    public FloatingText(String text, Vector3 worldPos, float lifetime, Color color) {
        this(text, worldPos, lifetime, color, 1f);
    }

    public FloatingText(String text, Vector3 worldPos, float lifetime, Color color, float baseScale) {
        this.text = text;
        this.worldPos = worldPos.cpy();
        this.lifetime = lifetime;
        this.maxLifetime = lifetime;
        this.baseScale = baseScale;
        this.scale = baseScale;
        this.color = color;
    }

    public boolean isDead() { return lifetime <= 0; }

    public void update(float delta) {
        lifetime -= delta;
        worldPos.y += delta * 0.8f;
        float t = 1f - (lifetime / maxLifetime);
        scale = baseScale * (1f + t * 1.5f);
        color.a = lifetime / maxLifetime;
    }
}
