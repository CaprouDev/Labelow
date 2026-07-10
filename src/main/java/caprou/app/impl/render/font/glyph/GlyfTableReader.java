package caprou.app.impl.render.font.glyph;

import caprou.app.impl.render.font.TrueTypeFont;
import caprou.app.impl.render.font.glyph.compose.GlyphContour;
import caprou.app.impl.render.font.table.TableRecord;

import java.nio.ByteBuffer;
import java.util.List;

import static caprou.app.impl.render.font.glyph.composite.CompositeResolver.parseCompositeGlyph;
import static caprou.app.impl.render.font.glyph.geometry.SegmentResolver.buildSegments;
import static caprou.app.impl.render.font.glyph.geometry.SegmentResolver.flattenContours;

public final class GlyfTableReader {

    private static final int FLATTEN_SUBDIVISIONS = 16;

    private GlyfTableReader() {
    }

    public static void parseGlyphs(ByteBuffer buffer, TrueTypeFont trueTypeFont, long[] glyphOffsets, int numGlyphs) {
        final TableRecord glyf = trueTypeFont.requireTable("glyf");

        for (int i = 0; i < numGlyphs; i++) {
            final long offset = glyphOffsets[i];
            final long length = glyphOffsets[i + 1] - offset;
            final Glyph glyph = length == 0 ? emptyGlyph(i, offset) : parseGlyph(buffer, glyf, i, offset, length);

            trueTypeFont.glyphs().put(i, glyph);
        }
    }

    private static Glyph emptyGlyph(int index, long offset) {
        return new Glyph(index, (short) 0, (short) 0, (short) 0, (short) 0, (short) 0, offset, 0);
    }

    private static Glyph parseGlyph(ByteBuffer buffer, TableRecord glyf, int index, long offset, long length) {
        buffer.position((int) (glyf.offset() + offset));

        final short numberOfContours = buffer.getShort();
        final short xMin = buffer.getShort();
        final short yMin = buffer.getShort();
        final short xMax = buffer.getShort();
        final short yMax = buffer.getShort();

        final Glyph glyph = new Glyph(index, numberOfContours, xMin, yMin, xMax, yMax, offset, length);
        if (numberOfContours >= 0) {
            final List<GlyphContour> contours = GlyphOutlineReader.readContours(buffer, numberOfContours);

            glyph.setContours(contours);
            glyph.setSegments(buildSegments(contours));
            glyph.setFlatContours(flattenContours(contours, FLATTEN_SUBDIVISIONS));
        } else {
            glyph.setComponents(parseCompositeGlyph(buffer));
        }

        return glyph;
    }
}