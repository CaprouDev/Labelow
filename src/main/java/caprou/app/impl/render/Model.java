package caprou.app.impl.render;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;

public class Model {

    private int drawCount;
    private int vaoID;
    private int vboID;

    public Model(float[] vertices, float[] uvs) {
        drawCount = vertices.length / 2;

        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        int vboPos = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboPos);
        glBufferData(GL_ARRAY_BUFFER, toBuffer(vertices), GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        int vboUV = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboUV);
        glBufferData(GL_ARRAY_BUFFER, toBuffer(uvs), GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
    }

    private FloatBuffer toBuffer(float[] data) {
        FloatBuffer buf = BufferUtils.createFloatBuffer(data.length);
        buf.put(data).flip();
        return buf;
    }

    public void render() {
        glBindVertexArray(vaoID);
        glDrawArrays(GL_TRIANGLES, 0, drawCount);
        glBindVertexArray(0);
    }
}
