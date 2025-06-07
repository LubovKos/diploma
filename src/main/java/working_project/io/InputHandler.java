package working_project.io;

import imgui.ImGui;
import org.joml.Vector2f;
import working_project.model.ModelManager;
import working_project.core.Camera;
import working_project.core.WindowManager;
import org.joml.Vector3f;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.*;

public class InputHandler {
    private final AtomicBoolean leftMouseButtonPressed = new AtomicBoolean(false);
    private final AtomicBoolean rightMouseButtonPressed = new AtomicBoolean(false);
    private final double[] lastX = {0};
    private final double[] lastY = {0};
    private final boolean[] firstMouseMovement = {true};
    private final Vector3f objectCenter = new Vector3f(0, 0, 0);

    public InputHandler(WindowManager window, Camera camera, ModelManager modelManager, boolean[] lassoMode) {
        lastX[0] = window.getWidth() / 2.0;
        lastY[0] = window.getHeight() / 2.0;

        window.setKeyCallback((windowHandle, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                float speed = 0.1f;
                float rotationSpeed = 2.0f;

                Vector3f forward = camera.getDirection();
                Vector3f up = new Vector3f(0, 1, 0);
                Vector3f right = forward.cross(up).normalize();

                if (key == GLFW_KEY_W) camera.move(0, speed, 0);
                if (key == GLFW_KEY_S) camera.move(0, -speed, 0);
                if (key == GLFW_KEY_Q) {
                    Vector3f moveDir = new Vector3f(forward).mul(speed);
                    camera.move(moveDir.x, 0, moveDir.z);
                }
                if (key == GLFW_KEY_E) {
                    Vector3f moveDir = new Vector3f(forward).mul(-speed);
                    camera.move(moveDir.x, 0, moveDir.z);
                }
                if (key == GLFW_KEY_A) {
                    Vector3f moveDir = new Vector3f(right).mul(-speed);
                    camera.move(moveDir.x, 0, moveDir.z);
                }
                if (key == GLFW_KEY_D) {
                    Vector3f moveDir = new Vector3f(right).mul(speed);
                    camera.move(moveDir.x, 0, moveDir.z);
                }

                // лассо: Отключить вращение модели в режиме лассо
                if (!lassoMode[0]) {
                    if (key == GLFW_KEY_LEFT) modelManager.updateModelYaw(-rotationSpeed);
                    if (key == GLFW_KEY_RIGHT) modelManager.updateModelYaw(rotationSpeed);
                    if (key == GLFW_KEY_UP) modelManager.updateModelPitch(rotationSpeed);
                    if (key == GLFW_KEY_DOWN) modelManager.updateModelPitch(-rotationSpeed);
                    modelManager.clampModelPitch();
                }

                System.out.println("Camera pos: " + camera.getPosition() + ", Camera yaw: " + camera.getYaw() +
                        ", Camera pitch: " + camera.getPitch() + ", Model yaw: " + modelManager.getModelYaw() +
                        ", Model pitch: " + modelManager.getModelPitch());
            }
        });

        window.setCursorPosCallback((windowHandle, xpos, ypos) -> {
            if (windowHandle == window.getWindow()) {
                if (!lassoMode[0] && leftMouseButtonPressed.get() && !ImGui.getIO().getWantCaptureMouse()) {
                    if (firstMouseMovement[0]) {
                        lastX[0] = xpos;
                        lastY[0] = ypos;
                        firstMouseMovement[0] = false;
                        return;
                    }

                    float mouseSensitivity = 0.05f;
                    float deltaX = (float) (xpos - lastX[0]);
                    float deltaY = (float) (lastY[0] - ypos);

                    modelManager.updateModelYaw(deltaX * mouseSensitivity);
                    modelManager.updateModelPitch(-deltaY * mouseSensitivity);

                    System.out.printf("Model: yaw=%.2f, pitch=%.2f\n", modelManager.getModelYaw(), modelManager.getModelPitch());
                }

                if (!lassoMode[0] && rightMouseButtonPressed.get() && !ImGui.getIO().getWantCaptureMouse()) {
                    if (firstMouseMovement[0]) {
                        lastX[0] = xpos;
                        lastY[0] = ypos;
                        firstMouseMovement[0] = false;
                        return;
                    }

                    float cameraSensitivity = 0.005f;
                    float deltaX = (float) (xpos - lastX[0]);
                    float deltaY = (float) (lastY[0] - ypos);

                    camera.rotate(deltaX, deltaY, cameraSensitivity);

                    System.out.printf("Camera: yaw=%.2f, pitch=%.2f\n", camera.getYaw(), camera.getPitch());
                }

                lastX[0] = xpos;
                lastY[0] = ypos;
            }
        });

        window.setMouseButtonCallback((windowHandle, button, action, mods) -> {
            if (windowHandle == window.getWindow()) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) {
                    if (action == GLFW_PRESS) {
                        leftMouseButtonPressed.set(true);
                        firstMouseMovement[0] = true;
                        System.out.println("Left mouse button pressed, model rotation enabled");
                    } else if (action == GLFW_RELEASE) {
                        leftMouseButtonPressed.set(false);
                        System.out.println("Left mouse button released, model rotation disabled");
                    }
                }
                if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                    if (action == GLFW_PRESS) {
                        rightMouseButtonPressed.set(true);
                        firstMouseMovement[0] = true;
                        // лассо: Логирование в зависимости от режима
                        System.out.println("Right mouse button pressed, " + (lassoMode[0] ? "lasso mode" : "camera rotation") + " enabled");
                    } else if (action == GLFW_RELEASE) {
                        rightMouseButtonPressed.set(false);
                        // лассо: Логирование в зависимости от режима
                        System.out.println("Right mouse button released, " + (lassoMode[0] ? "lasso mode" : "camera rotation") + " disabled");
                    }
                }
            }
        });

        window.setScrollCallback((windowHandle, xoffset, yoffset) -> {
            if (windowHandle == window.getWindow() && !ImGui.getIO().getWantCaptureMouse()) {
                float zoomSpeed = 0.1f;
                Vector3f cameraPos = camera.getPosition();

                Vector3f directionToCenter = new Vector3f(modelManager.getObjectCenter()).sub(cameraPos).normalize();
                float distanceToCenter = cameraPos.distance(modelManager.getObjectCenter());
                float zoomAmount = (float) yoffset * zoomSpeed * distanceToCenter;
                distanceToCenter -= zoomAmount;

                distanceToCenter = Math.max(0.1f, Math.min(100.0f, distanceToCenter));
                Vector3f newCameraPos = new Vector3f(modelManager.getObjectCenter()).sub(directionToCenter.mul(distanceToCenter));
                camera.setPosition(newCameraPos);

                System.out.println("Camera pos after zoom: " + camera.getPosition() + ", Distance to center: " + distanceToCenter);
            }
        });
    }
    // лассо: Возвращает состояние правой кнопки мыши
    public boolean isRightMouseButtonPressed() {
        return rightMouseButtonPressed.get();
    }

    // лассо: Возвращает текущие координаты курсора
    public Vector2f getCursorPosition() {
        return new Vector2f((float) lastX[0], (float) lastY[0]);
    }
}

