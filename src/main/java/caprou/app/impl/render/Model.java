package caprou.app.impl.render;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

public class Model {

    private final int drawCount;
    private final int vaoId;
    private final int positionVboId;
    private final int uvVboId;

    public Model(float[] vertices, float[] uvs) {
        if (vertices.length != uvs.length || vertices.length % 2 != 0) {
            throw new IllegalArgumentException("Les positions et UV doivnet avoir le même nombre de vec2");
        }

        drawCount = vertices.length / 2;

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        positionVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, positionVboId);
        glBufferData(GL_ARRAY_BUFFER, toBuffer(vertices), GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(0);

        uvVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, uvVboId);
        glBufferData(GL_ARRAY_BUFFER, toBuffer(uvs), GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0L);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    private FloatBuffer toBuffer(float[] data) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
        buffer.put(data).flip();
        return buffer;
    }

    public void render() {
        glBindVertexArray(vaoId);
        glDrawArrays(GL_TRIANGLES, 0, drawCount);
        glBindVertexArray(0);
    }

    public void delete() {
        glBindVertexArray(vaoId);
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glBindVertexArray(0);

        glDeleteBuffers(positionVboId);
        glDeleteBuffers(uvVboId);
        glDeleteVertexArrays(vaoId);
    }
}
