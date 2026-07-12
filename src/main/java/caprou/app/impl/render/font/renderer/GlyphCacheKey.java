package caprou.app.impl.render.font.renderer;

public record GlyphCacheKey(
        int glyphIndex,
        int size64,
        int subpixelX,
        int subpixelY,
        boolean rgbSubpixel
) { }
