package caprou.app.impl.render.font.kerning;


import caprou.app.impl.render.font.table.TableRecord;
import caprou.app.impl.render.font.TrueTypeFont;

import java.nio.ByteBuffer;

public final class KerningReader {

    private KerningReader() {
    }

    public static long kernKey(int leftGlyph, int rightGlyph) {
        return ((long) leftGlyph << 32) | (rightGlyph & 0xffffffffL);
    }

    public static void parseKern(ByteBuffer buffer, TrueTypeFont trueTypeFont) {
        final TableRecord kern = trueTypeFont.tables().get("kern");

        if (kern == null) {
            return;
        }

        buffer.position((int) kern.offset());

        buffer.getShort(); // version
        final int nTables = Short.toUnsignedInt(buffer.getShort());

        for (int t = 0; t < nTables; t++) {
            buffer.getShort(); // subable version

            final int length = Short.toUnsignedInt(buffer.getShort());
            final int coverage = Short.toUnsignedInt(buffer.getShort());
            final int format = coverage >> 8;

            if (format != 0) {
                buffer.position(buffer.position() + length - 6);
                continue;
            }

            final int nPairs = Short.toUnsignedInt(buffer.getShort());
            buffer.getShort(); //search range
            buffer.getShort(); // entry selector
            buffer.getShort(); // range shift

            for (int i = 0; i < nPairs; i++) {
                final int left = Short.toUnsignedInt(buffer.getShort());
                final int right = Short.toUnsignedInt(buffer.getShort());
                final short value = buffer.getShort();

                trueTypeFont.kerning().put(kernKey(left, right), value);
            }
        }
    }
}