package caprou.app.impl.render.shader;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ShaderManager {

    public static final Map<String, ShaderProgram> shaders = new LinkedHashMap<>();

    static {
        shaders.put("texture", new ShaderProgram("texture.glsl"));
        shaders.put("color", new ShaderProgram("color.glsl"));
        shaders.put("rounded", new ShaderProgram("rounded.glsl"));
        shaders.put("circle", new ShaderProgram("circle.glsl"));

        shaders.put("text", new ShaderProgram("vertex.vert", "font/text.glsl"));
        shaders.put("glyph-raster", new ShaderProgram("font/glyph_atlas.vert", "font/glyph_raster.glsl"));
    }

    private ShaderManager() {
    }

    public static ShaderProgram get(String name) {
        final ShaderProgram shader = shaders.get(name);
        if (shader == null) throw new IllegalArgumentException("Shader inconnu : " + name);

        return shader;
    }

    public static void compileShaders() {
        for (ShaderProgram shader : shaders.values()) {
            shader.compile();
        }
    }

    public static void deleteShaders() {
        for (ShaderProgram shader : shaders.values()) {
            shader.delete();
        }
    }
}
