package caprou.app.impl.render.font.renderer;


public final class Fonts {
    public static final Font LORA = FontManager.declare("loraRegular", "Lora-Regular.ttf");
    public static final Font LORA_BOLD = FontManager.declare("loraBold", "Lora-Bold.ttf");
    public static final Font LORA_MEDIUM = FontManager.declare("loraMedium", "Lora-Medium.ttf");
    public static final Font INTER_LIGHT = FontManager.declare("interLight", "Inter-Light.ttf");
    public static final Font INTER = FontManager.declare("interMedium", "Inter-Medium.ttf");
    public static final Font INTER_BOLD = FontManager.declare("interBold", "Inter-Bold.ttf");
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