package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import io.github.squidecim.genialtcg.model.CardData;

public class CardDecal {
    private CardData data;
    private Decal front;
    private Decal back;
    private boolean faceVisible = true;
    private boolean hovered = false;
    private float currentY;
    private float baseY;

    public CardDecal(CardData data, TextureRegion frontRegion, TextureRegion backRegion, float width, float height) {
        this.data = data;
        this.front = Decal.newDecal(frontRegion, true);
        this.back  = Decal.newDecal(backRegion, true);
        this.front.setDimensions(width, height);
        this.back.setDimensions(width, height);
    }

    public void setPosition(float x, float y, float z) {
        this.baseY    = y;
        this.currentY = y;
        this.front.setPosition(x, y, z);
        this.back.setPosition(x, y, z); // plus de 0.01f
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void update(float delta) {
        float targetY = this.hovered ? this.baseY + 1f : this.baseY;
        this.currentY += (targetY - this.currentY) * 8f * delta;

        Vector3 pos = this.front.getPosition();
        front.setPosition(pos.x, this.currentY, pos.z);
        back.setPosition(pos.x, this.currentY, pos.z);
    }

    public void setRotation(float yaw, float pitch, float roll) {
        this.front.setRotation(yaw, pitch, roll);
        this.back.setRotation(yaw + 180, pitch, roll);
    }

    public void flip() {
        this.faceVisible = !this.faceVisible;
    }

    public void addToBatch(DecalBatch batch) {
        if (this.faceVisible) batch.add(this.front);
        else             batch.add(this.back);
    }

    public Vector3 getPosition() { return this.front.getPosition(); }

    public float getWidth()      { return this.front.getWidth(); }

    public float getHeight()     { return this.front.getHeight(); }

    public boolean intersects(Ray ray) {

        float pitchRad = -50f * MathUtils.degreesToRadians;
        Vector3 normal = new Vector3(0, -MathUtils.sin(pitchRad), MathUtils.cos(pitchRad));

        Plane cardPlane = new Plane(normal, getPosition());
        Vector3 intersection = new Vector3();
        if (!Intersector.intersectRayPlane(ray, cardPlane, intersection)) return false;

        Vector3 pos = getPosition();
        float halfW = getWidth()  / 2f;
        float halfH = getHeight() / 2f;

        return intersection.x >= pos.x - halfW && intersection.x <= pos.x + halfW
            && intersection.z >= pos.z - halfH && intersection.z <= pos.z + halfH;
    }


}
