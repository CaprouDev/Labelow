package caprou.app.impl.render.font;

import caprou.app.impl.render.font.cmap.CmapReader;
import caprou.app.impl.render.font.glyph.composite.CompositeResolver;
import caprou.app.impl.render.font.glyph.GlyfTableReader;
import caprou.app.impl.render.font.glyph.LocaTableReader;
import caprou.app.impl.render.font.kerning.KerningReader;
import caprou.app.impl.render.font.metrics.FontMetricsReader;
import caprou.app.impl.render.font.table.TableDirectoryReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class TrueTypeFontReader {

    public TrueTypeFont parseFont(InputStream inputStream) {
        try {
            ByteBuffer buffer = readIntoBuffer(inputStream);
            TrueTypeFont trueTypeFont = new TrueTypeFont();

            buffer.getInt();   // sfnt version
            int numTables = Short.toUnsignedInt(buffer.getShort());
            buffer.getShort(); //search range
            buffer.getShort(); // entry selector
            buffer.getShort(); // range shift

            TableDirectoryReader.readTables(buffer, trueTypeFont, numTables);
            FontMetricsReader.parseHeadAndHheaMetrics(buffer, trueTypeFont);

            int numGlyphs = readNumGlyphs(buffer, trueTypeFont);
            int indexToLocFormat = readIndexToLocFormat(buffer, trueTypeFont);

            long[] glyphOffsets = LocaTableReader.readGlyphOffsets(buffer, trueTypeFont, numGlyphs, indexToLocFormat);

            GlyfTableReader.parseGlyphs(buffer, trueTypeFont, glyphOffsets, numGlyphs);
            CompositeResolver.resolveCompositeGlyphs(trueTypeFont);

            FontMetricsReader.parseHmtx(buffer, trueTypeFont, numGlyphs);
            CmapReader.parseCmap(buffer, trueTypeFont);
            KerningReader.parseKern(buffer, trueTypeFont);

            return trueTypeFont;
        } catch (IOException e) {
            throw new FontParseException("Echec de lecture du flux de la font", e);
        }
    }

    private ByteBuffer readIntoBuffer(InputStream inputStream) throws IOException {
        byte[] data = inputStream.readAllBytes();

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.BIG_ENDIAN);

        return buffer;
    }

    private int readNumGlyphs(ByteBuffer buffer, TrueTypeFont trueTypeFont) {
        buffer.position((int) trueTypeFont.requireTable("maxp").offset());
        buffer.getInt(); //version
        return Short.toUnsignedInt(buffer.getShort());
    }

    private int readIndexToLocFormat(ByteBuffer buffer, TrueTypeFont trueTypeFont) {
        buffer.position((int) trueTypeFont.requireTable("head").offset() + 50);
        return buffer.getShort();
    }
}