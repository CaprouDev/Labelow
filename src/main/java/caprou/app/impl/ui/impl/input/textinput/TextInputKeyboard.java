package caprou.app.impl.ui.impl.input.textinput;

import caprou.app.impl.util.keyboard.ClipboardManager;
import caprou.app.impl.util.keyboard.KeyCodes;

import java.util.List;

public final class TextInputKeyboard {
    private final TextInputState state;
    private final TextInputLayout layout;
    private final ClipboardManager clipboard;
    private final Runnable blinkResetter;

    public TextInputKeyboard(TextInputState state, TextInputLayout layout, ClipboardManager clipboard, Runnable blinkResetter) {
        this.state = state;
        this.layout = layout;
        this.clipboard = clipboard;
        this.blinkResetter = blinkResetter;
    }

    public void keyPressed(int keyCode) {
        if (!state.isFocused())
            return;

        if (KeyCodes.isCtrl(keyCode)) {
            state.setCtrlPressed(true);
            return;
        }

        if (KeyCodes.isShift(keyCode)) {
            beginSelection();
            state.setShiftPressed(true);
            return;
        }

        if (state.isCtrlPressed() && handleClipboardShortcut(keyCode)) {
            return;
        }

        switch (keyCode) {
            case KeyCodes.ENTER:
                insert("\n");
                return;
            case KeyCodes.BACKSPACE:
                deleteBackward();
                return;
            case KeyCodes.DELETE:
                deleteForward();
                return;
            case KeyCodes.LEFT:
                moveHorizontal(-1);
                return;
            case KeyCodes.RIGHT:
                moveHorizontal(1);
                return;
            case KeyCodes.UP:
                moveVertical(-1);
                return;
            case KeyCodes.DOWN:
                moveVertical(1);
                return;
            case KeyCodes.HOME:
                moveHome();
                return;
            case KeyCodes.END:
                moveEnd();
                return;
            default:
                return;
        }
    }

    public void keyReleased(int keyCode) {
        if (KeyCodes.isCtrl(keyCode)) {
            state.setCtrlPressed(false);
        }

        if (KeyCodes.isShift(keyCode)) {
            state.setShiftPressed(false);
            if (state.getSelectionAnchor() == state.getCursorPosition()) {
                state.clearSelection();
            }
        }
    }

    public void characterTyped(char character) {
        if (!state.isFocused() || !isAllowedChar(character)) {
            return;
        }

        insert(String.valueOf(character));
    }

    private boolean handleClipboardShortcut(int keyCode) {
        switch (keyCode) {
            case KeyCodes.A:
                state.selectAll();
                resetBlink();
                return true;
            case KeyCodes.C:
                copySelection();
                return true;
            case KeyCodes.X:
                cutSelection();
                return true;
            case KeyCodes.V:
                insert(clipboard.paste());
                return true;
            default:
                return false;
        }
    }

    private void deleteBackward() {
        if (state.hasSelection()) {
            deleteSelection();
            return;
        }

        final int cursor = state.getCursorPosition();
        if (cursor <= 0)
            return;

        if (state.isCtrlPressed()) {
            final int newCursor = previousWordBoundary(cursor);

            replaceRange(newCursor, cursor, "");
        } else {
            replaceRange(cursor - 1, cursor, "");
        }
    }

    private void deleteForward() {
        if (state.hasSelection()) {
            deleteSelection();
            return;
        }

        final int cursor = state.getCursorPosition();
        final String text = state.getText();

        if (cursor >= text.length())
            return;

        if (state.isCtrlPressed()) {
            final int end = nextWordBoundary(cursor);

            replaceRange(cursor, end, "");
        } else {
            replaceRange(cursor, cursor + 1, "");
        }
    }

    private void moveHorizontal(int direction) {
        if (state.isShiftPressed()) {
            beginSelection();
            if (state.isCtrlPressed()) {
                state.setCursorPosition(direction < 0 ? previousWordBoundary(state.getCursorPosition()) : nextWordBoundary(state.getCursorPosition()));
            } else {
                state.setCursorPosition(state.getCursorPosition() + direction);
            }
            resetBlink();
            return;
        }

        if (state.hasSelection()) {
            state.setCursorPosition(direction < 0 ? state.getSelectionStart() : state.getSelectionEnd());
            state.clearSelection();
            resetBlink();
            return;
        }

        if (state.isCtrlPressed()) {
            state.setCursorPosition(direction < 0 ? previousWordBoundary(state.getCursorPosition()) : nextWordBoundary(state.getCursorPosition()));
        } else {
            state.setCursorPosition(state.getCursorPosition() + direction);
        }

        resetBlink();
    }

    private void moveVertical(int direction) {
        if (state.isShiftPressed()) {
            beginSelection();
        } else {
            state.clearSelection();
        }

        List<TextLine> lines = layout.computeLines(state.getText());
        TextInputLayout.CursorLocation location = layout.locateCursor(lines, state.getCursorPosition(), state.getText().length());

        final int targetLine = location.lineIndex() + direction;
        final int targetPosition;

        if (targetLine < 0) {
            targetPosition = 0;
        } else if (targetLine >= lines.size()) {
            targetPosition = state.getText().length();
        } else {
            TextLine line = lines.get(targetLine);
            targetPosition = line.getStart() + Math.min(location.column(), line.length());
        }

        state.setCursorPosition(targetPosition);
        resetBlink();
    }

    private void moveHome() {
        if (!state.isShiftPressed())
            state.clearSelection();

        final String text = state.getText();
        final int cursor = state.getCursorPosition();
        final int newline = text.lastIndexOf("\n", Math.max(0, cursor - 1));

        state.setCursorPosition(newline + 1);

        resetBlink();
    }

    private void moveEnd() {
        if (!state.isShiftPressed())
            state.clearSelection();

        final String text = state.getText();
        final int cursor = state.getCursorPosition();
        final int newline = text.indexOf("\n", cursor);

        state.setCursorPosition(newline == -1 ? text.length() : newline);

        resetBlink();
    }

    private void insert(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return;
        }

        final String textToInsert = filterAllowedCharacters(rawText);
        if (textToInsert.isEmpty()) {
            return;
        }

        final String currentText = state.getText();


        int insertionPoint = state.getCursorPosition();
        String baseText = currentText;

        if (state.hasSelection()) {
            baseText = currentText.substring(0, state.getSelectionStart()) + currentText.substring(state.getSelectionEnd());
            insertionPoint = state.getSelectionStart();
        }

        final String candidate = baseText.substring(0, insertionPoint) + textToInsert + baseText.substring(insertionPoint);

        if (layout.exceedsHeight(candidate)) {
            return;
        }

        state.setText(candidate);
        state.setCursorPosition(insertionPoint + textToInsert.length());
        state.clearSelection();
        resetBlink();
    }

    private void copySelection() {
        if (!state.hasSelection())
            return;


        clipboard.copy(state.getText().substring(state.getSelectionStart(), state.getSelectionEnd()));
    }

    private void cutSelection() {
        if (!state.hasSelection())
            return;

        copySelection();
        deleteSelection();
    }

    private void deleteSelection() {
        if (!state.hasSelection())
            return;

        replaceRange(state.getSelectionStart(), state.getSelectionEnd(), "");
    }

    private void replaceRange(int start, int end, final String replacement) {
        final String text = state.getText();
        final String result = text.substring(0, start) + replacement + text.substring(end);

        state.setText(result);
        state.setCursorPosition(start + replacement.length());
        state.clearSelection();

        resetBlink();
    }

    private int previousWordBoundary(int position) {
        final String text = state.getText();

        int index = Math.max(0, Math.min(position, text.length()));

        while (index > 0 && Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }

        while (index > 0 && !Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }

        return index;
    }

    private int nextWordBoundary(int position) {
        final String text = state.getText();

        int index = Math.max(0, Math.min(position, text.length()));
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }

        while (index < text.length() && !Character.isWhitespace(text.charAt(index))) {
            index++;
        }

        return index;
    }

    private void beginSelection() {
        if (state.getSelectionAnchor() == -1) {
            state.setSelectionAnchor(state.getCursorPosition());
        }
    }

    private String filterAllowedCharacters(String text) {
        final StringBuilder result = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            if (isAllowedChar(character)) {
                result.append(character);
            }
        }
        return result.toString();
    }

    private boolean isAllowedChar(char character) {
        if (character == '\n') {
            return true;
        }
        if (character < 32 || character == 127) {
            return false;
        }
        if (character <= 126) {
            return true;
        }
        if (character >= 0x00A0 && character <= 0x00FF) {
            return true;
        }
        return character == 0x0152 || character == 0x0153 || character == 0x20AC;
    }

    private void resetBlink() {
        blinkResetter.run();
    }
}
