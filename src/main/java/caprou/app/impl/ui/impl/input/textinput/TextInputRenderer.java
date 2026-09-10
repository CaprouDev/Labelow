package caprou.app.impl.ui.impl.input.textinput;

import caprou.app.impl.interfaces.Consts;
import caprou.app.impl.render.animation.Animation;
import caprou.app.impl.render.animation.Easing;
import caprou.app.impl.render.font.renderer.Fonts;

import java.awt.Color;
import java.util.List;

public final class TextInputRenderer implements Consts {
    private static final Color OUTER_BACKGROUND = new Color(238, 238, 238);
    private static final Color INNER_BACKGROUND = new Color(250, 250, 248);
    private static final Color TEXT_COLOR = new Color(40, 40, 40);
    private static final Color PLACEHOLDER_COLOR = new Color(180, 180, 180);
    private static final Color SELECTION_COLOR = new Color(180, 205, 250, 150);
    private static final Color CURSOR_COLOR = new Color(60, 60, 60);

    private final String placeholder;
    private final int fontSize;
    private final int textSize;
    private final int margin;
    private final int horizontalPadding;
    private final long blinkInterval;

    private final Animation cursorXAnimation;
    private final Animation cursorYAnimation;

    private long lastBlinkTime = System.currentTimeMillis();
    private boolean cursorVisible = true;

    public TextInputRenderer(String placeholder, int fontSize, int textSize, int margin, int horizontalPadding) {
        this.placeholder = placeholder;
        this.fontSize = fontSize;
        this.textSize = textSize;
        this.margin = margin;
        this.horizontalPadding = horizontalPadding;
        this.blinkInterval = 500L;

        this.cursorXAnimation = new Animation(Easing.EASE_OUT_EXPO, 300);
        this.cursorYAnimation = new Animation(Easing.EASE_OUT_EXPO, 300);
    }

    public void resetBlink() {
        cursorVisible = true;
        lastBlinkTime = System.currentTimeMillis();
    }

    public void draw(float x, float y, float width, float height, TextInputState state, TextInputLayout layout) {
        renderer.drawRound(x, y, width, height, 20, OUTER_BACKGROUND);
        renderer.drawRound(x + 1, y + 1, width - 2, height - 2, 20, INNER_BACKGROUND);

        List<TextLine> lines = layout.computeLines(state.getText());

        if (state.hasSelection()) {
            drawSelection(x, y, state, lines);
        }

        drawText(x, y, state, lines);

        if (state.getText().isEmpty() && !state.isFocused()) {
            Fonts.INTER.drawString(placeholder, x + horizontalPadding, y + margin, textSize, PLACEHOLDER_COLOR);
        }

        if (state.isFocused()) {
            drawCursor(x, y, state, lines, layout);
        }
    }

    private void drawText(float x, float y, TextInputState state, List<TextLine> lines) {
        final String text = state.getText();

        for (int i = 0; i < lines.size(); i++) {
            final TextLine line = lines.get(i);
            Fonts.INTER.drawString(text.substring(line.getStart(), line.getEnd()), x + horizontalPadding, y + margin + i * fontSize, textSize, TEXT_COLOR);
        }
    }

    private void drawSelection(float x, float y, TextInputState state, List<TextLine> lines) {
        int selectionStart = state.getSelectionStart();
        int selectionEnd = state.getSelectionEnd();
        String text = state.getText();

        for (int i = 0; i < lines.size(); i++) {
            TextLine line = lines.get(i);
            int start = Math.max(line.getStart(), selectionStart);
            int end = Math.min(line.getEnd(), selectionEnd);

            if (start >= end) {
                continue;
            }

            final String beforeSelection = text.substring(line.getStart(), start);
            final String selectedText = text.substring(start, end);

            final float selectionX = x + horizontalPadding + Fonts.INTER.measureWidth(beforeSelection, textSize);
            final float selectionWidth = Fonts.INTER.measureWidth(selectedText, textSize);
            final float selectionY = y + margin + i * fontSize;

            renderer.drawRect(selectionX, selectionY, selectionWidth, fontSize - 2, SELECTION_COLOR);
        }
    }

    private void drawCursor(float x, float y, TextInputState state, List<TextLine> lines, TextInputLayout layout) {
        updateBlink();

        if (!cursorVisible || lines.isEmpty()) {
            return;
        }

        final int cursorPosition = state.getCursorPosition();
        TextInputLayout.CursorLocation location = layout.locateCursor(
                lines,
                cursorPosition,
                state.getText().length()
        );

        final TextLine line = lines.get(location.lineIndex());
        final int clampedCursor = line.clamp(cursorPosition);


        final String beforeCursor = state.getText().substring(line.getStart(), clampedCursor);

        float cursorX = x + horizontalPadding + Fonts.INTER.measureWidth(beforeCursor, textSize);
        float cursorY = y + margin + location.lineIndex() * fontSize;

        cursorXAnimation.run(cursorX);
        cursorYAnimation.run(cursorY);

        renderer.drawRect((float) cursorXAnimation.getValue(), (float) cursorYAnimation.getValue(), 1, fontSize, CURSOR_COLOR);
    }

    private void updateBlink() {
        long now = System.currentTimeMillis();
        if (now - lastBlinkTime >= blinkInterval) {
            cursorVisible = !cursorVisible;
            lastBlinkTime = now;
        }
    }
}
