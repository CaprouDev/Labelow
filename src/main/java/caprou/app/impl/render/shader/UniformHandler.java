package caprou.app.impl.render.shader;


import caprou.app.impl.util.object.ObjectUtil;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public final class UniformHandler {
    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();

    public UniformHandler(final int programId) {
        this.programId = programId;
    }

    private int getUniformLocation(String name) {
        return uniformCache.computeIfAbsent(name, key -> {

            final int location = glGetUniformLocation(programId, key);
            if (location == -1) System.err.println("Uniform '" + key + "' not found in shader program");
            return location;

        });
    }

    public void setUniformFloat(String name, Object... obj) {
        final float[] args = ObjectUtil.convertToFloatArray(obj);
        final int location = getUniformLocation(name);

        switch (args.length) {
            case 1 -> glUniform1f(location, args[0]);
            case 2 -> glUniform2f(location, args[0], args[1]);
            case 3 -> glUniform3f(location, args[0], args[1], args[2]);
            case 4 -> glUniform4f(location, args[0], args[1], args[2], args[3]);
            default -> throw new IllegalArgumentException("Invalid float array length: " + args.length);
        }
    }

    public void setUniformInteger(String name, Object... obj) {
        final int[] args = ObjectUtil.convertToIntegerArray(obj);
        final int location = getUniformLocation(name);

        switch (args.length) {
            case 1 -> glUniform1i(location, args[0]);
            case 2 -> glUniform2i(location, args[0], args[1]);
            case 3 -> glUniform3i(location, args[0], args[1], args[2]);
            case 4 -> glUniform4i(location, args[0], args[1], args[2], args[3]);
            default -> throw new IllegalArgumentException("Invalid int array length: " + args.length);
        }
    }

    public void setUniformFb(String name, FloatBuffer fb) {
        final int location = getUniformLocation(name);
        glUniform1fv(location, fb);
    }

    public int getUniform(String name) {
        return getUniformLocation(name);
    }
}
