package caprou.app.impl.ui.impl.scene.projet.impl;

import caprou.app.impl.interfaces.Consts;
import caprou.app.impl.render.animation.Animation;
import caprou.app.impl.render.animation.Easing;
import caprou.app.impl.render.font.renderer.Fonts;
import caprou.app.impl.render.image.ImageObject;
import caprou.app.impl.render.image.Texture;
import caprou.app.impl.util.mouse.MouseUtil;

import java.awt.*;

public class ProjectButton implements Consts {

    private final String text = "New project";
    private final ImageObject plusIcon = new ImageObject("add.png", true);
    private final float textSize;
    private float x, y;
    private final float width, height, radius;
    private final Runnable action;
    private final Animation hoverAnim = new Animation(Easing.EASE_OUT_BACK, 300);

    public ProjectButton(Runnable runnable) {
        this.action = runnable;
        this.textSize = Fonts.INTER_BOLD.measureWidth(text, 14);
        this.width = textSize + 52;
        this.height = 36;
        this.radius = 18;
    }


    public void init() {
        plusIcon.load();
    }


    public void destroy() {
        plusIcon.unload();
    }


    public void draw(float x, float y, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        final boolean hovered = MouseUtil.isHovering(mouseX, mouseY, x - width, y, width, height);

        hoverAnim.run(hovered ? 1 : 0);


        renderer.drawRound((float) (x - width - hoverAnim.getValue() * 5), (float) (y - hoverAnim.getValue() * 1), (float) (width + hoverAnim.getValue() * 5 * 2), (float) (height + hoverAnim.getValue() * 2), (float) (radius + hoverAnim.getValue()*2), new Color(62, 207, 74));
        plusIcon.drawImg((float) (x - width +16 - hoverAnim.getValue() * 5),y+11,14,14);
        Fonts.INTER_BOLD.animateSize((float) (14 + hoverAnim.getValue() * 1)).drawString(text, (float) (x - textSize - 16 - hoverAnim.getValue()), (float) (y + 9 - hoverAnim.getValue() * 0.5f), Color.WHITE);
    }


    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        final boolean hovered = MouseUtil.isHovering(mouseX, mouseY, x - width, y, width, height);

        if(!hovered)
            return;

        if(mouseButton == 0) {
            action.run();
        }

    }
    /*float buttonTextSize = Fonts.INTER_BOLD.measureWidth("New project", 14);
        renderer.drawRound(25 + 1230 - 140 - 5, 16, buttonTextSize + 53,36,18, new Color(52, 213, 93));
        Fonts.INTER_BOLD.drawString("New project", 56 + 1230 - 140, 16 + 9, 14, Color.WHITE);*/
}
