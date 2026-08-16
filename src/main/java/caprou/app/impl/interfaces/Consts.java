package caprou.app.impl.interfaces;

import caprou.app.Main;
import caprou.app.impl.render.SimpleRenderer;
import caprou.app.impl.render.display.Display;

public interface Consts {
    Display display = Main.getDisplay();
    SimpleRenderer renderer = Main.getRenderer();
}
