package caprou.app.impl.ui.impl.scene;

import caprou.app.Main;
import caprou.app.impl.interfaces.Consts;
import caprou.app.impl.render.animation.Animation;
import caprou.app.impl.render.animation.Easing;
import caprou.app.impl.render.font.renderer.Fonts;
import caprou.app.impl.render.util.time.TimeUtil;
import caprou.app.impl.ui.Scene;

import java.awt.*;

public class ProjectScene extends Scene implements Consts {

    @Override
    public void init() {
        final int[] monitorSize = display.getCurrentMonitorSize();
        final int width = (int) (monitorSize[0]*0.75f),
                  height = (int) (monitorSize[1]*0.75f);

        final int[] displayPosition = new int[] {
                monitorSize[0]/2 - width/2,
                monitorSize[1]/2 - height/2
        };

        display.setPosition(displayPosition[0], displayPosition[1]);
        display.setSize(width, height);
        display.setResizable(true);
        display.setDecorated(true);
    }

    @Override
    public void stop() {

    }

    @Override
    public void render(int mouseX, int mouseY) {
        renderer.drawRect(0,0, display.getWidth(), display.getHeight(), Color.WHITE);
    }

    @Override
    public String getName() {
        return "Project scene";
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int mouseButton) {

    }

    @Override
    public void onMouseReleased() {

    }

    @Override
    public void onKeyPressed(int keyCode, char c) {

    }

    @Override
    public void onKeyReleased(int keyCode) {

    }

    private void load() {

    }
}
