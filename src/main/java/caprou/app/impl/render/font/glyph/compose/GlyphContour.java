package caprou.app.impl.render.font.glyph.compose;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GlyphContour {

    private final List<GlyphPoint> points = new ArrayList<>();

    public void add(GlyphPoint p) {
        points.add(p);
    }

}