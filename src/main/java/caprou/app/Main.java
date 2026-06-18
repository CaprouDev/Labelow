package caprou.app;

import caprou.app.impl.render.display.Display;
import lombok.Getter;

import java.io.IOException;

public class Main {

    @Getter private static Display display;

    static {
        display = new Display("Labelow" ,800,600);
    }


    public static void main(String[] args) throws IOException {

        display.run();
    }
}