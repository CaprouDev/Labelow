package caprou.app.impl.util.render;


import caprou.app.impl.util.arithmetics.MathUtil;

import java.awt.*;

import static org.lwjgl.opengl.GL11.glColor4f;

public class ColorUtil {
    public static float[] toGLColor(final int color) {
        return new float[] {
            (float) (color >> 16 & 255) / 255.0F,
            (float) (color >> 8 & 255) / 255.0F,
            (float) (color & 255) / 255.0F,
            (float) (color >> 24 & 255) / 255.0F
        };
    }

    public static float[] toGLColor(final Color color) {
        return new float[] {
                color.getRed() / 255.0F,
                color.getGreen() / 255.0F,
                color.getBlue() / 255.0F,
                color.getAlpha() / 255.0F
        };
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(MathUtil.interpolateInt(color1.getRed(), color2.getRed(), amount),
                MathUtil.interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                MathUtil.interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                MathUtil.interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }


    public static void color(final Color color) {
        final float[] array = ColorUtil.toGLColor(color);
        glColor4f(array[0], array[1], array[2], array[3]);
    }

    public static Color toColor(float[] colors){
        return new Color(colors[0], colors[1], colors[2], colors[3]);
    }

    public static Color getRainbow(float seconds, float saturation, float brightness, long index) {
        float hue = ((System.currentTimeMillis() + index) % (int) (seconds * 1000)) / (seconds * 1000);
        int color = Color.HSBtoRGB(hue, saturation, brightness);
        return new Color(color);
    }

    public static Color getColorFromIndex(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolateColorC(start, end, angle / 360f);
    }
}