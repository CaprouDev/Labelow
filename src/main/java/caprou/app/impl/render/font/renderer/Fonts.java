package caprou.app.impl.render.font.renderer;


public final class Fonts {

    public static final Font INTER = FontManager.declare("inter", "default.ttf");
    public static final Font MIAMA = FontManager.declare("miama", "Miama.ttf");


    private Fonts() {
    }

    public static void init() {
        FontManager.initAll();
    }

    public static void beginFrame() {
        FontManager.beginFrame();
    }

    public static void delete() {
        FontManager.deleteAll();
    }
}