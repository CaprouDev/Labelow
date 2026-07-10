package caprou.app.impl.render.font.glyph.composite;

import caprou.app.impl.render.font.TrueTypeFont;
import caprou.app.impl.render.font.glyph.Glyph;
import caprou.app.impl.render.font.glyph.compose.GlyphContour;
import caprou.app.impl.render.font.glyph.compose.GlyphPoint;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static caprou.app.impl.render.font.glyph.geometry.PointResolver.getPointByIndex;
import static caprou.app.impl.render.font.glyph.geometry.SegmentResolver.buildSegments;
import static caprou.app.impl.render.font.glyph.geometry.SegmentResolver.flattenContours;
import static caprou.app.impl.render.util.BufferUtil.readF2Dot14;

public class CompositeResolver {

    private static final int ARG_1_AND_2_ARE_WORDS = 0x0001;
    private static final int ARGS_ARE_XY_VALUES = 0x0002;
    private static final int WE_HAVE_A_SCALE = 0x0008;
    private static final int MORE_COMPONENTS = 0x0020;
    private static final int WE_HAVE_AN_X_AND_Y_SCALE = 0x0040;
    private static final int WE_HAVE_A_TWO_BY_TWO = 0x0080;
    private static final int WE_HAVE_INSTRUCTIONS = 0x0100;

    private CompositeResolver() {
    }

    public static void resolveCompositeGlyphs(TrueTypeFont trueTypeFont) {
        for (Glyph glyph : trueTypeFont.glyphs().values()) {
            if (glyph == null || !glyph.isComposite()) {
                continue;
            }

            resolveCompositeGlyph(glyph, trueTypeFont, new HashSet<>());
        }
    }

    private static void resolveCompositeGlyph(Glyph glyph, TrueTypeFont trueTypeFont, Set<Integer> resolving) {
        if (glyph.getContours() != null && !glyph.getContours().isEmpty()) {
            return;
        }

        if (resolving.contains(glyph.getIndex())) {
            return;
        }

        resolving.add(glyph.getIndex());

        final List<GlyphContour> finalContours = new ArrayList<>();

        for (CompositeComponent component : glyph.getComponents()) {
            final Glyph base = trueTypeFont.glyphs().get(component.glyphIndex());

            //
            if (base == null)
                continue;

            if (base.isComposite())
                resolveCompositeGlyph(base, trueTypeFont, resolving);

            if (base.getContours() == null)
                continue;


            for (GlyphContour baseContour : base.getContours()) {
                finalContours.add(transformContour(baseContour, base, component, finalContours));
            }
        }

        glyph.setContours(finalContours);
        glyph.setSegments(buildSegments(finalContours));
        glyph.setFlatContours(flattenContours(finalContours, 16));

        resolving.remove(glyph.getIndex());
    }

    private static GlyphContour transformContour(GlyphContour baseContour, Glyph base, CompositeComponent component, List<GlyphContour> finalContours) {
        final GlyphContour transformedContour = new GlyphContour();

        for (GlyphPoint p : baseContour.getPoints()) {
            transformedContour.add(transformPoint(p, base, component, finalContours));
        }

        return transformedContour;
    }

    private static GlyphPoint transformPoint(GlyphPoint p, Glyph base, CompositeComponent component, List<GlyphContour> finalContours) {
        int x = p.x();
        int y = p.y();

        float dx = component.dx(), dy = component.dy();

        if (!component.argsAreXYValues()) {
            final GlyphPoint compositePoint = getPointByIndex(finalContours, component.arg1());
            final GlyphPoint componentPoint = getPointByIndex(base.getContours(), component.arg2());

            if (compositePoint != null && componentPoint != null) {
                final float transformedComponentX = component.a() * componentPoint.x() + component.b() * componentPoint.y();
                final float transformedComponentY = component.c() * componentPoint.x() + component.d() * componentPoint.y();

                dx = compositePoint.x() - transformedComponentX;
                dy = compositePoint.y() - transformedComponentY;
            }
        }


        final float tx = component.a() * x + component.b() * y + dx;
        final float ty = component.c() * x + component.d() * y + dy;

        return new GlyphPoint(Math.round(tx), Math.round(ty), p.onCurve());
    }

    public static List<CompositeComponent> parseCompositeGlyph(ByteBuffer buffer) {
        final List<CompositeComponent> components = new ArrayList<>();

        int lastFlags = 0;
        boolean moreComponents = true;
        while (moreComponents) {
            final int flags = Short.toUnsignedInt(buffer.getShort());
            final int glyphIndex = Short.toUnsignedInt(buffer.getShort());

            int arg1;
            int arg2;
            if ((flags & ARG_1_AND_2_ARE_WORDS) != 0) {
                arg1 = buffer.getShort();
                arg2 = buffer.getShort();
            } else if ((flags & ARGS_ARE_XY_VALUES) != 0) {
                arg1 = buffer.get();
                arg2 = buffer.get();
            } else {
                arg1 = Byte.toUnsignedInt(buffer.get());
                arg2 = Byte.toUnsignedInt(buffer.get());
            }

            int dx = 0;
            int dy = 0;
            if ((flags & ARGS_ARE_XY_VALUES) != 0) {
                dx = arg1;
                dy = arg2;
            }

            float a = 1.0f, b = 0.0f, c = 0.0f, d = 1.0f;

            if ((flags & WE_HAVE_A_SCALE) != 0) {
                float scale = readF2Dot14(buffer);
                a = scale;
                d = scale;

            } else if ((flags & WE_HAVE_AN_X_AND_Y_SCALE) != 0) {
                a = readF2Dot14(buffer);
                d = readF2Dot14(buffer);
            } else if ((flags & WE_HAVE_A_TWO_BY_TWO) != 0) {
                a = readF2Dot14(buffer);
                b = readF2Dot14(buffer);

                c = readF2Dot14(buffer);
                d = readF2Dot14(buffer);
            }

            components.add(new CompositeComponent(glyphIndex, (flags & ARGS_ARE_XY_VALUES) != 0, arg1, arg2, a, b, c, d, dx, dy));

            moreComponents = (flags & MORE_COMPONENTS) != 0;
            lastFlags = flags;
        }

        if ((lastFlags & WE_HAVE_INSTRUCTIONS) != 0) {
            final int instructionLength = Short.toUnsignedInt(buffer.getShort());
            buffer.position(buffer.position() + instructionLength);
        }

        return components;
    }
}