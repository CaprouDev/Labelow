package caprou.app.impl.render.display;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public final class OrthographicProjection {

    public static FloatBuffer projection;

    public static void updateProjection(int width, int height) {
        projection = buildOrtho(width, height);
    }

    private static FloatBuffer buildOrtho(int w, int h) {
        float[] ortho = {
                2f/w,  0,      0, 0,
                0,    -2f/h,   0, 0,
                0,     0,     -1, 0,
                -1,     1,      0, 1
        };
        FloatBuffer buf = BufferUtils.createFloatBuffer(16);
        buf.put(ortho).flip();
        return buf;
    }

}
