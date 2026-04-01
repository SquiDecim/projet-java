package io.github.squidecim.genialtcg;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.GroupStrategy;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Array;

import java.util.Comparator;

public class InsertionOrderStrategy implements GroupStrategy {
    private final ShaderProgram shader;
    private final Comparator<Decal> comparator = (a, b) -> 0;

    public InsertionOrderStrategy() {
        // on crée un CameraGroupStrategy temporaire juste pour voler son shader
        CameraGroupStrategy tmp = new CameraGroupStrategy(new PerspectiveCamera());
        shader = tmp.getGroupShader(0);
    }

    @Override
    public ShaderProgram getGroupShader(int group) {
        return shader;
    }

    @Override
    public int decideGroup(Decal decal) { return 0; }

    @Override
    public void beforeGroup(int group, Array<Decal> contents) {}

    @Override
    public void afterGroup(int group) {}

    @Override
    public void beforeGroups() {}

    @Override
    public void afterGroups() {}

    public Comparator<Decal> getComparator() { return comparator; }
}
