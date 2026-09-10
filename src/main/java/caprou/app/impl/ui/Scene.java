package caprou.app.impl.ui;

public abstract class Scene {
    public abstract void init();
    public abstract void stop();
    public abstract void render(int mouseX, int mouseY);
    public abstract String getName();
    public abstract void onMouseClicked(int mouseX, int mouseY, int mouseButton);
    public abstract void onMouseReleased();
    public abstract void onKeyPressed(int keyCode);
    public abstract void onChar(char c);
    public abstract void onKeyReleased(int keyCode);


    public void onScroll(double scrollDelta) {
        /* */
    }

}