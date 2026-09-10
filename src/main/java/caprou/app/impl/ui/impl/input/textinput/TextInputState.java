package caprou.app.impl.ui.impl.input.textinput;

public final class TextInputState {
    private String text = "";
    private int cursorPosition;
    private int selectionAnchor = -1;
    private boolean focused;
    private boolean ctrlPressed;
    private boolean shiftPressed;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        cursorPosition = clamp(cursorPosition);
        if (selectionAnchor > this.text.length()) {
            selectionAnchor = -1;
        }
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public void setCursorPosition(int position) {
        cursorPosition = clamp(position);
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public boolean hasSelection() {
        return selectionAnchor >= 0 && selectionAnchor != cursorPosition;
    }

    public int getSelectionAnchor() {
        return selectionAnchor;
    }

    public void setSelectionAnchor(int position) {
        selectionAnchor = position < 0 ? -1 : clamp(position);
    }

    public void clearSelection() {
        selectionAnchor = -1;
    }

    public int getSelectionStart() {
        return Math.min(selectionAnchor, cursorPosition);
    }

    public int getSelectionEnd() {
        return Math.max(selectionAnchor, cursorPosition);
    }

    public boolean isCtrlPressed() {
        return ctrlPressed;
    }

    public void setCtrlPressed(boolean ctrlPressed) {
        this.ctrlPressed = ctrlPressed;
    }

    public boolean isShiftPressed() {
        return shiftPressed;
    }

    public void setShiftPressed(boolean shiftPressed) {
        this.shiftPressed = shiftPressed;
    }

    public void selectAll() {
        selectionAnchor = 0;
        cursorPosition = text.length();
    }

    public void select(int start, int end) {
        selectionAnchor = clamp(start);
        cursorPosition = clamp(end);
    }

    private int clamp(int position) {
        return Math.max(0, Math.min(position, text.length()));
    }
}
