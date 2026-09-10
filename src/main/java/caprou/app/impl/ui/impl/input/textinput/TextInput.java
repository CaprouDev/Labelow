package caprou.app.impl.ui.impl.input.textinput;

import caprou.app.impl.interfaces.Consts;
import caprou.app.impl.util.keyboard.ClipboardManager;
import lombok.Getter;

public final class TextInput implements Consts {
    private static final int LINE_HEIGHT = 16;
    private static final int TEXT_SIZE = 14;
    private static final int MARGIN = 15;
    private static final int HORIZONTAL_PADDING = 18;

    @Getter private final String placeholderText;
    @Getter private final int numberLines;
    @Getter private final float width;
    @Getter private final float height;

    private final TextInputState state;
    private final TextInputLayout layout;
    private final ClipboardManager clipboard;
    private final TextInputRenderer textRenderer;
    private final TextInputKeyboard keyboard;
    private final TextInputMouse mouse;

    private float x;
    private float y;

    public TextInput(String placeholderText, int numberLines, double x, double y, float width) {
        this.placeholderText = placeholderText;
        this.numberLines = Math.max(1, numberLines);
        this.width = width;
        this.height = this.numberLines * LINE_HEIGHT + MARGIN * 2;

        this.state = new TextInputState();
        this.layout = new TextInputLayout(width, this.numberLines, TEXT_SIZE, HORIZONTAL_PADDING);
        this.clipboard = new ClipboardManager();
        this.textRenderer = new TextInputRenderer(placeholderText, LINE_HEIGHT, TEXT_SIZE, MARGIN, HORIZONTAL_PADDING, x, y);
        this.keyboard = new TextInputKeyboard(state, layout, clipboard, textRenderer::resetBlink);
        this.mouse = new TextInputMouse(state, layout, MARGIN, LINE_HEIGHT, width, height);
    }

    public void draw(float x, float y, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;

        mouse.setPosition(x, y);
        mouse.update(mouseX, mouseY);
        textRenderer.draw(x, y, width, height, state, layout);
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        mouse.setPosition(x, y);
        if (mouse.clicked(mouseX, mouseY, mouseButton)) {
            textRenderer.resetBlink();
        }
    }

    public void mouseReleased() {
        mouse.released();
    }

    public void keyTyped(int keyCode) {
        keyboard.keyPressed(keyCode);
    }

    public void onKeyReleased(int keyCode) {
        keyboard.keyReleased(keyCode);
    }

    public void onChar(char character) {
        keyboard.characterTyped(character);
    }

    public String getText() {
        return state.getText();
    }

    public void setText(String text) {
        state.setText(text);
        textRenderer.resetBlink();
    }

    public void setFocused(boolean focused) {
        state.setFocused(focused);
        textRenderer.resetBlink();
    }


}
