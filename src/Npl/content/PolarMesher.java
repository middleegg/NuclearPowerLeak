package Npl.content;

import arc.graphics.Color;
import arc.math.geom.Vec3;
import arc.util.noise.Simplex;
import mindustry.graphics.g3d.HexMesher;

public class PolarMesher implements HexMesher {
    private final float threshold;
    private final Color color;
    private final Color emissiveColor;
    private final float height;
    private final int seed;

    public PolarMesher(float threshold, Color color, Color emissiveColor, float height, int seed) {
        this.threshold = threshold;
        this.color = color;
        this.emissiveColor = emissiveColor;
        this.height = height;
        this.seed = seed;
    }

    @Override
    public float getHeight(Vec3 v) {
        if (Math.abs(v.y) > threshold) {
            float noise = Simplex.noise3d(seed, 4, 0.5f, 1.0f, 5.0 + v.x, 5.0 + v.y, 5.0 + v.z);
            return height + noise * 0.1f;
        }
        return -1.0f;
    }

    @Override
    public void getColor(Vec3 v, Color c) {
        if (Math.abs(v.y) > threshold) {
            c.set(color);
        } else {
            c.set(0, 0, 0, 0);
        }
    }

    @Override
    public void getEmissiveColor(Vec3 v, Color c) {
        if (Math.abs(v.y) > threshold) {
            c.set(emissiveColor);
        } else {
            c.set(0, 0, 0, 0);
        }
    }

    @Override
    public boolean isEmissive() {
        return true;
    }

    @Override
    public boolean skip(Vec3 v) {
        return false;
    }
}
