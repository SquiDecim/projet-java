package io.github.squidecim.genialtcg.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class TerrainParticleSystem implements Disposable {


    private static class Particle {
        float x, y;
        float vx, vy;
        float wavePhase;
        float waveAmp;
        float waveFreq;
        float alpha;
        float size;
        float life, maxLife;
        boolean active;
    }


    private final String terrain;
    private final SpriteBatch batch;
    private final Texture particleTex;
    private final TextureRegion particleRegion;

    private final Array<Particle> particles = new Array<>();

    private Color particleColor;
    private int   maxParticles;
    private float spawnRate;
    private float spawnAccum;
    private boolean active;

    private static final float FADE_FRAC = 0.15f;

    public TerrainParticleSystem(String terrain, SpriteBatch sharedBatch) {
        this.terrain = terrain;
        this.batch = sharedBatch;
        this.particleTex = createCircleTexture(16);
        this.particleRegion = new TextureRegion(particleTex);

        configure();
    }

    private void configure() {
        switch (terrain) {
            case "Désertique":
                active = true;
                maxParticles = 500;
                spawnRate    = 5f;
                particleColor = new Color(0.85f, 0.72f, 0.42f, 1f);
                break;
            case "Tropical":
                active = true;
                maxParticles = 500;
                spawnRate    = 5f;
                particleColor = new Color(0.30f, 0.72f, 0.22f, 1f);
                break;
            case "Glacial":
                active = true;
                maxParticles = 500;
                spawnRate    = 5f;
                particleColor = new Color(0.55f, 0.80f, 1.00f, 1f);
                break;
            case "Océanique":
                active = true;
                maxParticles = 500;
                spawnRate    = 30f;
                particleColor = new Color(0.65f, 0.82f, 1.00f, 1f);
                break;
            default:
                active = false;
                maxParticles = 0;
                spawnRate    = 0f;
                particleColor = Color.WHITE;
                break;
        }
    }

    private void spawnParticle() {
        if (particles.size >= maxParticles) return;

        Particle p = new Particle();
        p.active  = true;
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        switch (terrain) {
            case "Désertique": {
                p.x = -20f;
                p.y = MathUtils.random(sh * 0.05f, sh * 0.95f);
                p.vx = MathUtils.random(50f, 100f);
                p.vy = 0f;
                p.waveAmp  = MathUtils.random(8f, 22f);
                p.waveFreq = MathUtils.random(0.4f, 1.0f);
                p.wavePhase = MathUtils.random(0f, MathUtils.PI2);
                p.size     = MathUtils.random(8f, 16f);
                p.maxLife  = (sw + 40f) / p.vx;
                p.life     = 0f;
                p.alpha    = 0f;
                break;
            }
            case "Glacial": {
                p.x = -20f;
                p.y = MathUtils.random(sh * 0.05f, sh * 0.95f);
                p.vx = MathUtils.random(40f, 80f);
                p.vy = 0f;
                p.waveAmp  = MathUtils.random(6f, 18f);
                p.waveFreq = MathUtils.random(0.5f, 1.1f);
                p.wavePhase = MathUtils.random(0f, MathUtils.PI2);
                p.size     = MathUtils.random(8f, 16f);
                p.maxLife  = (sw + 40f) / p.vx;
                p.life     = 0f;
                p.alpha    = 0f;
                break;
            }
            case "Tropical": {
                p.x = MathUtils.random(-20f, sw + 20f);
                p.y = sh + 20f;
                p.vx = MathUtils.random(-15f, 15f);
                p.vy = -MathUtils.random(50f, 100f);
                p.waveAmp  = MathUtils.random(10f, 28f);
                p.waveFreq = MathUtils.random(0.3f, 0.7f);
                p.wavePhase = MathUtils.random(0f, MathUtils.PI2);
                p.size     = MathUtils.random(8f, 16f);
                p.maxLife  = (sh + 40f) / Math.abs(p.vy);
                p.life     = 0f;
                p.alpha    = 0f;
                break;
            }
            case "Océanique": {
                p.x = MathUtils.random(-sw * 0.1f, sw * 1.1f);
                p.y = sh + 10f;
                p.vx = -MathUtils.random(55f, 85f);
                p.vy = -MathUtils.random(350f, 480f);
                p.waveAmp  = 0f;
                p.waveFreq = 0f;
                p.wavePhase = 0f;
                p.size     = MathUtils.random(8f, 14f);
                p.maxLife  = (sh + 20f) / Math.abs(p.vy);
                p.life     = 0f;
                p.alpha    = 0f;
                break;
            }
        }
        particles.add(p);
    }

    public void update(float delta) {
        if (!active) return;

        spawnAccum += delta * spawnRate;
        while (spawnAccum >= 1f) {
            spawnAccum -= 1f;
            spawnParticle();
        }

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        for (int i = particles.size - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life += delta;

            float t = p.life / p.maxLife;
            if (t < FADE_FRAC) {
                p.alpha = t / FADE_FRAC;
            } else if (t > 1f - FADE_FRAC) {
                p.alpha = (1f - t) / FADE_FRAC;
            } else {
                p.alpha = 1f;
            }

            switch (terrain) {
                case "Désertique": p.alpha *= 1f; break;
                case "Glacial":    p.alpha *= 1f; break;
                case "Tropical":   p.alpha *= 1f; break;
                case "Océanique":  p.alpha *= 1f; break;
            }

            p.x += p.vx * delta;

            if (terrain.equals("Désertique") || terrain.equals("Glacial")) {

                p.wavePhase += p.waveFreq * delta * MathUtils.PI2;
                p.y += MathUtils.sin(p.wavePhase) * p.waveAmp * delta;
            } else if (terrain.equals("Tropical")) {
                p.y += p.vy * delta;
                p.wavePhase += p.waveFreq * delta * MathUtils.PI2;
                p.x += MathUtils.sin(p.wavePhase) * p.waveAmp * delta;
            } else if (terrain.equals("Océanique")) {
                p.y += p.vy * delta;
            }

            boolean offscreen;
            switch (terrain) {
                case "Désertique":
                case "Glacial":
                    offscreen = p.x > sw + 30f || p.y < -30f || p.y > sh + 30f;
                    break;
                case "Tropical":
                    offscreen = p.y < -30f;
                    break;
                case "Océanique":
                    offscreen = p.y < -20f || p.x > sw + 30f;
                    break;
                default:
                    offscreen = true;
                    break;
            }
            if (offscreen || p.life >= p.maxLife) {
                particles.removeIndex(i);
            }
        }
    }

    public void render() {
        if (!active || particles.size == 0) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.begin();

        float r = particleColor.r;
        float g = particleColor.g;
        float b = particleColor.b;

        for (int i = 0; i < particles.size; i++) {
            Particle p = particles.get(i);
            batch.setColor(r, g, b, p.alpha);

            if (terrain.equals("Océanique")) {
                float rainW = p.size;
                float rainH = p.size * 9f;
                batch.draw(particleRegion,
                    p.x - rainW * 0.5f,
                    p.y - rainH * 0.5f,
                    rainW * 0.5f, rainH * 0.5f,
                    rainW, rainH,
                    1f, 1f,
                    -10f
                );
            } else {
                float half = p.size * 0.5f;
                batch.draw(particleRegion,
                    p.x - half, p.y - half,
                    p.size, p.size
                );
            }
        }

        batch.setColor(1f, 1f, 1f, 1f);
        batch.end();
    }

    private static Texture createCircleTexture(int size) {
        Pixmap px = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        px.setBlending(Pixmap.Blending.None);
        float cx = size / 2f;
        float cy = size / 2f;
        float r  = size / 2f - 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                float a = Math.max(0f, 1f - (dist / r));
                a = a * a; // soft falloff
                px.setColor(1f, 1f, 1f, a);
                px.drawPixel(x, y);
            }
        }
        Texture t = new Texture(px);
        px.dispose();
        return t;
    }


    @Override
    public void dispose() {
        particleTex.dispose();
    }
}
