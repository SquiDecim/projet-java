package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import io.github.squidecim.genialtcg.model.CardData;

public class CardDecal {

    private CardData data;
    private Model model;
    private ModelInstance instance;

    private float width;
    private float height;

    private float yaw = 0f;
    private float pitch = 0f;
    private float roll = 0f;

    private float currentYaw = 0f;
    private float currentPitch = 0f;
    private float currentRoll = 0f;

    private Vector3 position = new Vector3();
    private float baseY = 0f;
    private float currentY = 0f;
    private boolean hovered = false;

    private Vector3 targetPos = null;
    private float[] startRot = new float[3];
    private float[] targetRot = new float[3];
    private Vector3 startPos = new Vector3();
    private float animTimer = 0f;
    private float animDuration = 0f;


    private boolean sliding = false;
    private Vector3 slideTarget = new Vector3();
    private Vector3 slideStart = new Vector3();
    private float slideTimer = 0f;
    private float slideDuration = 0f;

    private Vector3 pendingTarget = null;
    private float[] pendingTargetRot = new float[3];
    private float pendingDuration = 0f;

    private int handIndex = 0;

    public CardDecal(CardData data, TextureRegion frontRegion, TextureRegion backRegion, float width, float height, PerspectiveCamera cam) {
        this.data = data;
        this.width = width;
        this.height = height;

        TextureRegion flippedBack = new TextureRegion(backRegion);

        ModelBuilder builder = new ModelBuilder();
        builder.begin();

        MeshPartBuilder mpb = builder.part("front", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
            new Material(
                TextureAttribute.createDiffuse(frontRegion),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                IntAttribute.createCullFace(GL20.GL_BACK)
            ));
        mpb.rect(
            -width / 2, -height / 2, 0,
            width / 2, -height / 2, 0,
            width / 2,  height / 2, 0,
            -width / 2,  height / 2, 0,
            0, 0, 1
        );

        mpb = builder.part("back", GL20.GL_TRIANGLES,
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates,
            new Material(
                TextureAttribute.createDiffuse(flippedBack),
                new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA),
                IntAttribute.createCullFace(GL20.GL_BACK)
            ));
        mpb.rect(
            width / 2, -height / 2, 0,
            -width / 2, -height / 2, 0,
            -width / 2,  height / 2, 0,
            width / 2,  height / 2, 0,
            0, 0, -1
        );

        model    = builder.end();
        instance = new ModelInstance(model);
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        baseY    = y;
        currentY = y;
        applyTransform(x, y, z, yaw, pitch, roll);
    }

    public void setDragPosition(float x, float y, float z) {
        position.set(x, y, z);
        currentY = y;
        applyTransform(x, y, z, yaw, pitch, roll);
    }

    public Vector3 getPosition() { return new Vector3(position.x, currentY, position.z); }

    public void setRotation(float yaw, float pitch, float roll) {
        this.yaw   = yaw;
        this.pitch = pitch;
        this.roll  = roll;
        this.currentYaw   = yaw;
        this.currentPitch = pitch;
        this.currentRoll  = roll;
        applyTransform(position.x, currentY, position.z, yaw, pitch, roll);
    }

    public void setHandIndex(int index) {
        this.handIndex = index;
    }

    private void applyTransform(float x, float y, float z, float cy, float cp, float cr) {
        instance.transform
            .setToRotation(Vector3.Y, cy)
            .rotate(Vector3.X, cp)
            .rotate(Vector3.Z, cr);
        instance.transform.setTranslation(x, y, z);
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void update(float delta) {

        if (sliding) {
            slideTimer += delta;
            float t = Math.min(slideTimer / slideDuration, 1f);
            t = t * t * (3f - 2f * t);

            float nx = slideStart.x + (slideTarget.x - slideStart.x) * t;
            float ny = slideStart.y + (slideTarget.y - slideStart.y) * t;
            float nz = slideStart.z + (slideTarget.z - slideStart.z) * t;

            position.set(nx, ny, nz);
            currentY = ny;
            applyTransform(nx, ny, nz, currentYaw, currentPitch, currentRoll);

            if (t >= 1f) {
                sliding = false;
                if (pendingTarget != null) {
                    startMainAnim(pendingTarget, pendingTargetRot, pendingDuration);
                    pendingTarget = null;
                }
            }
            return;
        }

        if (targetPos != null) {
            animTimer += delta;
            float t = Math.min(animTimer / animDuration, 1f);
            t = t * t * (3f - 2f * t);

            float nx = startPos.x + (targetPos.x - startPos.x) * t;
            float ny = startPos.y + (targetPos.y - startPos.y) * t;
            float nz = startPos.z + (targetPos.z - startPos.z) * t;

            float cy = startRot[0] + (targetRot[0] - startRot[0]) * t;
            float cp = startRot[1] + (targetRot[1] - startRot[1]) * t;
            float cr = startRot[2] + (targetRot[2] - startRot[2]) * t;

            currentYaw   = cy;
            currentPitch = cp;
            currentRoll  = cr;

            float yOffset = handIndex * 0.002f;
            applyTransform(nx, ny + yOffset, nz, cy, cp, cr);

            if (t >= 1f) {
                position.set(targetPos);
                baseY    = targetPos.y;
                currentY = targetPos.y;
                yaw   = targetRot[0];
                pitch = targetRot[1];
                roll  = targetRot[2];
                currentYaw   = yaw;
                currentPitch = pitch;
                currentRoll  = roll;
                targetPos = null;
                applyTransform(position.x, currentY + yOffset, position.z, yaw, pitch, roll);
            }
            return;
        }

        float yOffset = handIndex * 0.002f;
        float targetY = hovered ? baseY + 0.75f : baseY;
        currentY += (targetY - currentY) * 8f * delta;
        applyTransform(position.x, currentY + yOffset, position.z, yaw, pitch, roll);
    }

    public void animateFromDeck(Vector3 deckTop, Vector3 target, float targetYaw, float targetPitch, float targetRoll, float duration) {

        position.set(deckTop);
        currentY = deckTop.y;
        currentYaw = yaw;
        currentPitch = pitch;
        currentRoll = roll;

        slideStart.set(deckTop);
        slideTarget.set(deckTop.x, deckTop.y + 0.5f, deckTop.z + 0.4f);
        slideTimer = 0f;
        slideDuration = 0.18f;
        sliding = true;

        pendingTarget      = target.cpy();
        pendingTargetRot[0] = targetYaw;
        pendingTargetRot[1] = targetPitch;
        pendingTargetRot[2] = targetRoll;
        pendingDuration    = duration;
    }

    public void animateTo(Vector3 target, float targetYaw, float targetPitch, float targetRoll, float duration) {
        if (sliding) {
            pendingTarget      = target.cpy();
            pendingTargetRot[0] = targetYaw;
            pendingTargetRot[1] = targetPitch;
            pendingTargetRot[2] = targetRoll;
            pendingDuration    = duration;
        } else {
            startMainAnim(target, new float[]{targetYaw, targetPitch, targetRoll}, duration);
        }
    }

    private void startMainAnim(Vector3 target, float[] rot, float duration) {
        startPos.set(position.x, currentY, position.z);
        startRot[0] = currentYaw;
        startRot[1] = currentPitch;
        startRot[2] = currentRoll;
        targetPos    = target.cpy();
        targetRot[0] = rot[0];
        targetRot[1] = rot[1];
        targetRot[2] = rot[2];
        animTimer    = 0f;
        animDuration = duration;
    }

    public boolean isAnimating() {
        return sliding || targetPos != null;
    }

    public void render(ModelBatch batch, Environment environment) {
        batch.render(instance, environment);
    }

    public boolean intersects(Ray ray) {
        float pitchRad = pitch * MathUtils.degreesToRadians;
        float yawRad   = yaw   * MathUtils.degreesToRadians;

        Vector3 normal = new Vector3(
            MathUtils.sin(yawRad) * MathUtils.cos(pitchRad),
            -MathUtils.sin(pitchRad),
            MathUtils.cos(yawRad) * MathUtils.cos(pitchRad)
        );

        Plane cardPlane = new Plane(normal, getPosition());
        Vector3 intersection = new Vector3();
        if (!Intersector.intersectRayPlane(ray, cardPlane, intersection)) return false;

        Vector3 diff = new Vector3(intersection).sub(getPosition());
        float halfW  = width  / 2f;
        float halfH  = height / 2f;

        Vector3 right = new Vector3(MathUtils.cos(yawRad), 0, -MathUtils.sin(yawRad));
        Vector3 up    = normal.cpy().crs(right).nor();

        return Math.abs(diff.dot(right)) <= halfW && Math.abs(diff.dot(up)) <= halfH;
    }

    public void dispose() {
        model.dispose();
    }

    public CardData getData() {
        return this.data;
    }

    public float getWidth()      { return width; }
    public float getHeight()     { return height; }
}
