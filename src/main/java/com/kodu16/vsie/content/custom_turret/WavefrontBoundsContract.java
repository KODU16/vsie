package com.kodu16.vsie.content.custom_turret;

import java.util.List;

/** Dependency-free bounds math shared by OBJ loading and regression tests. */
public final class WavefrontBoundsContract {
    private WavefrontBoundsContract() {
    }

    public static float[] center(List<float[]> vertices) {
        if (vertices.isEmpty()) {
            return null;
        }
        if (vertices.get(0).length < 3) {
            throw new IllegalArgumentException("Vertex must have three coordinates");
        }
        float[] minimum = vertices.get(0).clone();
        float[] maximum = vertices.get(0).clone();
        for (float[] vertex : vertices) {
            if (vertex.length < 3) {
                throw new IllegalArgumentException("Vertex must have three coordinates");
            }
            for (int axis = 0; axis < 3; axis++) {
                minimum[axis] = Math.min(minimum[axis], vertex[axis]);
                maximum[axis] = Math.max(maximum[axis], vertex[axis]);
            }
        }
        return new float[]{
                (minimum[0] + maximum[0]) * 0.5F,
                (minimum[1] + maximum[1]) * 0.5F,
                (minimum[2] + maximum[2]) * 0.5F
        };
    }
}
