package caprou.app.impl.ui.impl.scene.startup;

import caprou.app.Main;
import caprou.app.impl.interfaces.Consts;
import caprou.app.impl.render.animation.Animation;
import caprou.app.impl.render.animation.Easing;
import caprou.app.impl.render.font.renderer.Fonts;
import caprou.app.impl.render.util.time.TimeUtil;
import caprou.app.impl.ui.Scene;
import caprou.app.impl.ui.impl.scene.projet.ProjectScene;

import java.awt.*;

public class StartupScene extends Scene implements Consts {
    private boolean loading = true;

    private Animation animation = new Animation(Easing.EASE_IN_OUT_CUBIC, 1000);
    TimeUtil timer = new TimeUtil();


    @Override
    public void init() {
        final int[] monitorSize = display.getCurrentMonitorSize();
        final int width = 500, height = 350;
        final int[] displayPosition = new int[] {
                monitorSize[0]/2 - width/2,
                monitorSize[1]/2 - height/2
        };

        display.setPosition(displayPosition[0], displayPosition[1]);
        display.setSize(width, height);
        display.setResizable(false);
        display.setDecorated(false);

        load();
        loading = false;

        timer.reset();
    }

    @Override
    public void stop() {

    }

    @Override
    public void onChar(char c) {

    }

    @Override
    public void render(int mouseX, int mouseY) {
        renderer.drawRect(0,0, display.getWidth(), display.getHeight(), Color.WHITE);

        final Color black = new Color(0,0,0,(int) (255 * (1-animation.getValue())));
        final Color mint = new Color(168,251,171,(int) (255 * (1-animation.getValue())));

        Fonts.INSTRUMENT_SERIF.drawString(Main.getAppName(), (float) (33 - 300*animation.getValue()),105,96, black);
        //Fonts.INTER.drawString(Main.getAppVersion(), (float) (35 - 300*animation.getValue()),218,14, black);
        renderer.drawRect((float) (35 - 300*animation.getValue()),211,80,5, mint);

        renderer.drawRound((float) (445 + (animation.getValue() * 130)), (float) (245 + (animation.getValue() * 130)),130,130, 65, new Color(106,212,67));
        renderer.drawRound((float) (375 + (animation.getValue() * 130)), (float) (300 + (animation.getValue() * 130)),130,130, 65, new Color(106,212,67));

        if(timer.finished(0)) {
            animation.run(1.0);
        }

        if (animation.isFinished()) {
            Main.getSceneManager().setCurrentScene(new ProjectScene());
        }
    }

    @Override
    public String getName() {
        return "Starting up...";
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int mouseButton) {

    }

    @Override
    public void onMouseReleased() {

    }

    @Override
    public void onKeyPressed(int keyCode) {

    }

    @Override
    public void onKeyReleased(int keyCode) {

    }

    private void load() {

    }
}
