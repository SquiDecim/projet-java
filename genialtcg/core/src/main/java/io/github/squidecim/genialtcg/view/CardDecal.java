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
    private float yaw = 0f;
    private float pitch = 0f;
    private float roll = 0f;

    private float currentYaw = 0f;
    private float currentPitch = 0f;
    private float currentRoll = 0f;

    private Vector3 targetPos = null;
    private float[] startRot = new float[3];
    private float[] targetRot = new float[3];
    private float animTimer = 0f;
    private float animDuration = 0f;
    private Vector3 startPos = new Vector3();


    public CardDecal(CardData data, TextureRegion frontRegion, TextureRegion backRegion, float width, float height) {
        this.data = data;
        this.front = Decal.newDecal(frontRegion, true);
        TextureRegion flippedBack = new TextureRegion(backRegion);
        flippedBack.flip(true, false);
        this.back  = Decal.newDecal(flippedBack, true);
        this.front.setDimensions(width, height);
        this.back.setDimensions(width, height);
    }

    public void setPosition(float x, float y, float z) {
        this.baseY    = y;
        this.currentY = y;
        this.front.setPosition(x, y, z);
        this.back.setPosition(x, y, z);
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void update(float delta) {

        if (targetPos != null) {

            animTimer += delta;
            float t = Math.min(animTimer / animDuration, 1f);
            t = t * t * (3f - 2f * t);

            float nx = startPos.x + (targetPos.x - startPos.x) * t;
            float ny = startPos.y + (targetPos.y - startPos.y) * t;
            float nz = startPos.z + (targetPos.z - startPos.z) * t;
            front.setPosition(nx, ny, nz);
            back.setPosition(nx, ny, nz);

            float cy = startRot[0] + (targetRot[0] - startRot[0]) * t;
            float cp = startRot[1] + (targetRot[1] - startRot[1]) * t;
            float cr = startRot[2] + (targetRot[2] - startRot[2]) * t;

            this.currentYaw   = cy;
            this.currentPitch = cp;
            this.currentRoll  = cr;

            front.setRotation(cy, cp, cr);
            back.setRotation(cy + 180, cp, cr);

            float normalizedPitch = cp % 360f;
            if (normalizedPitch > 180f) normalizedPitch -= 360f;
            if (normalizedPitch < -180f) normalizedPitch += 360f;

            faceVisible = normalizedPitch > -90f && normalizedPitch < 90f;


            if (t >= 1f) {
                front.setPosition(targetPos.x, targetPos.y, targetPos.z);
                back.setPosition(targetPos.x, targetPos.y, targetPos.z);
                setPosition(targetPos.x, targetPos.y, targetPos.z);
                setRotation(targetRot[0], targetRot[1], targetRot[2]);
                targetPos = null;
            }
            return;
        }


        float targetY = this.hovered ? this.baseY + 0.4f : this.baseY;
        this.currentY += (targetY - this.currentY) * 8f * delta;

        Vector3 pos = this.front.getPosition();
        front.setPosition(pos.x, this.currentY, pos.z);
        back.setPosition(pos.x, this.currentY, pos.z);
        float normalizedPitch = this.pitch % 360f;
        if (normalizedPitch > 180f) normalizedPitch -= 360f;
        if (normalizedPitch < -180f) normalizedPitch += 360f;
        faceVisible = normalizedPitch > -90f && normalizedPitch < 90f;
    }

    public void setRotation(float yaw, float pitch, float roll) {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;

        this.currentYaw = yaw;
        this.currentPitch = pitch;
        this.currentRoll = roll;

        this.front.setRotation(yaw, pitch, roll);
        this.back.setRotation(yaw + 180, pitch, roll);

        float normalizedPitch = pitch % 360f;
        if (normalizedPitch > 180f) normalizedPitch -= 360f;
        if (normalizedPitch < -180f) normalizedPitch += 360f;
        faceVisible = normalizedPitch > -90f && normalizedPitch < 90f;
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

    public void animateTo(Vector3 targetPosition, float targetYaw, float targetPitch, float targetRoll, float duration) {
        startPos.set(front.getPosition());
        startRot[0] = this.currentYaw;
        startRot[1] = this.currentPitch;
        startRot[2] = this.currentRoll;
        targetPos = targetPosition.cpy();
        targetRot[0] = targetYaw;
        targetRot[1] = targetPitch;
        targetRot[2] = targetRoll;
        animTimer = 0f;
        animDuration = duration;
    }

    public boolean isAnimating() {
        return targetPos != null;
    }

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
