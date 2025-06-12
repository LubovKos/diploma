package working_project.gui;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImGuiStyle;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import org.joml.Vector2f;
import working_project.model.ModelManager;
import working_project.core.WindowManager;
import working_project.io.FileDialogHandler;
import working_project.io.InputHandler;
import working_project.model.ModelLoader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import working_project.rendering.Point3D;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

public class ImGuiManager {
    private final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private int fontTextureId;
    private int dockspaceId;

    private final ImFloat voxelSize = new ImFloat(0.1f);
    private final ImFloat isoLevel = new ImFloat(10.0f);


    public ImGuiManager(WindowManager window) {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.addConfigFlags(ImGuiConfigFlags.DockingEnable);
        io.addConfigFlags(ImGuiConfigFlags.ViewportsEnable);
        imGuiGlfw.init(window.getWindow(), true);
        imGuiGl3.init("#version 410 core");

        // Настройка шрифта
        ImFontAtlas fontAtlas = io.getFonts();
        ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setGlyphRanges(fontAtlas.getGlyphRangesCyrillic());
        fontConfig.setSizePixels(18.0f);

        // Загрузка шрифта
        String fontPath = "Quicksand-Regular.ttf";
        try {
            ImFont font = fontAtlas.addFontFromFileTTF(fontPath, 18.0f, fontConfig);
            if (font != null) {
                System.out.println("The font has been uploaded successfully.");
                io.setFontDefault(font);
            } else {
                System.err.println("Mistake: couldn't load font.");
                fontAtlas.addFontDefault();
            }
        } catch (Exception e) {
            System.err.println("Exception when downloading a font texture: " + e.getMessage());
            fontAtlas.addFontDefault();
        } finally {
            fontConfig.destroy();
        }

        // Построение текстурного атласа шрифтов
        try {
            ImInt width = new ImInt();
            ImInt height = new ImInt();
            ByteBuffer pixels = fontAtlas.getTexDataAsRGBA32(width, height);
            if (pixels != null && width.get() > 0 && height.get() > 0) {
                fontTextureId = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, fontTextureId);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(), height.get(), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
                glBindTexture(GL_TEXTURE_2D, 0);

                fontAtlas.setTexID(fontTextureId);
                fontAtlas.clearTexData();
                System.out.println("The font texture has been created: ID=" + fontTextureId + ", width=" + width.get() + ", height=" + height.get());
            } else {
                System.err.println("Mistake: couldn't create a texture atlas of fonts.");
            }
        } catch (Exception e) {
            System.err.println("Exception when creating a font texture: " + e.getMessage());
        }

        // Настройка стиля
        ImGuiStyle style = ImGui.getStyle();
        style.setColor(ImGuiCol.WindowBg, 0.1f, 0.1f, 0.13f, 1.0f);
        style.setColor(ImGuiCol.TitleBg, 0.2f, 0.2f, 0.25f, 1.0f);
        style.setColor(ImGuiCol.TitleBgActive, 0.3f, 0.3f, 0.35f, 1.0f);
        style.setColor(ImGuiCol.Button, 0.0f, 0.6f, 1.0f, 1.0f);
        style.setColor(ImGuiCol.ButtonHovered, 0.0f, 0.8f, 1.0f, 1.0f);
        style.setColor(ImGuiCol.ButtonActive, 0.0f, 0.5f, 0.9f, 1.0f);
        style.setColor(ImGuiCol.Text, 1.0f, 1.0f, 1.0f, 1.0f);
        style.setColor(ImGuiCol.FrameBg, 0.15f, 0.15f, 0.15f, 1.0f);
        style.setColor(ImGuiCol.Separator, 0.4f, 0.4f, 0.4f, 1.0f);
        style.setWindowRounding(8.0f);
        style.setFrameRounding(8.0f);
        style.setFramePadding(8.0f, 4.0f);
        style.setItemSpacing(10.0f, 8.0f);
        style.setWindowBorderSize(1.0f);
        style.setFrameBorderSize(1.0f);
    }

    public void newFrame() {
        imGuiGlfw.newFrame();
        ImGui.newFrame();
        dockspaceId = ImGui.dockSpaceOverViewport(ImGui.getMainViewport(), ImGuiDockNodeFlags.PassthruCentralNode);
    }

    public void renderUI(ModelManager modelManager, FileDialogHandler fileDialogHandler, List<ModelLoader.Chunk> chunks,
                         List<Point3D> points, boolean[] isModelLoaded, boolean[] isPointCloud, boolean[] isRendering,
                         boolean[] onlyPointsMode) {
        // Фиксируем позицию и размер панели
        ImGui.setNextWindowPos(0, 0, ImGuiCond.Always);
        ImGui.setNextWindowSize(300, ImGui.getMainViewport().getWorkSizeY(), ImGuiCond.Always);
        ImGui.setNextWindowDockID(dockspaceId, ImGuiCond.Always);

        // Делаем окно неизменяемым по размеру
        int windowFlags = ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoMove;
        ImGui.begin("3D Editor Control Panel", windowFlags);

        ImGui.text("Files");
        ImGui.pushStyleColor(ImGuiCol.Button, 0.0f, 0.5f, 1.0f, 1.0f);
        if (ImGui.button("Download file (STL, PLY, OBJ)", 280, 40)) {
            modelManager.loadModel(fileDialogHandler.openFileDialog(), chunks, points, isModelLoaded, isPointCloud, onlyPointsMode);
        }
        boolean loadOnlyPointsMode = onlyPointsMode[0];
        if (ImGui.checkbox("Load only points", loadOnlyPointsMode)) {
            onlyPointsMode[0] = !loadOnlyPointsMode;
            System.out.println("Points Mode changed to: " + onlyPointsMode[0]);
            if (onlyPointsMode[0]) {
                System.out.println("Points Mode enabled ");
            }
            else{
                System.out.println("Points Mode disabled ");
            }
        }

        if (modelManager.isExporting()) {
            ImGui.text("Exporting...");
            ImGui.text(modelManager.getExportStatus());
            ImGui.beginDisabled();
            ImGui.button("Export", 280, 40);
            ImGui.endDisabled();
        } else if (ImGui.button("Export", 280, 40)) {
            modelManager.exportModel(fileDialogHandler);
        }
        ImGui.popStyleColor();
        ImGui.spacing();
        ImGui.separator();

        ImGui.text("Model processing");
        ImGui.pushStyleColor(ImGuiCol.Button, 0.2f, 0.7f, 0.2f, 1.0f);
        ImGui.inputFloat("Voxel Size", voxelSize, 0.01f, 0.1f, "%.2f");
        if (voxelSize.get() <= 0) voxelSize.set(0.1f);
        ImGui.inputFloat("Iso Level", isoLevel, 0.1f, 1.0f, "%.1f");
        if (isoLevel.get() < 0) isoLevel.set(10.0f);
        if (ImGui.button("Marching cubes", 280, 40)) {
            modelManager.applyMarchingCubes(chunks, points, isPointCloud, isRendering, voxelSize.get(), isoLevel.get());
        }
        if (ImGui.button("Remove noise", 280, 40)) {
            modelManager.removeNoise(chunks, isPointCloud, isModelLoaded);
        }
        if (ImGui.button("Smooth model", 280, 40)) {
            modelManager.smoothModel(chunks);
        }
        if (ImGui.button("Smooth boundaries", 280, 40)){
            modelManager.smoothBoundaries(chunks, points, isPointCloud, isModelLoaded);
        }
        if (ImGui.button("Find the biggest object", 280, 40)) {
            modelManager.findLargestComponent(chunks, isPointCloud);
        }

        ImGui.popStyleColor();
        ImGui.spacing();
        ImGui.separator();

        ImGui.text("Rendering");
        ImGui.pushStyleColor(ImGuiCol.Button, 1.0f, 0.5f, 0.0f, 1.0f);
        if (ImGui.button("Drawing model", 280, 40)) {
            if (isModelLoaded[0]) {
                isRendering[0] = !isRendering[0];
                System.out.println("Rendering toggled: " + (isRendering[0] ? "ON" : "OFF"));
            } else {
                System.out.println("Cannot render: No model loaded.");
            }
        }
        ImGui.popStyleColor();
        ImGui.spacing();
        ImGui.separator();

        ImGui.text("Settings");
        float[] objectColor = modelManager.getObjectColor();
        if (ImGui.colorEdit3("Color", objectColor)) {
            modelManager.setObjectColor(objectColor[0], objectColor[1], objectColor[2]); // Применяем новый цвет
        }
        if (ImGui.button("Projection (P)", 280, 40)) {
            modelManager.toggleCameraProjection();
        }

        ImGui.spacing();
        ImGui.separator();

        ImGui.text("Interaction with the stage");
        ImGui.text("WASD - motion control\nScale - mouse wheel\nRotate the model - left mouse button\nRotate the camera - right mouse button");
        ImGui.end();
    }

    public void renderDrawData() {
        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
        if (ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable)) {
            final long backupWindowPtr = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
            ImGui.updatePlatformWindows();
            ImGui.renderPlatformWindowsDefault();
            org.lwjgl.glfw.GLFW.glfwMakeContextCurrent(backupWindowPtr);
        }
    }

    public void cleanup() {
        if (fontTextureId != 0) {
            glDeleteTextures(fontTextureId);
            System.out.println("Font texture deleted: ID=" + fontTextureId);
        }
        imGuiGl3.dispose();
        imGuiGlfw.dispose();
        ImGui.destroyContext();
    }
}
