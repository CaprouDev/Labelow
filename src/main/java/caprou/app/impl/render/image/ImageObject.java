package caprou.app.impl.render.image;

import caprou.app.impl.interfaces.Shaders;
import caprou.app.impl.render.SimpleRenderer;
import caprou.app.impl.render.display.Display;
import caprou.app.impl.util.file.FileUtil;
import caprou.app.impl.util.render.ColorUtil;
import lombok.Getter;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;

public class ImageObject implements Shaders {
    private final Texture texture;


    public ImageObject(final String imageName) {
        final InputStream inputStream = FileUtil.getInputStream("/images/" + imageName);
        this.texture = new Texture(inputStream, false);
    }

    public final void load() {
        this.texture.load();
    }


    public final void drawImg(float x, float y, float width, float height) {
        if(!texture.isLoaded())
            return;

        drawImg(x,y,width,height, new Color(255,255,255));
    }

    public final void drawImg(float x, float y) {
        if(!texture.isLoaded())
            return;

        final int width = texture.getWidth();
        final int height = texture.getHeight();
        drawImg(x,y,width,height, new Color(255,255,255));
    }

    public void drawImg(float x, float y, float width, float height, Color color) {
        if(!texture.isLoaded())
            return;

        SimpleRenderer simpleRenderer = SimpleRenderer.getInstance();
        simpleRenderer.drawImage(x, y, width, height, texture, color);
    }

    public void unload() {
        this.texture.unload();
    }
}