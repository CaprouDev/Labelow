package caprou.app.impl.render.font.glyph;

import caprou.app.impl.render.font.glyph.compose.GlyphContour;
import caprou.app.impl.render.font.glyph.compose.GlyphPoint;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class GlyphOutlineReader {

    private static final int FLAG_ON_CURVE = 0x01;
    private static final int FLAG_REPEAT = 0x08;

    private static final int FLAG_X_SHORT = 0x02;
    private static final int FLAG_X_SAME_OR_POSITIVE = 0x10;

    private static final int FLAG_Y_SHORT = 0x04;
    private static final int FLAG_Y_SAME_OR_POSITIVE = 0x20;

    private GlyphOutlineReader() {
    }

    public static List<GlyphContour> readContours(ByteBuffer buffer, short numberOfContours) {
        final int[] endPtsOfContours = readEndPoints(buffer, numberOfContours);
        final int pointCount = numberOfContours > 0 ? endPtsOfContours[numberOfContours - 1] + 1 : 0;

        skipInstructions(buffer);

        final byte[] flags = readFlags(buffer, pointCount);
        final int[] xs = readCoordinates(buffer, flags, FLAG_X_SHORT, FLAG_X_SAME_OR_POSITIVE);
        final int[] ys = readCoordinates(buffer, flags, FLAG_Y_SHORT, FLAG_Y_SAME_OR_POSITIVE);

        return buildContours(endPtsOfContours, flags, xs, ys);
    }

    private static int[] readEndPoints(ByteBuffer buffer, short numberOfContours) {
        final int[] endPtsOfContours = new int[numberOfContours];

        for (int j = 0; j < numberOfContours; j++) {
            endPtsOfContours[j] = Short.toUnsignedInt(buffer.getShort());
        }

        return endPtsOfContours;
    }

    private static void skipInstructions(ByteBuffer buffer) {
        final int instructionLength = Short.toUnsignedInt(buffer.getShort());
        buffer.position(buffer.position() + instructionLength);
    }

    private static byte[] readFlags(ByteBuffer buffer, int pointCount) {
        final byte[] flags = new byte[pointCount];

        for (int j = 0; j < pointCount; j++) {
            final byte flag = buffer.get();
            flags[j] = flag;

            if ((flag & FLAG_REPEAT) != 0) {
                int repeat = Byte.toUnsignedInt(buffer.get());

                for (int k = 0; k < repeat; k++) {
                    flags[++j] = flag;
                }
            }
        }

        return flags;
    }

    private static int[] readCoordinates(ByteBuffer buffer, byte[] flags, int shortBit, int sameOrPositiveBit) {
        final int[] values = new int[flags.length];

        int current = 0;
        for (int j = 0; j < flags.length; j++) {
            final byte flag = flags[j];

            if ((flag & shortBit) != 0) {
                int delta = Byte.toUnsignedInt(buffer.get());
                current += (flag & sameOrPositiveBit) != 0 ? delta : -delta;
            } else if ((flag & sameOrPositiveBit) == 0) {
                current += buffer.getShort();
            }

            values[j] = current;
        }

        return values;
    }

    private static List<GlyphContour> buildContours(int[] endPtsOfContours, byte[] flags, int[] xs, int[] ys) {
        final List<GlyphContour> contours = new ArrayList<>();

        int start = 0;
        for (int endIndex : endPtsOfContours) {
            final GlyphContour contour = new GlyphContour();

            for (int p = start; p <= endIndex; p++) {
                contour.add(new GlyphPoint(xs[p], ys[p], (flags[p] & FLAG_ON_CURVE) != 0));
            }

            contours.add(contour);
            start = endIndex + 1;
        }

        return contours;
    }
}