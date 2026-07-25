package caprou.app.impl.render.display;


import caprou.app.impl.render.SimpleRenderer;
import caprou.app.impl.render.animation.Animation;
import caprou.app.impl.render.animation.Easing;
import caprou.app.impl.render.font.renderer.FontManager;
import caprou.app.impl.render.font.renderer.Fonts;
import caprou.app.impl.render.shader.ShaderManager;
import lombok.Getter;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.nio.IntBuffer;

import static java.lang.System.exit;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

// https://www.lwjgl.org/guide
public class Display {
    @Getter private long window;
    @Getter private int width, height;
    @Getter private String title;

    //framerate
    private final boolean vsync = false;
    private double lastTime;
    private int frames;
    @Getter
    private double fps;

    //tick
    final double TICKS = 10.0;
    final double tickRate = 1.0 / TICKS;
    double lastTick = glfwGetTime();

    private final SimpleRenderer renderer = SimpleRenderer.getInstance();

    private Animation animation = new Animation(Easing.EASE_IN_OUT_BACK, 1500);


    public Display(final String title, final int width, final int height) {
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    public void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if(!glfwInit()) // ça initialise ici, le bool return si ça sest bien passé c tt
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, title, NULL, NULL);

        if (window == NULL)
            glfwSetWindowShouldClose(window, true);


        // resize event
        glfwSetWindowSizeCallback(window, (window, width, height) -> {
            this.width = width;
            this.height = height;
            OrthographicProjection.updateProjection(width, height);
        });


        try (MemoryStack stack = stackPush()) {
            final IntBuffer pWidth = stack.mallocInt(1);
            final IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);

            final GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode == null) throw new RuntimeException("Video mode failed to initialize.");
            glfwSetWindowPos(window,
                    (vidMode.width() - pWidth.get(0)) / 2,
                    (vidMode.height() - pHeight.get(0)) / 2);
        }

        glfwMakeContextCurrent(window); // passe le context => window
        glfwSwapInterval(vsync ? 1 : 0);
        glfwShowWindow(window);

        GL.createCapabilities(); // le main context opengl

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        ShaderManager.compileShaders();
        OrthographicProjection.updateProjection(width, height);
        renderer.init();

        FontManager.initAll();


        setWindowTitle(title);
        lastTime = glfwGetTime();
    }


    private void loop() {
        while(!glfwWindowShouldClose(window)) {
            glViewport(0,0,width,height);

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearColor(0.0f,0.0f,0.0f,1.0f); // black, 255 alpha

            final double[] xpos = new double[1];
            final double[] ypos = new double[1];
            glfwGetCursorPos(window, xpos, ypos); // Pass mouseX -> xpos --- mouseY -> ypos

            //HOOK pour le render ici

            FontManager.beginFrame();

            animation.loop(0,1);

            renderer.drawRect(10,10,100,30,new Color(255,0,0));
            Fonts.INTER.animateSize(30 + (float) animation.getValue() * 10).drawString("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012345678910àé&'(-è_çàà)=$ù*!:", 10, 10, -1);

            //FIN DU HOOK

            double currentTime = glfwGetTime();
            while (currentTime - lastTick >= tickRate) {

                //ontick event
                lastTick += tickRate;
            }

            frames++;
            if (currentTime - lastTime >= 1.0) {
                fps = frames / (currentTime - lastTime); //Update fps counter every sec
                frames = 0;
                lastTime = currentTime;
                System.out.println("" + getFps());
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void cleanup() {
        ShaderManager.deleteShaders();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
        exit(0);
    }


    public void setWindowTitle(final String title) {
        if (window != NULL) {
            this.title = title;
            glfwSetWindowTitle(window, title);
        }
    }


}
