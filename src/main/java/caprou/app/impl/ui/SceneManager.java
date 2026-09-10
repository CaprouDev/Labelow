package caprou.app.impl.ui;

import caprou.app.impl.interfaces.Consts;

public class SceneManager implements Consts {
    private Scene currentScene;

    public void render(int mouseX, int mouseY) {
        if (currentScene != null) currentScene.render(mouseX, mouseY);
    }

    public void setCurrentScene(final Scene scene) {
        currentScene.stop();
        scene.init();
        currentScene = scene;
        display.setWindowTitle(display.getTitle() + " - " + scene.getName());
        System.out.println("Switched to scene: " + scene.getName());
    }

    public void setInitScene(final Scene scene) {
        scene.init();
        currentScene = scene;
        display.setWindowTitle(display.getTitle() + " - " + scene.getName());
        System.out.println("Initialized to scene: " + scene.getName());
    }

    public void onMouseClicked(final int mouseX, final int mouseY, final int mouseButton){
        currentScene.onMouseClicked(mouseX, mouseY, mouseButton);
    }


    public void onMouseReleased() {
        currentScene.onMouseReleased();
    }

    public void onKeyPressed(int keyCode) {
        if(currentScene != null) currentScene.onKeyPressed(keyCode);
    }
    public void onChar(char c) {
        if(currentScene != null) currentScene.onChar(c);
    }

    public void onKeyReleased(int keyCode) {
        if(currentScene != null) currentScene.onKeyReleased(keyCode);
    }

    public void onScroll(double scrollDelta) {
        if (currentScene != null) {
            currentScene.onScroll(scrollDelta*10);
        }
    }

    private void crash() {
        Object[] o = null;

        while (true) {
            o = new Object[] {o};
        }
    }
}