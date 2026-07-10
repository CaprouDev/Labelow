package caprou.app.impl.render;


import caprou.app.impl.interfaces.Shaders;
import caprou.app.impl.render.image.Texture;
import caprou.app.impl.render.shader.ShaderProgram;
import caprou.app.impl.util.object.Pair;
import caprou.app.impl.util.render.ColorUtil;

import java.awt.*;
import java.util.List;

import static caprou.app.impl.render.display.OrthographicProjection.projection;

public class SimpleRenderer implements Shaders {
    private static SimpleRenderer instance;

    public static SimpleRenderer getInstance() {
        if (instance == null)
            instance = new SimpleRenderer();

        return instance;
    }

    private Model quadModel;

    private final float[] QUAD_VERTICES = {
            0,0,  0,1,  1,1,
            0,0,  1,1,  1,0,
    };

    private final float[] QUAD_UVS = {
            0,0,  0,1,  1,1,
            0,0,  1,1,  1,0,
    };


    public void init() {
        quadModel = new Model(QUAD_VERTICES, QUAD_UVS);
    }

    private void basePass(ShaderProgram shader, float x, float y, float width, float height) {
        shader.bind();
        shader.setUniform("projection", projection);
        shader.setUniform("transform", x, y, width, height);
    }

    private void midPass(ShaderProgram shader, List<Pair<String, ?>> uniforms) {
        for(Pair<String, ?> pair : uniforms) {
            shader.setUniform(pair.first, pair.second);
        }
    }

    private void endPass(ShaderProgram shader, Model model) {
        model.render();
        shader.unbind();
    }


    public void drawRect(float x, float y, float width, float height, Color color) {
        final float[] colors = ColorUtil.toGLColor(color);

        basePass(colorShader, x, y, width, height);
        colorShader.setUniform("color", colors[0],colors[1],colors[2],colors[3]);
        endPass(colorShader, quadModel);
    }

    public void drawImage(float x, float y, float width, float height, Texture texture, Color color) {
        final float[] colors = ColorUtil.toGLColor(color);

        basePass(textureShader, x, y, width, height);
        texture.bind();
        textureShader.setUniform("iChannel0", 0);
        textureShader.setUniform("color", colors[0], colors[1], colors[2], colors[3]);
        endPass(textureShader, quadModel);
    }

    public void drawRound(float x, float y, float width, float height, float radius, Color color) {
        final float[] colors = ColorUtil.toGLColor(color);

        basePass(roundShader, x, y, width, height);
        roundShader.setUniform("rectSize", width, height);
        roundShader.setUniform("radius", radius);
        roundShader.setUniform("color", colors[0],colors[1],colors[2],colors[3]);
        endPass(roundShader, quadModel);
    }

    public void drawCircle(float x, float y, float size, Color color) {
        final float[] colors = ColorUtil.toGLColor(color);

        basePass(circleShader, x, y, size, size);
        circleShader.setUniform("size", size);
        circleShader.setUniform("color", colors[0],colors[1],colors[2],colors[3]);
        endPass(circleShader, quadModel);
    }


}