package io.github.squidecim.genialtcg;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

public class CardsStackDecal {

    private boolean hovered = false;
    private float currentY;
    private float baseY;
    private float thickness = 0.035f;

    public CardsStackDecal(TextureRegion cardTexture, float width, float height, float depth, int nbrCards) {
        //A voir si je fabrique le stack à la bonne position ou si je le construis à un endroit fixe puis que je le déplace (plutot option 1 je pense)
    }

    public void setPosition(float x, float y, float z) {
        //Placer la pile de carte à l'endroit voulu
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    public void update(float delta) {
        //Méthode permettant de diminuer l'épaisseur de la pile quand les cartes sont tirés ou posés
    }

    public void setRotation(float yaw, float pitch, float roll) {

    }



    public boolean intersects(Ray ray) {

        //Potentiellement reprendre la logique de la méthode de CardDecal mais rajouter la profondeur (depth) en paramètre de détection jsp encore
        return false;
    }


}

