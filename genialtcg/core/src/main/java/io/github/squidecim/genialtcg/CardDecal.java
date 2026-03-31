package io.github.squidecim.genialtcg;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;

public class CardDecal {
    private Decal front;
    private Decal back;
    private boolean faceVisible = true;

    public CardDecal(TextureRegion frontRegion, TextureRegion backRegion, float width, float height) {
        front = Decal.newDecal(frontRegion, true);
        back = Decal.newDecal(backRegion, true);

        front.setDimensions(width, height);
        back.setDimensions(width, height);
    }

    public void setPosition(float x, float y, float z) {
        front.setPosition(x, y, z);
        back.setPosition(x, y + 0.01f, z);
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
        else batch.add(back);
    }
}
