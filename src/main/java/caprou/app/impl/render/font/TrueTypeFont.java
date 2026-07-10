package caprou.app.impl.render.font;

import caprou.app.impl.render.font.glyph.Glyph;
import caprou.app.impl.render.font.table.TableRecord;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public final class TrueTypeFont {

    private final Map<String, TableRecord> tables = new HashMap<>();
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Map<Integer, Integer> codepointToGlyph = new HashMap<>();
    private final Map<Long, Short> kerning = new HashMap<>();

    @Getter @Setter
    private int unitsPerEm;
    @Getter @Setter
    private short ascent;
    @Getter @Setter
    private short descent;
    @Getter @Setter
    private short lineGap;

    public Map<String, TableRecord> tables() {
        return tables;
    }

    public Map<Integer, Glyph> glyphs() {
        return glyphs;
    }

    public Map<Integer, Integer> codepointToGlyph() {
        return codepointToGlyph;
    }

    public Map<Long, Short> kerning() {
        return kerning;
    }

    public TableRecord requireTable(String tag) {
        TableRecord record = tables.get(tag);

        if (record == null) {
            throw new FontParseException("Table requise manquante dans la font : " + tag);
        }

        return record;
    }
}