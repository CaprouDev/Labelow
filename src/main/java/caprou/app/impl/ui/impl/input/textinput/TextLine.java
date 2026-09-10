package caprou.app.impl.ui.impl.input.textinput;

public final class TextLine {
    private final int start;
    private final int end;

    public TextLine(int start, int end) {
        this.start = start;
        this.end = Math.max(start, end);
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int length() {
        return end - start;
    }

    public boolean contains(int index, boolean includeEnd) {
        return index >= start && (index < end || includeEnd && index <= end);
    }

    public int clamp(int index) {
        return Math.max(start, Math.min(index, end));
    }
}
