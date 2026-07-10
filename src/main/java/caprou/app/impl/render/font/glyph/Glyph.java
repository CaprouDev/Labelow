package caprou.app.impl.render.font.glyph;

import caprou.app.impl.render.font.glyph.compose.GlyphContour;
import caprou.app.impl.render.font.glyph.composite.CompositeComponent;
import caprou.app.impl.render.font.glyph.geometry.BezierSegment;
import caprou.app.impl.render.font.glyph.geometry.FlatPoint;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class Glyph {

    private final int index;
    private final short numberOfContours;

    private final short xMin;
    private final short yMin;
    private final short xMax;
    private final short yMax;

    private final long offset;
    private final long length;
    private int advanceWidth;
    private short leftSideBearing;

    private int[] endPtsOfContours;
    private byte[] flags;
    private int[] xCoordinates;
    private int[] yCoordinates;

    private List<GlyphContour> contours;
    private List<BezierSegment> segments;
    private List<List<FlatPoint>> flatContours;
    private List<CompositeComponent> components;


    public boolean isComposite() { // quand c un glyphe qui reference plusieurs glyphe (i)
        return numberOfContours < 0;
    }

}