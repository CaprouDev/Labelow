package caprou.app.impl.render.shader;



import caprou.app.Main;
import caprou.app.impl.render.SimpleRenderer;
import caprou.app.impl.render.display.Display;
import caprou.app.impl.util.file.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private static final String VERTEX_SHADER = "vertex.vert";
    private final String fragmentShader;
    private UniformHandler uniformHandler;
    private boolean compiled;
    private int programId;
    public long startTime;

    public ShaderProgram(final String fragmentShader) {
        this.fragmentShader = fragmentShader;
    }

    public void compile() {
        this.programId = glCreateProgram();

        try {

            final int fragmentShaderID = loadShader(getShaderStream(fragmentShader), GL_FRAGMENT_SHADER);
            final int vertexShaderID = loadShader(getShaderStream(VERTEX_SHADER), GL_VERTEX_SHADER);

            glAttachShader(programId, fragmentShaderID);
            glAttachShader(programId, vertexShaderID);

            glLinkProgram(programId);
            validateProgram();
            compiled = true;

        } catch (IOException e) {
            throw new RuntimeException("Error while loading shader files.", e);
        }

        this.uniformHandler = new UniformHandler(programId);
        this.startTime = System.currentTimeMillis();
    }

    private InputStream getShaderStream(String shaderFile) throws IOException {
        return this.getClass().getResourceAsStream("/shader/" + shaderFile);
    }

    private int loadShader(InputStream inputStream, int shaderType) {
        final int shaderId = glCreateShader(shaderType);

        glShaderSource(shaderId, FileUtil.readInputStream(inputStream));
        glCompileShader(shaderId);

        final int status = glGetShaderi(shaderId, GL_COMPILE_STATUS);
        if (status == 0) throw new IllegalStateException(glGetShaderInfoLog(shaderId) + shaderType);

        return shaderId;
    }

    private void validateProgram() {
        final int status = glGetProgrami(programId, GL_LINK_STATUS);
        if(status == 0) throw new RuntimeException(glGetProgramInfoLog(programId));
    }

    public final void bind() {
        glUseProgram(programId);
    }

    public final void unbind() {
        glUseProgram(GL_NONE);
    }

    public final void setUniform(String name, Object... obj){
        if (!compiled) return;

        final Object value = obj[0];
        switch (value){
            case Integer _ -> uniformHandler.setUniformInteger(name, obj);
            case Float _ -> uniformHandler.setUniformFloat(name, obj);
            case FloatBuffer fb -> uniformHandler.setUniformFb(name, fb);
            default -> throw new IllegalStateException("Unexpected value: " + value);
        }
    }

    public final void drawQuads(float x, float y, float width, float height) {
        SimpleRenderer.render(GL_QUADS, () -> {

            glTexCoord2f(0, 0); glVertex2f(x, y);
            glTexCoord2f(0, 1); glVertex2f(x, y + height);
            glTexCoord2f(1, 1); glVertex2f(x + width, y + height);
            glTexCoord2f(1, 0); glVertex2f(x + width, y);

        });
    }

    public final void drawQuads() {
        final Display display = Main.getDisplay();
        drawQuads(0, 0, (float) display.getWidth(), (float) display.getHeight());
    }

    public int getUniform(final String name) {
        return uniformHandler.getUniform(name);
    }
}