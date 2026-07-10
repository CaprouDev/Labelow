package caprou.app.impl.interfaces;

import caprou.app.impl.render.shader.ShaderManager;
import caprou.app.impl.render.shader.ShaderProgram;

public interface Shaders {
    ShaderProgram textureShader = ShaderManager.shaders.get("texture");
    ShaderProgram colorShader = ShaderManager.shaders.get("color");
    ShaderProgram roundShader = ShaderManager.shaders.get("rounded");
    ShaderProgram circleShader = ShaderManager.shaders.get("circle");

}