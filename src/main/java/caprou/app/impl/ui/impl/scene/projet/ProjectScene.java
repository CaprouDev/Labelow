package caprou.app.impl.ui.impl.scene.projet;

import caprou.app.Main;
import caprou.app.impl.interfaces.Consts;
import caprou.app.impl.render.animation.Animation;
import caprou.app.impl.render.animation.Easing;
import caprou.app.impl.render.font.renderer.Fonts;
import caprou.app.impl.ui.Scene;
import caprou.app.impl.ui.impl.input.textinput.TextInput;
import caprou.app.impl.ui.impl.scene.projet.impl.ProjectBanner;
import caprou.app.impl.ui.impl.scene.projet.impl.ProjectButton;

import java.awt.*;

public class ProjectScene extends Scene implements Consts {

    private final ProjectButton projectButton = new ProjectButton(() -> { System.out.println("clicked !"); });
    private final ProjectBanner projectBanner = new ProjectBanner();

    private final Animation widthAnimation = new Animation(Easing.EASE_OUT_EXPO, 500);

    private final TextInput textInput = new TextInput("Blablabla",2,440, 336, 400);

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

        projectButton.init();
        widthAnimation.setValue(display.getWidth());
    }

    @Override
    public void stop() {
        projectButton.destroy();
    }

    @Override
    public void render(int mouseX, int mouseY) {
        widthAnimation.run(display.getWidth());
        renderer.drawRect(0,0, display.getWidth(), display.getHeight(), Color.WHITE);

        //header
        final float headerMargin = 32;
        final float nameSize = Fonts.LORA_BOLD.measureWidth(Main.getAppName(), 20);
        final float versionSize = Fonts.INTER.measureWidth(Main.getAppVersion(),12);

        Fonts.LORA_BOLD.drawString(Main.getAppName(),32,21,20, Color.BLACK);
        renderer.drawRound(headerMargin + nameSize + 8, 16 + 8, 16 + versionSize, 20, 12, new Color(240,240,240));
        Fonts.INTER.drawString(Main.getAppVersion(),32 + nameSize + 8 + 8,16 + 10,12, new Color(136,136,136));
        projectButton.draw((float) (widthAnimation.getValue() - headerMargin), 16, mouseX, mouseY);


        // Banner
        final float projectMargin = 24;
        projectBanner.draw(projectMargin,0,68, (float) widthAnimation.getValue(), mouseX,mouseY);

        // Cards
        renderer.drawRound(projectMargin, 16 + 30 + 22 + 200 + 32, 295,360,25, new Color(15,15,15));
        Fonts.LORA_BOLD.drawString("Urban Traffic",24 + 20,16 + 30 + 22 + 200 + 32 + 275,18, Color.WHITE);
        Fonts.INTER.drawString("Detection of vehicles and pedest...",24 + 20,575 + 25,12, new Color(255,255,255,100));

        textInput.draw(414 + 28,336,mouseX,mouseY);
    }

    @Override
    public String getName() {
        return "Project scene";
    }

    @Override
    public void onMouseClicked(int mouseX, int mouseY, int mouseButton) {
        textInput.mouseClicked(mouseX,mouseY,mouseButton);
        projectButton.mouseClicked(mouseX,mouseY,mouseButton);
    }

    @Override
    public void onMouseReleased() {
        textInput.mouseReleased();
    }

    @Override
    public void onKeyPressed(int keyCode) {
        textInput.keyTyped(keyCode);
    }

    @Override
    public void onKeyReleased(int keyCode) {
        textInput.onKeyReleased(keyCode);
    }

    @Override
    public void onChar(char c) {
        textInput.onChar(c);
    }

}
