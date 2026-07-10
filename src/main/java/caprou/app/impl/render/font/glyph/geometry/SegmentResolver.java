package caprou.app.impl.render.font.glyph.geometry;

import caprou.app.impl.render.font.glyph.compose.GlyphContour;
import caprou.app.impl.render.font.glyph.compose.GlyphPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SegmentResolver {

    public static List<BezierSegment> buildSegments(List<GlyphContour> contours) {
        final List<BezierSegment> segments = new ArrayList<>();

        for (GlyphContour contour : contours) {
            final List<GlyphPoint> points = contour.getPoints();

            if (points == null || points.isEmpty()) {
                continue;
            }

            final int n = points.size();
            final GlyphPoint first = points.getFirst();
            final GlyphPoint last = points.get(n - 1);

            int index;
            GlyphPoint current;
            GlyphPoint startPoint;

            if (first.onCurve()) {
                current = first;
                startPoint = first;
                index = 1;
            } else if (last.onCurve()) {
                current = last;
                startPoint = last;
                index = 0;
            } else {
                current = midpoint(last, first);
                startPoint = current;
                index = 0;
            }

            while (index < n) {
                final GlyphPoint next = points.get(index);

                if (next.onCurve()) {
                    segments.add(new BezierSegment(current, null, next));
                    current = next;
                    index++;
                } else {
                    GlyphPoint after = points.get((index + 1) % n);

                    if (after.onCurve()) {
                        segments.add(new BezierSegment(current, next, after));
                        current = after;
                        index += 2;
                    } else {
                        GlyphPoint mid = midpoint(next, after);
                        segments.add(new BezierSegment(current, next, mid));
                        current = mid;
                        index++;
                    }
                }
            }

            if (current != startPoint) {
                segments.add(new BezierSegment(current, null, startPoint));
            }
        }

        return segments;
    }

    private static GlyphPoint midpoint(GlyphPoint a, GlyphPoint b) {
        return new GlyphPoint(
                (a.x() + b.x()) / 2,
                (a.y() + b.y()) / 2,
                true
        );
    }

    public static List<List<FlatPoint>> flattenContours(List<GlyphContour> contours, int subdivisions) {
        final List<List<FlatPoint>> result = new ArrayList<>();

        for (GlyphContour contour : contours) {
            final List<BezierSegment> segments = buildSegments(Collections.singletonList(contour));
            final List<FlatPoint> flatContour = flattenSegments(segments, subdivisions);

            result.add(flatContour);
        }

        return result;
    }

    private static List<FlatPoint> flattenSegments(List<BezierSegment> segments, int subdivisions) {
        final List<FlatPoint> result = new ArrayList<>();

        if (segments.isEmpty()) {
            return result;
        }

        final BezierSegment first = segments.getFirst();
        result.add(new FlatPoint(first.start().x(), first.start().y()));

        for (BezierSegment segment : segments) {
            final GlyphPoint start = segment.start();
            final GlyphPoint control = segment.control();
            final GlyphPoint end = segment.end();

            if (control == null) {
                result.add(new FlatPoint(end.x(), end.y()));
            } else {

                for (int i = 1; i <= subdivisions; i++) {
                    float t = i / (float) subdivisions;
                    float mt = 1.0f - t;

                    float x = mt * mt * start.x() + 2.0f * mt * t * control.x() + t * t * end.x();
                    float y = mt * mt * start.y() + 2.0f * mt * t * control.y() + t * t * end.y();

                    result.add(new FlatPoint(x, y));
                }
            }
        }

        return result;
    }
}