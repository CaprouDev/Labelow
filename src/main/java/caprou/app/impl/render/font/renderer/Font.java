package caprou.app.impl.render.font.renderer;

import caprou.app.impl.render.font.TrueTypeFont;
import caprou.app.impl.render.font.TrueTypeFontReader;
import lombok.Getter;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Font {

    @Getter
    private final String source;
    @Getter
    private final int atlasSize;

    @Getter
    private boolean rgbSubpixel = true;
    @Getter
    private float animatedRasterSize = GpuTextRenderer.DEFAULT_ANIMATED_RASTER_SIZE;
    private TrueTypeFont trueTypeFont;
    private GpuTextRenderer renderer;
    private final AnimatedSize animatedSizeView = new AnimatedSize();

    public Font(String source) {
        this(source, GpuTextRenderer.DEFAULT_ATLAS_SIZE);
    }

    public Font(String source, int atlasSize) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("La source de la font ne peut pas être vide.");
        }
        if (atlasSize <= 0) {
            throw new IllegalArgumentException("Taille d'atlas invalide : " + atlasSize);
        }

        this.source = source;
        this.atlasSize = atlasSize;

        FontManager.track(this);
    }

    public synchronized void init() {
        if (renderer != null)
            return;


        try (InputStream stream = openFontStream()) {
            trueTypeFont = new TrueTypeFontReader().parseFont(stream);
            renderer = new GpuTextRenderer(trueTypeFont, atlasSize);
            renderer.setRgbSubpixel(rgbSubpixel);
            renderer.setAnimatedRasterSize(animatedRasterSize);
        } catch (Exception exception) {
            trueTypeFont = null;
            renderer = null;

            throw new IllegalStateException(
                    "Font '" + source + " src/main/resources/font/" + exception
            );
        }
    }

    public void beginFrame() {
        requireRenderer().beginFrame();
    }

    public void drawString(String text, float x, float y, float size, Color color) {
        requireColor(color);
        requireRenderer().drawString(text, x, y, size, color);
    }

    public void drawString(String text, float x, float y, float size, int argb) {
        drawString(text, x, y, size, new Color(argb, true));
    }

    public void drawStringBaseline(String text, float x, float baselineY, float size, Color color) {
        requireColor(color);
        requireRenderer().drawStringBaseline(text, x, baselineY, size, color);
    }

    public void drawStringBaseline(String text, float x, float baselineY, float size, int argb) {
        drawStringBaseline(text, x, baselineY, size, new Color(argb, true));
    }

    public void drawStringAnimated(String text, float x, float y, float size, Color color) {
        requireColor(color);
        requireRenderer().drawStringAnimated(text, x, y, size, color);
    }

    public void drawStringAnimated(String text, float x, float y, float size, int argb) {
        drawStringAnimated(text, x, y, size, new Color(argb, true));
    }

    public void drawStringBaselineAnimated(String text, float x, float baselineY, float size, Color color) {
        requireColor(color);
        requireRenderer().drawStringBaselineAnimated(text, x, baselineY, size, color);
    }

    public void drawStringBaselineAnimated(String text, float x, float baselineY, float size, int argb) {
        drawStringBaselineAnimated(text, x, baselineY, size, new Color(argb, true));
    }

    public AnimatedSize animateSize(float size) {
        requireValidSize(size);
        animatedSizeView.size = size;
        return animatedSizeView;
    }

    public float measureWidth(String text, float size) {
        return requireRenderer().measureWidth(text, size);
    }

    public float lineHeight(float size) {
        return requireRenderer().lineHeight(size);
    }

    public float measureWidthAnimated(String text, float size) {
        return requireRenderer().measureWidthAnimated(text, size);
    }

    public float lineHeightAnimated(float size) {
        return requireRenderer().lineHeightAnimated(size);
    }

    public void clearCache() {
        requireRenderer().clearCache();
    }

    public void setRgbSubpixel(boolean enabled) {
        rgbSubpixel = enabled;
        if (renderer != null) {
            renderer.setRgbSubpixel(enabled);
        }
    }

    public void setAnimatedRasterSize(float size) {
        requireValidSize(size);
        animatedRasterSize = size;

        if (renderer != null) {
            renderer.setAnimatedRasterSize(size);
        }
    }

    public void warmupAnimated(String text) {
        requireRenderer().warmupAnimated(text);
    }

    public boolean isInitialized() {
        return renderer != null;
    }

    public int getAtlasTextureId() {
        return requireRenderer().getAtlasTextureId();
    }

    public TrueTypeFont getTrueTypeFont() {
        if (trueTypeFont == null) {
            throw notInitializedException();
        }
        return trueTypeFont;
    }

    public GpuTextRenderer getRenderer() {
        return requireRenderer();
    }

    public synchronized void delete() {
        if (renderer != null) {
            renderer.delete();
            renderer = null;
        }
        trueTypeFont = null;
    }

    private GpuTextRenderer requireRenderer() {
        GpuTextRenderer current = renderer;
        if (current == null) {
            throw notInitializedException();
        }
        return current;
    }

    private IllegalStateException notInitializedException() {
        return new IllegalStateException("La font '" + source + "' n'est pas initialisée. " + "Appelle FontManager.initAll() après GL.createCapabilities().");
    }

    private InputStream openFontStream() throws IOException {
        final List<String> resourceCandidates = new ArrayList<>();
        String normalized = source.replace('\\', '/');

        if (normalized.startsWith("classpath:")) {
            normalized = normalized.substring("classpath:".length());
        }

        if (normalized.startsWith("/")) {
            resourceCandidates.add(normalized);
        } else {
            resourceCandidates.add("/font/" + normalized);
            resourceCandidates.add("/" + normalized);
        }

        for (String candidate : resourceCandidates) {
            final InputStream stream = Font.class.getResourceAsStream(candidate); // fileutil
            if (stream != null) {
                return stream;
            }
        }

        final String fileSource = source.startsWith("file:") ? source.substring("file:".length()) : source;
        final Path file = Paths.get(fileSource);

        if (Files.isRegularFile(file))
            return Files.newInputStream(file);


        throw new IOException("Fichier introuvable : " + source +" Ressources testées : " + resourceCandidates + " Chemin disque testé : " + file.toAbsolutePath());
    }



    private static void requireValidSize(float size) {
        if (!(size > 0.0f) || Float.isInfinite(size) || Float.isNaN(size))
            System.err.println("SKIP : Taille de texte invalide : " + size);

    }

    private static void requireColor(Color color) {
        Objects.requireNonNull(color, "La couleur ne peut pas être null.");
    }


    public final class AnimatedSize {
        @Getter
        private float size;

        private AnimatedSize() {
        }

        public void drawString(String text, float x, float y, Color color) {
            Font.this.drawStringAnimated(text, x, y, size, color);
        }

        public void drawString(String text, float x, float y, int argb) {
            Font.this.drawStringAnimated(text, x, y, size, argb);
        }

        public void drawStringBaseline(String text, float x, float baselineY, Color color) {
            Font.this.drawStringBaselineAnimated(text, x, baselineY, size, color);
        }

        public void drawStringBaseline(String text, float x, float baselineY, int argb) {
            Font.this.drawStringBaselineAnimated(text, x, baselineY, size, argb);
        }

        public float measureWidth(String text) {
            return Font.this.measureWidthAnimated(text, size);
        }

        public float lineHeight() {
            return Font.this.lineHeightAnimated(size);
        }
    }
}