package caprou.app.impl.render.image;

import java.util.HashMap;

public final class ImageManager {
    public static HashMap<String, ImageObject> menuImages = new HashMap<>(){{
    }};

    public static HashMap<String, ImageObject> inGameImages = new HashMap<>(){{
        //
    }};

    public static void loadMenu() {
        for (ImageObject image : menuImages.values()) {
            image.load();
        }
    }

}
