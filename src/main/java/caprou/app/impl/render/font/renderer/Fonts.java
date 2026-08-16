package caprou.app.impl.render.font.renderer;


public final class Fonts {

    public static final Font INTER = FontManager.declare("inter", "default.ttf");
    public static final Font INSTRUMENT_SERIF = FontManager.declare("instrumentSerif", "instrumentSerif.ttf");
    public static final Font LOHIT = FontManager.declare("lohit", "Lohit-Devanagari.ttf");


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