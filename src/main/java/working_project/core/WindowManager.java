package working_project.core;

import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwFocusWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL20.GL_SHADING_LANGUAGE_VERSION;

public class WindowManager {
    private long window;
    private int width;
    private int height;

    public WindowManager(int width, int height, String title) {
        this.width = width;
        this.height = height;

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);

        window = glfwCreateWindow(width, height, title, 0, 0);
        if (window == 0) {
            glfwTerminate();
            throw new RuntimeException("Failed to create GLFW window");
        }

        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            int xPos = (vidMode.width() - width) / 2;
            int yPos = (vidMode.height() - height) / 2;
            glfwSetWindowPos(window, xPos, yPos);
            System.out.println("Setting window position to (" + xPos + ", " + yPos + ")");
        }

        glfwSetWindowSize(window, width, height);
        glfwShowWindow(window);
        glfwFocusWindow(window);
        glfwMakeContextCurrent(window);
        GL.createCapabilities();

        System.out.println("OpenGL Version: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL20.GL_VERSION));
        System.out.println("Renderer: " + org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL20.GL_RENDERER));
        System.out.println("GLSL Version: " + org.lwjgl.opengl.GL11.glGetString(GL_SHADING_LANGUAGE_VERSION));

        org.lwjgl.opengl.GL11.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_BLEND);
        org.lwjgl.opengl.GL11.glViewport(0, 0, width, height);

        int[] x = new int[1], y = new int[1];
        glfwGetWindowPos(window, x, y);
        System.out.println("Window position after setup: (" + x[0] + ", " + y[0] + ")");
        int[] w = new int[1], h = new int[1];
        glfwGetWindowSize(window, w, h);
        System.out.println("Window size after setup: (" + w[0] + "x" + h[0] + ")");

        glfwSetFramebufferSizeCallback(window, (win, wSize, hSize) -> {
            this.width = wSize;
            this.height = hSize;
            org.lwjgl.opengl.GL20.glViewport(0, 0, wSize, hSize);
            System.out.println("Framebuffer resized to: " + wSize + "x" + hSize);
        });
    }

    public void setKeyCallback(GLFWKeyCallbackI callback) {
        glfwSetKeyCallback(window, callback);
    }

    public void setCursorPosCallback(GLFWCursorPosCallbackI callback) {
        glfwSetCursorPosCallback(window, callback);
    }

    public void setScrollCallback(GLFWScrollCallbackI callback) {
        glfwSetScrollCallback(window, callback);
    }

    public void setMouseButtonCallback(GLFWMouseButtonCallbackI callback) {
        glfwSetMouseButtonCallback(window, callback);
    }

    public long getWindow() {
        return window;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void cleanup() {
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(window);
    }

    public void swapBuffers() {
        glfwSwapBuffers(window);
    }

    public void pollEvents() {
        glfwPollEvents();
    }
}
