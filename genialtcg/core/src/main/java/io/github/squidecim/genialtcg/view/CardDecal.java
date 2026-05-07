package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
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

    public TextureRegion frontRegion;
    public TextureRegion backRegion;

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
    private boolean dragging = false;

    private Vector3 targetPos = null;
    private float[] startRot = new float[3];
    private float[] targetRot = new float[3];
    private Vector3 startPos = new Vector3();
    private float animTimer = 0f;
    private float animDuration = 0f;

    private boolean shaking = false;
    private float shakeTimer = 0f;
    private static final float SHAKE_DURATION = 0.5f;
    private static final float SHAKE_INTENSITY = 0.1f;
    private static final float SHAKE_FREQUENCY = 25f;

    private boolean sliding = false;
    private Vector3 slideTarget = new Vector3();
    private Vector3 slideStart = new Vector3();
    private float slideTimer = 0f;
    private float slideDuration = 0f;

    private Vector3 pendingTarget = null;
    private float[] pendingTargetRot = new float[3];
    private float pendingDuration = 0f;

    private int handIndex = 0;

    public String emplacement;

    private FrameBuffer frameBuffer;
    private Texture dynamicTexture;
    private SpriteBatch fb_batch;
    private BitmapFont fb_font_pv;
    private BitmapFont fb_font_stats;
    private BitmapFont fb_font_special_cost;

    private boolean hasDynamicTexture = false;
    private static final int CARD_TEX_W = 512;
    private static final int CARD_TEX_H = 716;

    public CardDecal(CardData data, TextureRegion frontRegion, TextureRegion backRegion, float width, float height, PerspectiveCamera cam, String emplacement) {
        this.data = data;
        this.width = width;
        this.height = height;
        this.frontRegion = frontRegion;
        this.backRegion = backRegion;
        this.emplacement = emplacement;
        buildModel(frontRegion, backRegion, width, height);

    }

    public void buildModel(TextureRegion frontRegion, TextureRegion backRegion, float width, float height){

        if (model != null) {
            model.dispose();
        }

        this.width = width;
        this.height = height;

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
                TextureAttribute.createDiffuse(backRegion),
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

    public void generateDynamicTexture(int cardPixelW, int cardPixelH) {
        if (data.id.startsWith("ACT-") || data.id.startsWith("OUT-")) return;
        if (fb_batch == null) {
            fb_batch = new SpriteBatch();
            FreeTypeFontGenerator generator_bold = new FreeTypeFontGenerator(
                Gdx.files.internal("ui/dejavu-sans/DejaVuSans-Bold.ttf")
            );
            FreeTypeFontGenerator generator_regular = new FreeTypeFontGenerator(
                Gdx.files.internal("ui/dejavu-sans/DejaVuSans.ttf")
            );
            FreeTypeFontGenerator.FreeTypeFontParameter params =
                new FreeTypeFontGenerator.FreeTypeFontParameter();
            params.size = 40;
            fb_font_pv = generator_bold.generateFont(params);
            params.size = 20;
            fb_font_special_cost = generator_bold.generateFont(params);
            fb_font_stats = generator_regular.generateFont(params);
        }
        if (frameBuffer != null) frameBuffer.dispose();

        frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, cardPixelW, cardPixelH, false);
        frameBuffer.begin();

        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        fb_batch.getProjectionMatrix().setToOrtho2D(0, 0, cardPixelW, cardPixelH);
        fb_batch.begin();


        fb_batch.draw(frontRegion, 0, 0, cardPixelW, cardPixelH);

        if (data != null) {

            fb_font_pv.setColor(Color.BLACK);
            fb_font_pv.draw(fb_batch, Integer.toString(data.pv),
                cardPixelW - 110, cardPixelH - 25);


            fb_font_stats.setColor(Color.BLACK);
            fb_font_stats.draw(fb_batch, Integer.toString(data.stats[0]), 210, cardPixelH / 2f - 99);
            fb_font_stats.draw(fb_batch, Integer.toString(data.stats[2]), 423, cardPixelH / 2f - 99);
            fb_font_stats.draw(fb_batch, Integer.toString(data.stats[3]), 210, cardPixelH / 2f - 144);
            fb_font_stats.draw(fb_batch, Integer.toString(data.stats[4]), 423, cardPixelH / 2f - 144);
            fb_font_stats.draw(fb_batch, Integer.toString(data.stats[1]), 325, cardPixelH / 2f - 188);

            fb_font_special_cost.setColor(Color.BLACK);
            fb_font_special_cost.draw(fb_batch, Integer.toString(data.specialCout), 415, cardPixelH / 2f - 225);


            fb_font_stats.setColor(Color.BLACK);
            fb_font_stats.draw(fb_batch, Integer.toString(data.cost),75, 43);
            fb_font_stats.draw(fb_batch, Integer.toString(data.revocation),110, 25);
        }

        fb_batch.end();
        frameBuffer.end();

        dynamicTexture = frameBuffer.getColorBufferTexture();
        TextureRegion dynamicRegion = new TextureRegion(dynamicTexture);
        dynamicRegion.flip(false, true);

        buildModel(dynamicRegion, backRegion, width, height);

        hasDynamicTexture = true;
    }

    public TextureRegion getTopTextureRegion() {
        if (dynamicTexture != null) {
            TextureRegion r = new TextureRegion(dynamicTexture);
            r.flip(false, true);
            return r;
        }
        return frontRegion;
    }

    public void rebuildWithDynamic(float w, float h) {
        if (hasDynamicTexture) {
            this.width = w;
            this.height = h;
            generateDynamicTexture(CARD_TEX_W, CARD_TEX_H);
        } else {
            buildModel(frontRegion, backRegion, w, h);
        }
    }

    public void refreshStats() {

        Vector3 pos = getPosition();
        float yaw = currentYaw;
        float pitch = currentPitch;
        float roll = currentRoll;

        if (dynamicTexture != null) {
            generateDynamicTexture(512, 716);
        }

        setPosition(pos.x, pos.y, pos.z);
        setRotation(yaw, pitch, roll);
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        baseY    = y;
        currentY = y;
        applyTransform(x, y, z, yaw, pitch, roll);
    }

    public void setDragPosition(float x, float y, float z) {
        dragging = true;
        position.set(x, y, z);
        currentY = y;
        applyTransform(x, y, z, yaw, pitch, roll);
    }

    public void setDragging(boolean dragging) {
        this.dragging = dragging;
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

        if (shaking) {
            shakeTimer += delta;
            if (shakeTimer >= SHAKE_DURATION) {
                shaking = false;
                shakeTimer = 0f;
                applyTransform(position.x, currentY, position.z, yaw, pitch, roll);
            }
            float shake = MathUtils.sin(shakeTimer * SHAKE_FREQUENCY * MathUtils.PI2)
                * SHAKE_INTENSITY
                * (1f - shakeTimer / SHAKE_DURATION);
            applyTransform(
                position.x + shake,
                currentY,
                position.z,
                yaw + shake * 5f,
                pitch,
                roll
            );
            return;
        }

        if (dragging) {
            applyTransform(position.x, currentY, position.z, yaw, pitch, roll);
            return;
        }

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
        float targetY = hovered && emplacement.equals("hand")? baseY + 0.75f : baseY;
        currentY += (targetY - currentY) * 8f * delta;
        applyTransform(position.x, currentY + yOffset, position.z, yaw, pitch, roll);
    }

    public void animateFromDeck(Vector3 deckTop, Vector3 target, float targetYaw, float targetPitch, float targetRoll, float duration, boolean fromPlayer) {

        position.set(deckTop);
        currentY = deckTop.y;
        currentYaw = yaw;
        currentPitch = pitch;
        currentRoll = roll;

        slideStart.set(deckTop);
        if (fromPlayer) slideTarget.set(deckTop.x, deckTop.y + 0.5f, deckTop.z + 1f);
        else slideTarget.set(deckTop.x, deckTop.y + 0.5f, deckTop.z - 1f);
        slideTimer = 0f;
        slideDuration = 0.3f;
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
        return sliding || targetPos != null || shaking;
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
        if (frameBuffer != null) frameBuffer.dispose();
        if (fb_batch != null) fb_batch.dispose();
        if (fb_font_pv != null) fb_font_pv.dispose();
    }

    public CardData getData() {
        return this.data;
    }

    public float getWidth()      { return width; }

    public float getHeight()     { return height; }

    public int getHandIndex() {
        return handIndex;
    }

    public void shake() {
        shaking = true;
        shakeTimer = 0f;
    }
}
