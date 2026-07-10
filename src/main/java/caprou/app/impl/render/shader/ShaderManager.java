package caprou.app.impl.render.shader;

import java.util.HashMap;

public class ShaderManager {
    public static HashMap<String, ShaderProgram> shaders = new HashMap<>(){{
        put("texture", new ShaderProgram("texture.glsl"));
        put("color", new ShaderProgram("color.glsl"));
        put("rounded", new ShaderProgram("rounded.glsl"));
        put("circle", new ShaderProgram("circle.glsl"));
    }};

    public static void compileShaders() {
        for(ShaderProgram shader : shaders.values()){
            shader.compile();
        }
    }
}
