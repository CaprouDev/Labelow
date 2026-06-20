package caprou.app.impl.render.util;

import org.lwjgl.opengl.GL11;

import java.awt.*;

import static caprou.app.impl.util.render.ColorUtil.color;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;

public final class RenderUtil {

    public static void render(final int mode, final Runnable render) {
        GL11.glBegin(mode);
        render.run();
        GL11.glEnd();
    }

    public static void translate(final Runnable run, final float x, final float y) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        run.run();

        GL11.glPopMatrix();
    }

    public static void rotate(final Runnable run, final float x, final float y, final float angle) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glRotatef(angle, 0, 0,1);
        GL11.glTranslatef(-x, -y, 0);
        run.run();

        GL11.glPopMatrix();
    }

    public static void scale(Runnable run, float x, float y, float xScale, float yScale) {
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0);
        GL11.glScalef(xScale, yScale, 1);
        GL11.glTranslatef(-(x), -(y), 0);
        run.run();

        GL11.glPopMatrix();
    }

    public static void drawRect(final float x, final float y, final float width, final float height, final Color color) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        color(color);
        render(GL_QUADS, () -> {
            glVertex2d(x, y);
            glVertex2d(x + width, y);
            glVertex2d(x + width, y + height);
            glVertex2d(x, y + height);
        });

        glDisable(GL_BLEND);
    }
}
