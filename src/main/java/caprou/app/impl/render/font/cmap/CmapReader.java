package caprou.app.impl.render.font.cmap;

import caprou.app.impl.render.font.table.TableRecord;
import caprou.app.impl.render.font.TrueTypeFont;

import java.nio.ByteBuffer;

public final class CmapReader {

    private CmapReader() {
    }

    public static void parseCmapFormat12(ByteBuffer buffer, int format12Offset, TrueTypeFont trueTypeFont) {
        buffer.position(format12Offset);

        buffer.getShort(); // format
        buffer.getShort(); // reserved
        buffer.getInt();   // length
        buffer.getInt();   // language

        long nGroups = Integer.toUnsignedLong(buffer.getInt());

        for (long i = 0; i < nGroups; i++) {
            long startCharCode = Integer.toUnsignedLong(buffer.getInt());
            long endCharCode = Integer.toUnsignedLong(buffer.getInt());
            long startGlyphID = Integer.toUnsignedLong(buffer.getInt());

            for (long cp = startCharCode; cp <= endCharCode; cp++) {
                long glyphIndex = startGlyphID + (cp - startCharCode);

                if (cp <= Integer.MAX_VALUE && glyphIndex <= Integer.MAX_VALUE) {
                    trueTypeFont.codepointToGlyph().put((int) cp, (int) glyphIndex);
                }
            }
        }
    }

    public static void parseCmapFormat4(ByteBuffer buffer, int format4Offset, TrueTypeFont trueTypeFont) {
        buffer.position(format4Offset);

        buffer.getShort(); // format
        buffer.getShort(); // length
        buffer.getShort(); // language

        int segCountX2 = Short.toUnsignedInt(buffer.getShort());
        int segCount = segCountX2 / 2;

        buffer.getShort(); // search range
        buffer.getShort(); // entry selector
        buffer.getShort(); // range shift

        int[] endCode = new int[segCount];
        int[] startCode = new int[segCount];
        int[] idDelta = new int[segCount];
        int[] idRangeOffset = new int[segCount];

        for (int i = 0; i < segCount; i++) {
            endCode[i] = Short.toUnsignedInt(buffer.getShort());
        }

        buffer.getShort(); // reservedPad

        for (int i = 0; i < segCount; i++) {
            startCode[i] = Short.toUnsignedInt(buffer.getShort());
        }

        for (int i = 0; i < segCount; i++) {
            idDelta[i] = buffer.getShort();
        }

        int idRangeOffsetStart = buffer.position();

        for (int i = 0; i < segCount; i++) {
            idRangeOffset[i] = Short.toUnsignedInt(buffer.getShort());
        }

        for (int i = 0; i < segCount; i++) {
            int start = startCode[i];
            int end = endCode[i];

            if (start == 0xFFFF && end == 0xFFFF) {
                continue;
            }

            for (int codepoint = start; codepoint <= end; codepoint++) {
                int glyphIndex;

                if (idRangeOffset[i] == 0) {
                    glyphIndex = (codepoint + idDelta[i]) & 0xFFFF;
                } else {
                    int glyphIndexAddress = idRangeOffsetStart + i * 2 + idRangeOffset[i] + (codepoint - start) * 2;
                    int oldPos = buffer.position();

                    buffer.position(glyphIndexAddress);
                    glyphIndex = Short.toUnsignedInt(buffer.getShort());
                    buffer.position(oldPos);

                    if (glyphIndex != 0) {
                        glyphIndex = (glyphIndex + idDelta[i]) & 0xFFFF;
                    }
                }

                trueTypeFont.codepointToGlyph().put(codepoint, glyphIndex);
            }
        }
    }

    public static void parseCmap(ByteBuffer buffer, TrueTypeFont trueTypeFont) {
        TableRecord cmap = trueTypeFont.tables().get("cmap");

        if (cmap == null) {
            return;
        }

        buffer.position((int) cmap.offset());

        buffer.getShort(); // version
        int numSubtables = Short.toUnsignedInt(buffer.getShort());

        int format12Offset = -1;
        int format4Offset = -1;

        for (int i = 0; i < numSubtables; i++) {
            buffer.getShort(); // platform ID
            buffer.getShort(); // encoding ID
            long subtableOffset = Integer.toUnsignedLong(buffer.getInt());

            int oldPos = buffer.position();
            int absoluteOffset = (int) (cmap.offset() + subtableOffset);

            buffer.position(absoluteOffset);
            int format = Short.toUnsignedInt(buffer.getShort());

            if (format == 12) {
                format12Offset = absoluteOffset;
            } else if (format == 4) {
                format4Offset = absoluteOffset;
            }

            buffer.position(oldPos);
        }

        if (format12Offset != -1) {
            parseCmapFormat12(buffer, format12Offset, trueTypeFont);
        }

        if (format4Offset != -1) {
            parseCmapFormat4(buffer, format4Offset, trueTypeFont);
        }
    }
}