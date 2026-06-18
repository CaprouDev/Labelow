package caprou.app.impl.render;

import caprou.app.impl.util.render.ColorUtil;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static caprou.app.impl.util.render.ColorUtil.color;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_BLEND;

public final class SimpleRenderer {

    private final List<float[]> vertices = new ArrayList<>();

    public void begin() {
        vertices.clear();
    }

    public void posTexColor(float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        vertices.add(new float[] {
                x, y, z,
                u, v,
                r / 255f, g / 255f, b / 255f, a / 255f
        });
    }

    public void draw() {
        GL11.glBegin(GL11.GL_QUADS);
        for (float[] v : vertices) {
            GL11.glColor4f(v[5], v[6], v[7], v[8]);
            GL11.glTexCoord2f(v[3], v[4]);
            GL11.glVertex3f(v[0], v[1], v[2]);
        }
        GL11.glEnd();
    }


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