package caprou.app.impl.render.font.glyph.geometry;

import caprou.app.impl.render.font.glyph.compose.GlyphPoint;

public record BezierSegment(GlyphPoint start, GlyphPoint control, GlyphPoint end) {
}