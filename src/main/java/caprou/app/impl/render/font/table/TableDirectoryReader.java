package caprou.app.impl.render.font.table;

import caprou.app.impl.render.font.TrueTypeFont;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class TableDirectoryReader {

    private TableDirectoryReader() {
    }

    public static void readTables(ByteBuffer buffer, TrueTypeFont trueTypeFont, int numTables) {
        for (int i = 0; i < numTables; i++) {
            byte[] tagBytes = new byte[4];
            buffer.get(tagBytes);

            final String tag = new String(tagBytes, StandardCharsets.US_ASCII);
            final long checksum = Integer.toUnsignedLong(buffer.getInt());
            final long offset = Integer.toUnsignedLong(buffer.getInt());
            final long length = Integer.toUnsignedLong(buffer.getInt());

            trueTypeFont.tables().put(tag, new TableRecord(tag, checksum, offset, length));
        }
    }
}