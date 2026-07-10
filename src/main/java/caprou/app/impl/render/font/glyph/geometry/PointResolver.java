package caprou.app.impl.render.font.glyph.geometry;

import caprou.app.impl.render.font.glyph.compose.GlyphContour;
import caprou.app.impl.render.font.glyph.compose.GlyphPoint;

import java.util.List;

public class PointResolver {


    public static GlyphPoint getPointByIndex(List<GlyphContour> contours, int index) {
        int count = 0;

        for (GlyphContour contour : contours) {
            for (GlyphPoint point : contour.getPoints()) {
                if (count == index)
                    return point;

                count++;
            }
        }

        return null;
    }
}
