package caprou.app.impl.render.font.renderer;

final class CachedGlyph {

    final GlyphCacheKey key;
    final AtlasRegion region;
    final float fontScale;
    final float offsetX;
    final float offsetY;

    int accumulatedSamples;
    long lastRefinedFrame = Long.MIN_VALUE;

    CachedGlyph(GlyphCacheKey key, AtlasRegion region, float fontScale, float offsetX, float offsetY) {

        this.key = key;
        this.region = region;
        this.fontScale = fontScale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
}