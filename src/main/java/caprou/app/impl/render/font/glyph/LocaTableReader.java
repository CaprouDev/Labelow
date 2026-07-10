package caprou.app.impl.render.font.glyph;

import caprou.app.impl.render.font.TrueTypeFont;
import caprou.app.impl.render.font.table.TableRecord;

import java.nio.ByteBuffer;

public final class LocaTableReader {

    private LocaTableReader() {
    }

    public static long[] readGlyphOffsets(ByteBuffer buffer, TrueTypeFont trueTypeFont, int numGlyphs, int indexToLocFormat) {
        final TableRecord loca = trueTypeFont.requireTable("loca");
        final long[] offsets = new long[numGlyphs + 1];

        buffer.position((int) loca.offset());

        if (indexToLocFormat == 0) {
            for (int i = 0; i <= numGlyphs; i++) {
                offsets[i] = Short.toUnsignedInt(buffer.getShort()) * 2L;
            }
        } else {
            for (int i = 0; i <= numGlyphs; i++) {
                offsets[i] = Integer.toUnsignedLong(buffer.getInt());
            }
        }

        return offsets;
    }
}