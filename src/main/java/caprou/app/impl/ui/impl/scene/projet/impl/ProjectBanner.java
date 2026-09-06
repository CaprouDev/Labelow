package caprou.app.impl.ui.impl.scene.projet.impl;

import caprou.app.impl.interfaces.Consts;

import java.awt.*;

public class ProjectBanner implements Consts {
    private final float height, radius;

    public ProjectBanner() {
        this.height = 200;
        this.radius = 25;
    }



    public void draw(float projectMargin, float x, float y, float screenWidth, int mouseX, int mouseY) {
        final float width = screenWidth - projectMargin*2;

        renderer.drawRound(x + projectMargin, y, width, height,radius, new Color(15,15,15));
    }



}
