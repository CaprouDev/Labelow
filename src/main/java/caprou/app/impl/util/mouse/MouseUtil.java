package caprou.app.impl.util.mouse;

public final class MouseUtil {

    public static boolean isHovering(final int mouseX, final int mouseY, final int x, final int y, final int width, final int height) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public static boolean isHovering(final int mouseX, final int mouseY, final float x, final float y, final float width, final float height) {
        return isHovering(mouseX, mouseY, (int) x, (int) y, (int) width, (int) height);
    }
}
