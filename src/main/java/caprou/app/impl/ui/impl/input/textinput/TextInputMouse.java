package caprou.app.impl.ui.impl.input.textinput;

import caprou.app.impl.util.mouse.MouseUtil;

import java.util.List;

public final class TextInputMouse {
    private static final long DOUBLE_CLICK_INTERVAL_MS = 400L;
    private static final int CLICK_DISTANCE_THRESHOLD = 5;

    private final TextInputState state;
    private final TextInputLayout layout;
    private final int margin;
    private final int lineHeight;
    private final float width;
    private final float height;

    private float x;
    private float y;
    private boolean dragging;

    private long lastClickTime;
    private int lastClickX;
    private int lastClickY;
    private int clickCount;

    public TextInputMouse(TextInputState state, TextInputLayout layout, int margin, int lineHeight, float width, float height) {
        this.state = state;
        this.layout = layout;
        this.margin = margin;
        this.lineHeight = lineHeight;
        this.width = width;
        this.height = height;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean clicked(int mouseX, int mouseY, int mouseButton) {
        boolean hovered = MouseUtil.isHovering(mouseX, mouseY, x, y, width, height);
        state.setFocused(hovered);

        if (!hovered) {
            state.clearSelection();
            clickCount = 0;
            dragging = false;
            return false;
        }

        final int cursorPosition = getCursorPosition(mouseX, mouseY);
        updateClickCount(mouseX, mouseY);

        switch (clickCount) {
            case 3:
                selectCurrentLine(cursorPosition);
                dragging = false;
                break;
            case 2:
                selectCurrentWord(cursorPosition);
                dragging = false;
                break;
            default:
                if (state.isShiftPressed()) {
                    if (state.getSelectionAnchor() == -1) {
                        state.setSelectionAnchor(state.getCursorPosition());
                    }
                } else {
                    state.setSelectionAnchor(cursorPosition);
                }
                state.setCursorPosition(cursorPosition);
                dragging = true;
                break;
        }

        return true;
    }

    public void update(int mouseX, int mouseY) {
        if (!dragging || !state.isFocused())
            return;

        state.setCursorPosition(getCursorPosition(mouseX, mouseY));
    }

    public void released() {
        dragging = false;
    }

    private void updateClickCount(int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        boolean sameSpot = Math.abs(mouseX - lastClickX) <= CLICK_DISTANCE_THRESHOLD && Math.abs(mouseY - lastClickY) <= CLICK_DISTANCE_THRESHOLD;

        if (now - lastClickTime <= DOUBLE_CLICK_INTERVAL_MS && sameSpot) {
            clickCount++;
        } else {
            clickCount = 1;
        }

        clickCount = ((clickCount - 1) % 3) + 1;
        lastClickTime = now;
        lastClickX = mouseX;
        lastClickY = mouseY;
    }

    private void selectCurrentLine(int cursorPosition) {
        final List<TextLine> lines = layout.computeLines(state.getText());
        final TextInputLayout.CursorLocation location = layout.locateCursor(lines, cursorPosition, state.getText().length());
        final TextLine line = lines.get(location.lineIndex());
        state.select(line.getStart(), line.getEnd());
    }

    private void selectCurrentWord(int cursorPosition) {
        final String text = state.getText();

        if (text.isEmpty()) {
            state.clearSelection();
            return;
        }

        final int index = Math.min(cursorPosition, text.length() - 1);
        if (Character.isWhitespace(text.charAt(index))) {
            state.select(cursorPosition, cursorPosition);
            return;
        }

        int start = index;
        int end = index;

        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) {
            start--;
        }

        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
            end++;
        }

        state.select(start, end);
    }

    private int getCursorPosition(int mouseX, int mouseY) {
        final List<TextLine> lines = layout.computeLines(state.getText());
        return layout.getCursorPositionFromMouse(lines, state.getText(), x, y, mouseX, mouseY, margin, lineHeight);
    }
}
