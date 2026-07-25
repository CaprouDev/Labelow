package caprou.app.impl.render.font.renderer;

import caprou.app.impl.render.Model;
import caprou.app.impl.render.display.OrthographicProjection;
import caprou.app.impl.render.font.TrueTypeFont;
import caprou.app.impl.render.font.glyph.Glyph;
import caprou.app.impl.render.font.kerning.KerningReader;
import caprou.app.impl.render.shader.ShaderManager;
import caprou.app.impl.render.shader.ShaderProgram;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.system.MemoryStack;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public final class GpuTextRenderer {

    public static final int DEFAULT_ATLAS_SIZE = 2048;

    private static final int GLYPH_PADDING = 3;
    private static final int SUBPIXEL_STEPS = 8;
    private static final int SIZE_STEPS = 64;

    private static final int SAMPLES_PER_PASS = 4;
    private static final int MAX_ACCUMULATED_SAMPLES = 128;
    private static final int MAX_ANIMATED_SAMPLES = 16;

    public static final float DEFAULT_ANIMATED_RASTER_SIZE = 64.0f;

    private static final float[] QUAD_VERTICES = {
            0, 0,  0, 1,  1, 1,
            0, 0,  1, 1,  1, 0
    };

    private static final float[] QUAD_UVS = {
            0, 0,  0, 1,  1, 1,
            0, 0,  1, 1,  1, 0
    };

    private final TrueTypeFont font;
    private final GpuGlyphAtlas atlas;
    private final GlyphCurveBuffer curveBuffer;
    private final Model quad;

    private final ShaderProgram rasterShader;
    private final ShaderProgram textShader;

    private final Map<GlyphCacheKey, CachedGlyph> cache = new HashMap<>();

    @Getter
    @Setter
    private boolean rgbSubpixel = true;
    @Getter
    private float animatedRasterSize = DEFAULT_ANIMATED_RASTER_SIZE;
    private long frameId;

    public GpuTextRenderer(TrueTypeFont font) {
        this(font, DEFAULT_ATLAS_SIZE);
    }

    public GpuTextRenderer(TrueTypeFont font, int atlasSize) {
        if (font == null) throw new IllegalArgumentException("La font ne peut pas être null.");
        if (font.getUnitsPerEm() <= 0) throw new IllegalArgumentException("unitsPerEm invalide.");

        this.font = font;
        this.atlas = new GpuGlyphAtlas(atlasSize, atlasSize);
        this.curveBuffer = new GlyphCurveBuffer();
        this.quad = new Model(QUAD_VERTICES, QUAD_UVS);
        this.rasterShader = ShaderManager.get("glyph-raster");
        this.textShader = ShaderManager.get("text");
    }

    public void beginFrame() {
        frameId++;
    }

    public void setAnimatedRasterSize(float animatedRasterSize) {
        this.animatedRasterSize = validateSize(animatedRasterSize);
    }

    public void warmupAnimated(String text) {
        if (text == null || text.isEmpty()) return;

        final float rasterSize = animatedRasterSize;
        final float rasterScale = rasterSize / font.getUnitsPerEm();
        final int size64 = Math.round(rasterSize * SIZE_STEPS);

        atlas.setLinearFiltering(true);

        try {
            for (int index = 0; index < text.length();) {
                final int codePoint = text.codePointAt(index);
                index += Character.charCount(codePoint);

                if (codePoint == '\r' || codePoint == '\n' || codePoint == '\t') {
                    continue;
                }

                final Glyph glyph = font.glyphs().get(glyphIndex(codePoint));
                if (glyph == null || glyph.getSegments() == null || glyph.getSegments().isEmpty()) {
                    continue;
                }

                final GlyphCacheKey key = new GlyphCacheKey(
                        glyph.getIndex(), size64, 0, 0, false
                );

                CachedGlyph cachedGlyph = cache.get(key);
                if (cachedGlyph == null) {
                    cachedGlyph = allocateGlyph(key, glyph, rasterScale, 0.0f, 0.0f);
                    cache.put(key, cachedGlyph);
                }

                while (cachedGlyph.accumulatedSamples < MAX_ANIMATED_SAMPLES) {
                    refineGlyph(glyph, cachedGlyph, MAX_ANIMATED_SAMPLES);
                }
            }
        } finally {
            textShader.unbind();
        }
    }

    public void drawString(String text, float x, float y, float fontSize, Color color) {
        final float quantizedSize = quantizeSize(fontSize);
        final float scale = quantizedSize / font.getUnitsPerEm();
        final float baseline = y + font.getAscent() * scale;

        drawStringBaselineInternal(
                text, x, baseline,
                scale, quantizedSize, scale,
                false, color
        );
    }

    public void drawStringBaseline(String text, float x, float baselineY, float fontSize, Color color) {
        final float quantizedSize = quantizeSize(fontSize);
        final float scale = quantizedSize / font.getUnitsPerEm();

        drawStringBaselineInternal(
                text, x, baselineY,
                scale, quantizedSize, scale,
                false, color
        );
    }

    public void drawStringAnimated(String text, float x, float y, float fontSize, Color color) {
        final float displaySize = validateSize(fontSize);
        final float displayScale = displaySize / font.getUnitsPerEm();
        final float rasterSize = animatedRasterSize;
        final float rasterScale = rasterSize / font.getUnitsPerEm();
        final float baseline = y + font.getAscent() * displayScale;

        drawStringBaselineInternal(
                text, x, baseline,
                displayScale, rasterSize, rasterScale,
                true, color
        );
    }

    public void drawStringBaselineAnimated(String text, float x, float baselineY, float fontSize, Color color) {
        final float displaySize = validateSize(fontSize);
        final float displayScale = displaySize / font.getUnitsPerEm();
        final float rasterSize = animatedRasterSize;
        final float rasterScale = rasterSize / font.getUnitsPerEm();

        drawStringBaselineInternal(
                text, x, baselineY,
                displayScale, rasterSize, rasterScale,
                true, color
        );
    }

    private void drawStringBaselineInternal(
            String text,
            float startX,
            float startBaseline,
            float displayScale,
            float rasterSize,
            float rasterScale,
            boolean animated,
            Color color
    ) {
        if (text == null || text.isEmpty() || color.getAlpha() == 0)
            return;

        float penX = startX;
        float baseline = startBaseline;
        int previousGlyphIndex = -1;

        BlendState blendState = captureBlendState();
        glEnable(GL_BLEND);
        glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
        glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        atlas.setLinearFiltering(animated);

        textShader.bind();
        textShader.setUniform("projection", OrthographicProjection.projection);
        textShader.setUniform("atlas", 0);
        textShader.setUniform(
                "color",
                color.getRed() / 255.0f,
                color.getGreen() / 255.0f,
                color.getBlue() / 255.0f,
                color.getAlpha() / 255.0f
        );
        atlas.bindTexture(0);

        try {
            for (int charIndex = 0; charIndex < text.length();) {
                final int codePoint = text.codePointAt(charIndex);
                charIndex += Character.charCount(codePoint);

                if (codePoint == '\r') continue;
                if (codePoint == '\n') {
                    penX = startX;
                    baseline += lineHeightForScale(displayScale);
                    previousGlyphIndex = -1;
                    continue;
                }

                if (codePoint == '\t') {
                    penX += glyphAdvance(glyphIndex(' '), displayScale) * 4.0f;
                    previousGlyphIndex = -1;
                    continue;
                }

                final int glyphIndex = glyphIndex(codePoint);
                final Glyph glyph = font.glyphs().get(glyphIndex);
                if (glyph == null) continue;

                if (previousGlyphIndex >= 0) {
                    final short kern = font.kerning().getOrDefault(KerningReader.kernKey(previousGlyphIndex, glyphIndex), (short) 0);

                    penX += kern * displayScale;
                }

                if (glyph.getSegments() != null && !glyph.getSegments().isEmpty()) {
                    drawGlyph(glyph, penX, baseline, displayScale, rasterSize, rasterScale, animated);
                }

                penX += glyph.getAdvanceWidth() * displayScale;
                previousGlyphIndex = glyphIndex;
            }
        } finally {
            textShader.unbind();
            restoreBlendState(blendState);
        }
    }

    private void drawGlyph(final Glyph glyph, float penX, float baseline, float displayScale, float rasterSize, float rasterScale, boolean animated) {
        final float glyphLeft = penX + glyph.getXMin() * displayScale;
        final float glyphTop = baseline - glyph.getYMax() * displayScale;

        final GlyphCacheKey key;
        final float rasterOffsetX;
        final float rasterOffsetY;
        final QuantizedPosition xPosition;
        final QuantizedPosition yPosition;

        if (animated) {
            key = new GlyphCacheKey(glyph.getIndex(), Math.round(rasterSize * SIZE_STEPS), 0, 0, false);
            rasterOffsetX = 0.0f;
            rasterOffsetY = 0.0f;
            xPosition = null;
            yPosition =
                    null;
        } else {
            xPosition = quantizePosition(glyphLeft);
            yPosition = quantizePosition(glyphTop);
            rasterOffsetX = xPosition.fraction();
            rasterOffsetY = yPosition.fraction();
            key = new GlyphCacheKey(glyph.getIndex(), Math.round(rasterSize * SIZE_STEPS), xPosition.step(), yPosition.step(), rgbSubpixel);
        }

        CachedGlyph cachedGlyph = cache.get(key);
        if (cachedGlyph == null) {

            cachedGlyph = allocateGlyph(key, glyph, rasterScale, rasterOffsetX, rasterOffsetY);
            cache.put(key, cachedGlyph);
        }

        final int targetSamples = animated ? MAX_ANIMATED_SAMPLES : MAX_ACCUMULATED_SAMPLES;

        if (cachedGlyph.accumulatedSamples < targetSamples
                && cachedGlyph.lastRefinedFrame != frameId) {
            refineGlyph(glyph, cachedGlyph, targetSamples);
        }

        final AtlasRegion region = cachedGlyph.region;
        final float drawX;
        final float drawY;
        final float drawWidth;
        final float drawHeight;

        if (animated) {
            final float textureToDisplayScale = displayScale / cachedGlyph.fontScale;

            drawX = glyphLeft - (GLYPH_PADDING + cachedGlyph.offsetX) * textureToDisplayScale;
            drawY = glyphTop - (GLYPH_PADDING + cachedGlyph.offsetY) * textureToDisplayScale;
            drawWidth = region.width() * textureToDisplayScale;
            drawHeight = region.height() * textureToDisplayScale;
        } else {
            drawX = xPosition.basePixel() - GLYPH_PADDING;
            drawY = yPosition.basePixel() - GLYPH_PADDING;
            drawWidth = region.width();
            drawHeight = region.height();
        }

        textShader.setUniform("transform", drawX, drawY, drawWidth, drawHeight);

        final float u0 = region.x() / (float) atlas.getWidth();
        final float v0 = region.y() / (float) atlas.getHeight();
        final float u1 = (region.x() + region.width()) / (float) atlas.getWidth();
        final float v1 = (region.y() + region.height()) / (float) atlas.getHeight();

        textShader.setUniform("uvRect", u0, v1, u1, v0);
        quad.render();
    }

    private CachedGlyph allocateGlyph(GlyphCacheKey key, Glyph glyph, float scale, float offsetX, float offsetY) {
        final float glyphWidth = Math.max(0.0f, (glyph.getXMax() - glyph.getXMin()) * scale);
        final float glyphHeight = Math.max(0.0f, (glyph.getYMax() - glyph.getYMin()) * scale);

        final int bitmapWidth = Math.max(1, (int) Math.ceil(glyphWidth + offsetX) + GLYPH_PADDING * 2);
        final int bitmapHeight = Math.max(1, (int) Math.ceil(glyphHeight + offsetY) + GLYPH_PADDING * 2);

        AtlasRegion region = atlas.allocate(bitmapWidth, bitmapHeight);
        if (region == null) {
            atlas.clear();
            cache.clear();
            region = atlas.allocate(bitmapWidth, bitmapHeight);
        }

        if (region == null) {
            throw new IllegalStateException("allocation impossible apres le vidage de l'atlas");
        }

        return new CachedGlyph(key, region, scale, offsetX, offsetY);
    }

    private void refineGlyph(Glyph glyph, CachedGlyph cachedGlyph, int targetSamples) {
        final int curveCount = curveBuffer.upload(glyph);
        if (curveCount == 0) return;

        final AtlasRegion region = cachedGlyph.region;
        final int remaining = targetSamples - cachedGlyph.accumulatedSamples;
        final int samplesThisPass = Math.min(SAMPLES_PER_PASS, remaining);
        final int previousFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
        final boolean scissorWasEnabled = glIsEnabled(GL_SCISSOR_TEST);
        final BlendState blendState = captureBlendState();

        final int viewportX;
        final int viewportY;
        final int viewportWidth;
        final int viewportHeight;
        final int scissorX;
        final int scissorY;
        final int scissorWidth;
        final int scissorHeight;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            IntBuffer scissorBox = stack.mallocInt(4);
            glGetIntegerv(GL_VIEWPORT, viewport);
            glGetIntegerv(GL_SCISSOR_BOX, scissorBox);

            viewportX = viewport.get(0);
            viewportY = viewport.get(1);
            viewportWidth = viewport.get(2);
            viewportHeight = viewport.get(3);

            scissorX = scissorBox.get(0);
            scissorY = scissorBox.get(1);
            scissorWidth = scissorBox.get(2);
            scissorHeight = scissorBox.get(3);
        }

        try {
            atlas.bindFramebuffer();
            glViewport(region.x(), region.y(), region.width(), region.height());
            glEnable(GL_SCISSOR_TEST);
            glScissor(region.x(), region.y(), region.width(), region.height());

            if (cachedGlyph.accumulatedSamples == 0) {
                glDisable(GL_BLEND);
            } else {
                float newWeight = samplesThisPass
                        / (float) (cachedGlyph.accumulatedSamples + samplesThisPass);
                glEnable(GL_BLEND);
                glBlendEquation(GL_FUNC_ADD);
                glBlendColor(0.0f, 0.0f, 0.0f, newWeight);
                glBlendFunc(GL_CONSTANT_ALPHA, GL_ONE_MINUS_CONSTANT_ALPHA);
            }

            rasterShader.bind();
            curveBuffer.bindTexture(0);
            rasterShader.setUniform("curves", 0);
            rasterShader.setUniform("curveCount", curveCount);
            rasterShader.setUniform("atlasOrigin", (float) region.x(), (float) region.y());
            rasterShader.setUniform("cellSize", (float) region.width(), (float) region.height());
            rasterShader.setUniform("glyphBounds", (float) glyph.getXMin(), (float) glyph.getYMin(), (float) glyph.getXMax(), (float) glyph.getYMax());
            rasterShader.setUniform("fontScale", cachedGlyph.fontScale);
            rasterShader.setUniform("glyphOffsetPx", GLYPH_PADDING + cachedGlyph.offsetX, GLYPH_PADDING + cachedGlyph.offsetY);
            rasterShader.setUniform("rgbSubpixel", cachedGlyph.key.rgbSubpixel() ? 1 : 0);
            rasterShader.setUniform("sampleBase", cachedGlyph.accumulatedSamples);
            rasterShader.setUniform("samplesPerPass", samplesThisPass);
            quad.render();
            rasterShader.unbind();

            cachedGlyph.accumulatedSamples += samplesThisPass;
            cachedGlyph.lastRefinedFrame = frameId;
        } finally {
            glBindFramebuffer(GL_FRAMEBUFFER, previousFramebuffer);
            glViewport(viewportX, viewportY, viewportWidth, viewportHeight);
            glScissor(scissorX, scissorY, scissorWidth, scissorHeight);
            if (scissorWasEnabled) glEnable(GL_SCISSOR_TEST);
            else glDisable(GL_SCISSOR_TEST);
            restoreBlendState(blendState);
        }

        textShader.bind();
        textShader.setUniform("projection", OrthographicProjection.projection);
        textShader.setUniform("atlas", 0);
        atlas.bindTexture(0);
    }

    public float measureWidth(String text, float fontSize) {
        final float size = quantizeSize(fontSize);
        return measureWidthForScale(text, size / font.getUnitsPerEm());
    }

    public float measureWidthAnimated(String text, float fontSize) {
        final float size = validateSize(fontSize);
        return measureWidthForScale(text, size / font.getUnitsPerEm());
    }

    private float measureWidthForScale(String text, float scale) {
        if (text == null || text.isEmpty()) return 0.0f;

        float lineWidth = 0.0f;
        float maxWidth = 0.0f;
        int previousGlyph = -1;

        for (int index = 0; index < text.length();) {
            final int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);

            if (codePoint == '\r') continue;
            if (codePoint == '\n') {
                maxWidth = Math.max(maxWidth, lineWidth);
                lineWidth = 0.0f;
                previousGlyph = -1;
                continue;
            }

            if (codePoint == '\t') {
                lineWidth += glyphAdvance(glyphIndex(' '), scale) * 4.0f;
                previousGlyph = -1;
                continue;
            }

            final int glyphIndex = glyphIndex(codePoint);
            final Glyph glyph = font.glyphs().get(glyphIndex);
            if (glyph == null) continue;

            if (previousGlyph >= 0) {
                lineWidth += font.kerning().getOrDefault(
                        KerningReader.kernKey(previousGlyph, glyphIndex),
                        (short) 0
                ) * scale;
            }

            lineWidth += glyph.getAdvanceWidth() * scale;
            previousGlyph = glyphIndex;
        }

        return Math.max(maxWidth, lineWidth);
    }

    public float lineHeight(float fontSize) {
        final float size = quantizeSize(fontSize);
        return lineHeightForScale(size / font.getUnitsPerEm());
    }

    public float lineHeightAnimated(float fontSize) {
        final float size = validateSize(fontSize);
        return lineHeightForScale(size / font.getUnitsPerEm());
    }

    private float lineHeightForScale(float scale) {
        return (font.getAscent() - font.getDescent() + font.getLineGap()) * scale;
    }

    private int glyphIndex(int codePoint) {
        return font.codepointToGlyph().getOrDefault(codePoint, 0);
    }

    private float glyphAdvance(int glyphIndex, float scale) {
        final Glyph glyph = font.glyphs().get(glyphIndex);
        return glyph == null ? 0.0f : glyph.getAdvanceWidth() * scale;
    }

    private float quantizeSize(float size) {
        final float validSize = validateSize(size);
        return Math.max(1, Math.round(validSize * SIZE_STEPS)) / (float) SIZE_STEPS;
    }

    private float validateSize(float size) {
        if (!(size > 0.0f) || Float.isInfinite(size) || Float.isNaN(size))
            System.err.println("SKIP : Taille de texte invalide : " + size);

        return size;
    }

    private QuantizedPosition quantizePosition(float value) {
        int base = (int) Math.floor(value);
        float fraction = value - base;
        int step = Math.round(fraction * SUBPIXEL_STEPS);

        if (step == SUBPIXEL_STEPS) {
            base++;
            step = 0;
        }

        return new QuantizedPosition(base, step, step / (float) SUBPIXEL_STEPS);
    }

    private BlendState captureBlendState() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer blendColor = stack.mallocFloat(4);
            glGetFloatv(GL_BLEND_COLOR, blendColor);

            return new BlendState(
                    glIsEnabled(GL_BLEND),
                    glGetInteger(GL_BLEND_SRC_RGB),
                    glGetInteger(GL_BLEND_DST_RGB),
                    glGetInteger(GL_BLEND_SRC_ALPHA),
                    glGetInteger(GL_BLEND_DST_ALPHA),
                    glGetInteger(GL_BLEND_EQUATION_RGB),
                    glGetInteger(GL_BLEND_EQUATION_ALPHA),
                    blendColor.get(0),
                    blendColor.get(1),
                    blendColor.get(2),
                    blendColor.get(3)

            );
        }
    }

    private void restoreBlendState(BlendState state) {
        glBlendEquationSeparate(state.equationRgb(), state.equationAlpha());
        glBlendFuncSeparate(state.srcRgb(), state.dstRgb(), state.srcAlpha(), state.dstAlpha());
        glBlendColor(state.colorR(), state.colorG(), state.colorB(), state.colorA());

        if (state.enabled())
            glEnable(GL_BLEND);
        else
            glDisable(GL_BLEND);
    }

    public int getAtlasTextureId() {
        return atlas.getTextureId();
    }

    public void clearCache() {
        cache.clear();
        atlas.clear();
    }

    public void delete() {
        cache.clear();
        quad.delete();
        curveBuffer.delete();
        atlas.delete();
    }

    private record QuantizedPosition(int basePixel, int step, float fraction) { /* */ }

    private record BlendState(

            boolean enabled, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha, int equationRgb,
            int equationAlpha, float colorR, float colorG, float colorB, float colorA)

    {/* */}
}