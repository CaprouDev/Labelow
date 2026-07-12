package caprou.app.impl.render.font.renderer;

import caprou.app.impl.render.font.glyph.Glyph;
import caprou.app.impl.render.font.glyph.compose.GlyphPoint;
import caprou.app.impl.render.font.glyph.geometry.BezierSegment;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL31.*;

public final class GlyphCurveBuffer {

    private final int bufferId;
    private final int textureId;
    private final Map<Integer, float[]> cpuCache = new HashMap<>();

    public GlyphCurveBuffer() {
        bufferId = glGenBuffers();
        textureId = glGenTextures();
    }

    public int upload(Glyph glyph) {
        float[] packed = cpuCache.computeIfAbsent(glyph.getIndex(), ignored -> pack(glyph));
        if (packed.length == 0) return 0;

        FloatBuffer data = BufferUtils.createFloatBuffer(packed.length);
        data.put(packed).flip();

        glBindBuffer(GL_TEXTURE_BUFFER, bufferId);
        glBufferData(GL_TEXTURE_BUFFER, data, GL_STREAM_DRAW);

        glBindTexture(GL_TEXTURE_BUFFER, textureId);
        glTexBuffer(GL_TEXTURE_BUFFER, GL_RGBA32F, bufferId);

        glBindBuffer(GL_TEXTURE_BUFFER, 0);
        glBindTexture(GL_TEXTURE_BUFFER, 0);

        return packed.length / 8;
    }

    private float[] pack(Glyph glyph) {
        List<BezierSegment> segments = glyph.getSegments();
        if (segments == null || segments.isEmpty()) return new float[0];

        float[] result = new float[segments.size() * 8];
        int cursor = 0;

        for (BezierSegment segment : segments) {
            GlyphPoint p0 = segment.start();
            GlyphPoint p2 = segment.end();
            GlyphPoint p1 = segment.control();

            float controlX = p1 == null ? (p0.x() + p2.x()) * 0.5f : p1.x();
            float controlY = p1 == null ? (p0.y() + p2.y()) * 0.5f : p1.y();

            result[cursor++] = p0.x();
            result[cursor++] = p0.y();
            result[cursor++] = controlX;
            result[cursor++] = controlY;

            result[cursor++] = p2.x();
            result[cursor++] = p2.y();
            result[cursor++] = 0.0f;
            result[cursor++] = 0.0f;
        }

        return result;
    }

    public void bindTexture(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_BUFFER, textureId);
    }

    public void delete() {
        glDeleteTextures(textureId);
        glDeleteBuffers(bufferId);
        cpuCache.clear();
    }
}
