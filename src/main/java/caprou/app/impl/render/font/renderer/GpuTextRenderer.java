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

    public void drawString(String text, float x, float y, float fontSize, Color color) {
        final float quantizedSize = quantizeSize(fontSize);
        final float scale = quantizedSize / font.getUnitsPerEm();
        final float baseline = y + font.getAscent() * scale;
        drawStringBaselineInternal(text, x, baseline, quantizedSize, scale, color);
    }

    public void drawStringBaseline(String text, float x, float baselineY, float fontSize, Color color) {
        final float quantizedSize = quantizeSize(fontSize);
        final float scale = quantizedSize / font.getUnitsPerEm();
        drawStringBaselineInternal(text, x, baselineY, quantizedSize, scale, color);
    }

    private void drawStringBaselineInternal(String text, float startX, float startBaseline, float quantizedSize, float scale, Color color) {
        if (text == null || text.isEmpty() || color.getAlpha() == 0)
            return;

        float penX = startX;
        float baseline = startBaseline;
        int previousGlyphIndex = -1;

        BlendState blendState = captureBlendState();
        glEnable(GL_BLEND);
        glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
        glBlendFuncSeparate(GL_ONE, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        textShader.bind();
        textShader.setUniform("projection", OrthographicProjection.projection);
        textShader.setUniform("atlas", 0);
        textShader.setUniform("color", color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f, color.getAlpha() / 255.0f); //colorutil
        atlas.bindTexture(0);

        try {
            for (int charIndex = 0; charIndex < text.length();) {
                final int codePoint = text.codePointAt(charIndex);
                charIndex += Character.charCount(codePoint);

                if (codePoint == '\r') continue;
                if (codePoint == '\n') {
                    penX = startX;
                    baseline += lineHeight(quantizedSize);
                    previousGlyphIndex = -1;
                    continue;
                }


                if (codePoint == '\t') {
                    penX += glyphAdvance(glyphIndex(' '), scale) * 4.0f;
                    previousGlyphIndex = -1;
                    continue;
                }

                final int glyphIndex = glyphIndex(codePoint);
                Glyph glyph = font.glyphs().get(glyphIndex);
                if (glyph == null) continue;

                if (previousGlyphIndex >= 0) {
                    final short kern = font.kerning().getOrDefault(KerningReader.kernKey(previousGlyphIndex, glyphIndex), (short) 0);
                    penX += kern * scale;
                }

                if (glyph.getSegments() != null && !glyph.getSegments().isEmpty()) {
                    drawGlyph(glyph, penX, baseline, quantizedSize, scale);
                }

                penX += glyph.getAdvanceWidth() * scale;
                previousGlyphIndex = glyphIndex;
            }
        } finally {
            textShader.unbind();
            restoreBlendState(blendState);
        }
    }

    private void drawGlyph(Glyph glyph, float penX, float baseline, float size, float scale) {
        final float glyphLeft = penX + glyph.getXMin() * scale;
        final float glyphTop = baseline - glyph.getYMax() * scale;

        final QuantizedPosition xPosition = quantizePosition(glyphLeft);
        final QuantizedPosition yPosition = quantizePosition(glyphTop);

        final int size64 = Math.round(size * SIZE_STEPS);
        final GlyphCacheKey key = new GlyphCacheKey(glyph.getIndex(), size64, xPosition.step(), yPosition.step(), rgbSubpixel);

        CachedGlyph cachedGlyph = cache.get(key);
        if (cachedGlyph == null) {
            cachedGlyph = allocateGlyph(key, glyph, scale, xPosition.fraction(), yPosition.fraction());
            cache.put(key, cachedGlyph);
        }

        if (cachedGlyph.accumulatedSamples < MAX_ACCUMULATED_SAMPLES
                && cachedGlyph.lastRefinedFrame != frameId) {
            refineGlyph(glyph, cachedGlyph);
        }

        final AtlasRegion region = cachedGlyph.region;
        final float drawX = xPosition.basePixel() - GLYPH_PADDING;
        final float drawY = yPosition.basePixel() - GLYPH_PADDING;

        textShader.setUniform("transform", drawX, drawY, (float) region.width(), (float) region.height());

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

    private void refineGlyph(Glyph glyph, CachedGlyph cachedGlyph) {
        final int curveCount = curveBuffer.upload(glyph);
        if (curveCount == 0) return;

        final AtlasRegion region = cachedGlyph.region;
        final int remaining = MAX_ACCUMULATED_SAMPLES - cachedGlyph.accumulatedSamples;
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
        if (text == null || text.isEmpty()) return 0.0f;

        final float size = quantizeSize(fontSize);
        final float scale = size / font.getUnitsPerEm();

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
        final float scale = size / font.getUnitsPerEm();
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
        if (!(size > 0.0f) || Float.isInfinite(size) || Float.isNaN(size))
            throw new IllegalArgumentException("taille de texte invalide : " + size);

        return Math.max(1, Math.round(size * SIZE_STEPS)) / (float) SIZE_STEPS;
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

    private record QuantizedPosition(int basePixel, int step, float fraction) {
    }

    private record BlendState(
            boolean enabled,
            int srcRgb,
            int dstRgb,
            int srcAlpha,
            int dstAlpha,
            int equationRgb,
            int equationAlpha,
            float colorR,
            float colorG,
            float colorB,
            float colorA
    ) { }
}