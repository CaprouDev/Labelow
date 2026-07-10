package caprou.app.impl.render.font.metrics;

import caprou.app.impl.render.font.glyph.Glyph;
import caprou.app.impl.render.font.table.TableRecord;
import caprou.app.impl.render.font.TrueTypeFont;

import java.nio.ByteBuffer;

public final class FontMetricsReader {

    private FontMetricsReader() {
    }

    public static void parseHeadAndHheaMetrics(ByteBuffer buffer, TrueTypeFont trueTypeFont) {
        final TableRecord head = trueTypeFont.requireTable("head");
        final TableRecord hhea = trueTypeFont.requireTable("hhea");

        buffer.position((int) head.offset() + 18);
        trueTypeFont.setUnitsPerEm(Short.toUnsignedInt(buffer.getShort()));

        buffer.position((int) hhea.offset() + 4);
        trueTypeFont.setAscent(buffer.getShort());
        trueTypeFont.setDescent(buffer.getShort());
        trueTypeFont.setLineGap(buffer.getShort());
    }

    public static void parseHmtx(ByteBuffer buffer, TrueTypeFont trueTypeFont, int numGlyphs) {
        final TableRecord hhea = trueTypeFont.requireTable("hhea");
        final TableRecord hmtx = trueTypeFont.requireTable("hmtx");

        buffer.position((int) hhea.offset() + 34);
        int numberOfHMetrics = Short.toUnsignedInt(buffer.getShort());

        buffer.position((int) hmtx.offset());

        int lastAdvanceWidth = 0;

        for (int i = 0; i < numGlyphs; i++) {
            int advanceWidth;
            short leftSideBearing;

            if (i < numberOfHMetrics) {
                advanceWidth = Short.toUnsignedInt(buffer.getShort());
                leftSideBearing = buffer.getShort();
                lastAdvanceWidth = advanceWidth;
            } else {
                advanceWidth = lastAdvanceWidth;
                leftSideBearing = buffer.getShort();
            }

            Glyph glyph = trueTypeFont.glyphs().get(i);

            if (glyph != null) {
                glyph.setAdvanceWidth(advanceWidth);
                glyph.setLeftSideBearing(leftSideBearing);
            }
        }
    }
}