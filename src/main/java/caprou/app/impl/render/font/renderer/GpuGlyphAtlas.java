package caprou.app.impl.render.font.renderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import lombok.Getter;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

public final class GpuGlyphAtlas {

    private static final int OUTER_SPACING = 1;

    @Getter
    private final int width, height;

    @Getter
    private final int textureId;
    @Getter
    private final int framebufferId;

    private int cursorX = OUTER_SPACING;
    private int cursorY = OUTER_SPACING;
    private int rowHeight;
    private boolean linearFiltering;

    public GpuGlyphAtlas(int width, int height) {
        this.width = width;
        this.height = height;

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA16F,
                width,
                height,
                0,
                GL_RGBA,
                GL_FLOAT,
                0L
        );
        glBindTexture(GL_TEXTURE_2D, 0);

        framebufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                textureId,
                0
        );

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer de l'atlas incomplet : 0x" + Integer.toHexString(status));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        clear();
    }

    public AtlasRegion allocate(int requestedWidth, int requestedHeight) {
        if (requestedWidth <= 0 || requestedHeight <= 0) {
            throw new IllegalArgumentException("Taille de glyphe invalide.");
        }
        if (requestedWidth + OUTER_SPACING * 2 > width
                || requestedHeight + OUTER_SPACING * 2 > height) {
            throw new IllegalArgumentException(
                    "Le glyphe " + requestedWidth + "x" + requestedHeight
                            + " ne tient pas dans l'atlas " + width + "x" + height
            );
        }

        if (cursorX + requestedWidth + OUTER_SPACING > width) {
            cursorX = OUTER_SPACING;
            cursorY += rowHeight + OUTER_SPACING;
            rowHeight = 0;
        }

        if (cursorY + requestedHeight + OUTER_SPACING > height) {
            return null;
        }

        AtlasRegion region = new AtlasRegion(cursorX, cursorY, requestedWidth, requestedHeight);
        cursorX += requestedWidth + OUTER_SPACING;
        rowHeight = Math.max(rowHeight, requestedHeight);
        return region;
    }

    public void clear() {
        int previousFramebuffer = glGetInteger(GL_FRAMEBUFFER_BINDING);
        boolean scissorWasEnabled = glIsEnabled(GL_SCISSOR_TEST);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer viewport = stack.mallocInt(4);
            IntBuffer scissorBox = stack.mallocInt(4);
            FloatBuffer clearColor = stack.mallocFloat(4);

            glGetIntegerv(GL_VIEWPORT, viewport);
            glGetIntegerv(GL_SCISSOR_BOX, scissorBox);
            glGetFloatv(GL_COLOR_CLEAR_VALUE, clearColor);

            glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
            glViewport(0, 0, width, height);
            glDisable(GL_SCISSOR_TEST);
            glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            glBindFramebuffer(GL_FRAMEBUFFER, previousFramebuffer);
            glViewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
            glScissor(scissorBox.get(0), scissorBox.get(1), scissorBox.get(2), scissorBox.get(3));
            glClearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));

            if (scissorWasEnabled) glEnable(GL_SCISSOR_TEST);
            else glDisable(GL_SCISSOR_TEST);
        }

        cursorX = OUTER_SPACING;
        cursorY = OUTER_SPACING;
        rowHeight = 0;
    }


    public void setLinearFiltering(boolean linear) {
        if (linearFiltering == linear) return;

        final int previousActiveTexture = glGetInteger(GL_ACTIVE_TEXTURE);
        glActiveTexture(GL_TEXTURE0);
        final int previousTexture = glGetInteger(GL_TEXTURE_BINDING_2D);

        glBindTexture(GL_TEXTURE_2D, textureId);
        final int filter = linear ? GL_LINEAR : GL_NEAREST;
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);

        glBindTexture(GL_TEXTURE_2D, previousTexture);
        glActiveTexture(previousActiveTexture);
        linearFiltering = linear;
    }

    public void bindFramebuffer() {
        glBindFramebuffer(GL_FRAMEBUFFER, framebufferId);
    }

    public void bindTexture(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void delete() {
        glDeleteFramebuffers(framebufferId);
        glDeleteTextures(textureId);
    }
}