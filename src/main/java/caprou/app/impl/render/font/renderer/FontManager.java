package caprou.app.impl.render.font.renderer;

import java.util.*;
public final class FontManager {

    private static final Set<Font> MANAGED_FONTS = new LinkedHashSet<>();
    private static final Map<String, Font> NAMED_FONTS = new LinkedHashMap<>();

    private static boolean initialized;

    private FontManager() {
    }

    static synchronized void track(Font font) {
        MANAGED_FONTS.add(font);
    }

    public static synchronized Font declare(String name, String source) {
        return register(name, new Font(source));
    }

    // opti la size pour chaque font avec demo file
    public static synchronized Font declare(String name, String source, int atlasSize) {
        return register(name, new Font(source, atlasSize));
    }

    public static synchronized Font register(String name, Font font) {
        if (font == null)
            throw new IllegalArgumentException("La font ne peut pas être null.");


        final String key = normalizeName(name);
        final Font previous = NAMED_FONTS.get(key);
        if (previous != null && previous != font) {
            throw new IllegalStateException("Une font est déjà enregistrée sous le nom '" + key + "'.");
        }

        MANAGED_FONTS.add(font);
        NAMED_FONTS.put(key, font);

        if (initialized && !font.isInitialized()) {
            font.init();
        }

        return font;
    }

    public static synchronized Font unregister(String name) {
        return NAMED_FONTS.remove(normalizeName(name));
    }

    public static void initAll() {
        final List<Font> snapshot;
        synchronized (FontManager.class) {
            snapshot = new ArrayList<>(MANAGED_FONTS);
        }

        for (Font font : snapshot) {
            font.init();
        }

        synchronized (FontManager.class) {
            initialized = true;
        }
    }

    public static void beginFrame() {
        final List<Font> snapshot;
        synchronized (FontManager.class) {
            snapshot = new ArrayList<>(MANAGED_FONTS);
        }

        for (Font font : snapshot) {
            if (font.isInitialized()) {
                font.beginFrame();
            }
        }
    }

    public static synchronized Font get(String name) {
        final String key = normalizeName(name);
        final Font font = NAMED_FONTS.get(key);

        if (font == null)
            throw new IllegalArgumentException("font inconnue : '" + key);

        return font;
    }

    public static synchronized boolean contains(String name) {
        return NAMED_FONTS.containsKey(normalizeName(name));
    }

    public static synchronized Map<String, Font> getNamedFonts() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(NAMED_FONTS));
    }

    public static synchronized int size() {
        return MANAGED_FONTS.size();
    }

    public static synchronized boolean isInitialized() {
        return initialized;
    }

    public static void clearCaches() {
        final List<Font> snapshot;
        synchronized (FontManager.class) {
            snapshot = new ArrayList<>(MANAGED_FONTS);
        }

        for (Font font : snapshot) {
            if (font.isInitialized()) {
                font.clearCache();
            }
        }
    }

    public static void deleteAll() {
        final List<Font> snapshot;
        synchronized (FontManager.class) {
            snapshot = new ArrayList<>(MANAGED_FONTS);
            initialized = false;
        }

        for (int index = snapshot.size() - 1; index >= 0; index--) {
            snapshot.get(index).delete();
        }
    }

    private static String normalizeName(String name) { //move dans string util
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Le nom de la font ne peut pas être vide.");

        return name.trim().toLowerCase(Locale.ROOT);
    }
}
