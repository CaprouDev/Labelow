package caprou.app.impl.interfaces;

import caprou.app.impl.render.shader.ShaderManager;
import caprou.app.impl.render.shader.ShaderProgram;

public interface Shaders {
    ShaderProgram textureShader = ShaderManager.get("texture");
    ShaderProgram colorShader = ShaderManager.get("color");
    ShaderProgram roundShader = ShaderManager.get("rounded");
    ShaderProgram circleShader = ShaderManager.get("circle");
    ShaderProgram textShader = ShaderManager.get("text");
    ShaderProgram glyphRasterShader = ShaderManager.get("glyph-raster");
}
