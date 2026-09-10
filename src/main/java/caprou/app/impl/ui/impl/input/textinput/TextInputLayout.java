package caprou.app.impl.ui.impl.input.textinput;

import caprou.app.impl.render.font.renderer.Fonts;
import caprou.app.impl.util.arithmetics.MathUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TextInputLayout {
    private final float width;
    @Getter
    private final int numberLines;
    @Getter
    private final int textSize;
    private final int horizontalPadding;

    public TextInputLayout(float width, int numberLines, int textSize, int horizontalPadding) {
        this.width = width;
        this.numberLines = numberLines;
        this.textSize = textSize;
        this.horizontalPadding = horizontalPadding;
    }

    public List<TextLine> computeLines(String text) {
        if (text == null || text.isEmpty())
            return Collections.singletonList(new TextLine(0, 0));

        final List<TextLine> lines = new ArrayList<TextLine>();
        final float maxWidth = getMaxTextWidth();
        final String[] paragraphs = text.split("\\n", -1);


        int offset = 0;
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add(new TextLine(offset, offset));
            } else {
                appendWrappedParagraph(lines, paragraph, offset, maxWidth);
            }

            offset += paragraph.length() + 1;
        }

        return lines;
    }

    private void appendWrappedParagraph(List<TextLine> lines, String paragraph, int offset, float maxWidth) {
        int lineStart = 0;
        int lastSpace = -1;
        int index = 0;

        while (index < paragraph.length()) {
            final float textWidth = Fonts.INTER.measureWidth(paragraph.substring(lineStart, index + 1), textSize);

            if (textWidth > maxWidth && index > lineStart) {
                int breakAt = lastSpace >= lineStart ? lastSpace + 1 : index;
                breakAt = Math.max(lineStart + 1, breakAt);

                lines.add(new TextLine(offset + lineStart, offset + breakAt));
                lineStart = breakAt;
                lastSpace = -1;
                index = lineStart;
                continue;
            }

            if (Character.isWhitespace(paragraph.charAt(index))) {
                lastSpace = index;
            }

            index++;
        }

        lines.add(new TextLine(offset + lineStart, offset + paragraph.length()));
    }

    public boolean exceedsHeight(String text) {
        return computeLines(text).size() > numberLines;
    }

    public CursorLocation locateCursor(List<TextLine> lines, int cursorPosition, int textLength) {
        if (lines.isEmpty())
            return new CursorLocation(0, 0);

        int index = MathUtil.clamp(cursorPosition, 0, textLength);

        for (int i = 0; i < lines.size(); i++) {
            TextLine line = lines.get(i);
            boolean lastLine = i == lines.size() - 1;

            if (line.contains(index, lastLine)) {
                return new CursorLocation(i, index - line.getStart());
            }
        }

        TextLine last = lines.getLast();
        return new CursorLocation(lines.size() - 1, last.length());
    }

    public int getCursorPositionFromMouse(List<TextLine> lines, String text, float componentX, float componentY, float mouseX, float mouseY, int margin, int lineHeight) {
        if (lines.isEmpty())
            return 0;

        final int relativeY = (int) (mouseY - (componentY + margin));
        final int lineIndex = MathUtil.clamp(relativeY / lineHeight, 0, lines.size() - 1);
        final TextLine line = lines.get(lineIndex);
        final String lineText = text.substring(line.getStart(), line.getEnd());

        final float relativeX = mouseX - (componentX + horizontalPadding);
        int column = 0;
        float accumulatedWidth = 0;

        while (column < lineText.length()) {
            float charWidth = Fonts.INTER.measureWidth(
                    String.valueOf(lineText.charAt(column)),
                    textSize
            );

            if (accumulatedWidth + charWidth / 2f > relativeX) {
                break;
            }

            accumulatedWidth += charWidth;
            column++;
        }

        return line.getStart() + column;
    }

    public float getMaxTextWidth() {
        return width - horizontalPadding - 15;
    }

    public record CursorLocation(int lineIndex, int column) { /* */ }

}
