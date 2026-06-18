package caprou.app.impl.util.object;

import java.util.Arrays;

public final class ObjectUtil {

    public static int[] convertToIntegerArray(Object[] obj) {
        return Arrays.stream(obj)
                .mapToInt(o -> (Integer) o)
                .toArray();
    }

    public static float[] convertToFloatArray(Object[] obj) {
        float[] floatArgs = new float[obj.length];
        for (int i = 0; i < obj.length; i++) {
            floatArgs[i] = (Float) obj[i];
        }
        return floatArgs;
    }

}
