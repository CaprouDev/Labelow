package caprou.app.impl.render;


import caprou.app.impl.interfaces.Shaders;
import caprou.app.impl.render.image.Texture;
import caprou.app.impl.util.render.ColorUtil;

import java.awt.*;

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


    public void drawRect(float x, float y, float width, float height, Color color) {
        final float[] colors = ColorUtil.toGLColor(color);

        colorShader.bind();
        colorShader.setUniform("projection", projection);
        colorShader.setUniform("transform", x, y, width, height);
        colorShader.setUniform("color", colors[0],colors[1],colors[2],colors[3]);
        quadModel.render();
        colorShader.unbind();
    }

    public void drawImage(float x, float y, float width, float height, Texture texture, Color color) {
        final float[] colors = ColorUtil.toGLColor(color);

        textureShader.bind();
        texture.bind();
        textureShader.setUniform("projection", projection);
        textureShader.setUniform("transform", x, y, width, height);
        textureShader.setUniform("iChannel0", 0);
        textureShader.setUniform("color", colors[0], colors[1], colors[2], colors[3]);
        quadModel.render();
        textureShader.unbind();
    }

}