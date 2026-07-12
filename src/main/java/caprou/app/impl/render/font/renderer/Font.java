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
    private TrueTypeFont trueTypeFont;
    private GpuTextRenderer renderer;

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

    public float measureWidth(String text, float size) {
        return requireRenderer().measureWidth(text, size);
    }

    public float lineHeight(float size) {
        return requireRenderer().lineHeight(size);
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
        return new IllegalStateException(
                "La font '" + source + "' n'est pas initialisée. "
                        + "Appelle FontManager.initAll() après GL.createCapabilities()."
        );
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

    private static void requireColor(Color color) {
        Objects.requireNonNull(color, "La couleur ne peut pas être null.");
    }
}