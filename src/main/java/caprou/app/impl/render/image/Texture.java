package caprou.app.impl.render.image;

import lombok.Getter;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;

public class Texture {
    private int textureID;
    @Getter private boolean loaded;
    private final boolean pixelated;
    @Getter private int width, height;
    private final InputStream inputStream;

    public Texture(final InputStream inputStream, final boolean pixelated) {
        this.inputStream = inputStream;
        this.pixelated = pixelated;
    }

    public void load() {
        BufferedImage img;
        try {
            img = ImageIO.read(inputStream);
            width = img.getWidth();
            height = img.getHeight();

            int[] pixels = new int[width * height];
            img.getRGB(0, 0, width, height, pixels, 0, width);

            ByteBuffer buffer = ByteBuffer.allocateDirect(4 * width * height);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = pixels[y * width + x];
                    buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                    buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                    buffer.put((byte) (pixel & 0xFF));         // Blue
                    buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
                }
            }

            buffer.flip();

            textureID = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureID);


            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            if(!pixelated) {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            }else {
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            }

            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);

            if(!pixelated) GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            if (Objects.requireNonNull(GL11.glGetString(GL11.GL_EXTENSIONS)).contains("GL_EXT_texture_filter_anisotropic")) {
                float maxAnisotropy = GL11.glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
                GL11.glTexParameterf(GL11.GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, maxAnisotropy);
            }

            loaded = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public final void unload() {
        if (!loaded)
            return;

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDeleteTextures(textureID);
        loaded = false;
    }
}
