package caprou.app;

import caprou.app.impl.render.SimpleRenderer;
import caprou.app.impl.render.display.Display;
import caprou.app.impl.ui.SceneManager;
import lombok.Getter;

import java.io.IOException;

public class Main {

    @Getter private static Display display;
    @Getter private static SceneManager sceneManager;
    @Getter private static SimpleRenderer renderer;

    @Getter private static String appName = "Labelow";
    @Getter private static String appVersion = "Beta 0.1";

    static {
        display = new Display("Labelow" ,800,600);
        sceneManager = new SceneManager();
        renderer = SimpleRenderer.getInstance();
    }


    public static void main(String[] args) throws IOException {

        display.run();
    }
}