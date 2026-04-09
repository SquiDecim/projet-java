package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class CardDecal {
    private Decal front;
    private Decal back;
    private boolean faceVisible = true;
    private boolean hovered = false;
    private float currentY;
    private float baseY;

    public CardDecal(TextureRegion frontRegion, TextureRegion backRegion, float width, float height) {
        front = Decal.newDecal(frontRegion, true);
        back  = Decal.newDecal(backRegion, true);
        front.setDimensions(width, height);
        back.setDimensions(width, height);
    }

    public void setPosition(float x, float y, float z) {
        this.baseY    = y;
        this.currentY = y;
        front.setPosition(x, y, z);
        back.setPosition(x, y, z); // plus de 0.01f
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void update(float delta) {
        float targetY = hovered ? baseY + 0.6f : baseY;
        currentY += (targetY - currentY) * 8f * delta;

        Vector3 pos = front.getPosition();
        front.setPosition(pos.x, currentY, pos.z);
        back.setPosition(pos.x, currentY, pos.z);
    }

    public void setRotation(float yaw, float pitch, float roll) {
        front.setRotation(yaw, pitch, roll);
        back.setRotation(yaw + 180, pitch, roll);
    }

    public void flip() {
        faceVisible = !faceVisible;
    }

    public void addToBatch(DecalBatch batch) {
        if (faceVisible) batch.add(front);
        else             batch.add(back);
    }

    public Vector3 getPosition() { return front.getPosition(); }

    public float getWidth()      { return front.getWidth(); }

    public float getHeight()     { return front.getHeight(); }

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
