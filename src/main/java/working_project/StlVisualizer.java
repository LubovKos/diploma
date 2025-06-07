package working_project;

import working_project.core.Camera;
import working_project.core.WindowManager;
import working_project.gui.ImGuiManager;
import working_project.io.FileDialogHandler;
import working_project.io.InputHandler;
import working_project.model.ModelLoader;
import working_project.model.ModelManager;
import working_project.rendering.Point3D;
import working_project.rendering.Renderer;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.lwjgl.opengl.GL11.*;

public class StlVisualizer {


    private static final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );

    public static void main(String[] args) {
        System.out.println("Starting StlVisualizer...");
        WindowManager window = new WindowManager(1920, 1080, "3D Editor");
        System.out.println("WindowManager initialized");

        ImGuiManager imgui = new ImGuiManager(window);
        System.out.println("ImGuiManager initialized");
        Camera camera = new Camera(new Vector3f(0, 0, 5));
        System.out.println("Camera initialized");
        Renderer renderer = new Renderer();
        System.out.println("Renderer initialized");
        ModelLoader loader = new ModelLoader();
        System.out.println("ModelLoader initialized");

        ModelManager modelManager = new ModelManager(loader, camera, renderer);
        boolean[] onlyPointsMode = {false};
        // лассо: Добавляем флаг lassoMode
        boolean[] lassoMode = {false};
        // лассо: Передаём lassoMode в InputHandler
        InputHandler inputHandler = new InputHandler(window, camera, modelManager, lassoMode);
        FileDialogHandler dialogHandler = new FileDialogHandler();

        List<ModelLoader.Chunk> chunks = new ArrayList<>();
        List<Point3D> points = new ArrayList<>();
        boolean[] isModelLoaded = {false};
        boolean[] isPointCloud = {false};
        boolean[] isRendering = {false};

        System.out.println("Starting main loop...");
        while (!window.shouldClose()) {
            window.pollEvents();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glClearColor(0.15f, 0.15f, 0.15f, 1.0f); // Тёмный фон для сцены

            imgui.newFrame();
            // лассо: Передаём lassoMode и inputHandler в renderUI
            imgui.renderUI(modelManager, dialogHandler, chunks, points, isModelLoaded, isPointCloud, isRendering, lassoMode, inputHandler, onlyPointsMode);


            if (isRendering[0] && isModelLoaded[0]) {
                float[] objectColor = modelManager.getObjectColor();
                Vector3f objectCenter = modelManager.getObjectCenter(); // <--- Добавил получение центра

                if (isPointCloud[0]) {
                    renderer.renderPoints(window, camera, points, modelManager.getModelYaw(), modelManager.getModelPitch(), objectCenter);
                } else {
                    renderer.render(window, camera, chunks, modelManager.getModelYaw(), modelManager.getModelPitch(), objectColor);
                }
            }

            imgui.renderDrawData();
            window.swapBuffers();
        }

        System.out.println("Cleaning up...");
        for (ModelLoader.Chunk chunk : chunks) {
            chunk.cleanup();
        }
        modelManager.cleanup();
        renderer.cleanup();
        imgui.cleanup();
        window.cleanup();

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}