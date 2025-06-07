package working_project.core;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private Vector3f position;
    private float yaw;
    private float pitch;
    private boolean isOrthographic;
    private float fov = 45.0f; // Угол обзора для перспективной проекции
    private float orthoSize = 10.0f; // Размер ортографической проекции

    public Camera(Vector3f initialPosition) {
        this.position = new Vector3f(initialPosition);
        this.yaw = -90.0f;
        this.pitch = 0.0f;
        this.isOrthographic = false;
    }

    public void move(float x, float y, float z) {
        Vector3f direction = getDirection();
        Vector3f right = direction.cross(new Vector3f(0, 1, 0)).normalize();
        Vector3f up = right.cross(direction).normalize();
        position.add(right.mul(x)).add(up.mul(y)).add(direction.mul(z));
    }

    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition);
    }

    public void rotate(float xOffset, float yOffset, float sensitivity) {
        xOffset *= sensitivity;
        yOffset *= sensitivity;
        yaw += xOffset;
        pitch += yOffset; // Инвертируем Y для естественного управления
        pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
    }

    public void resetRotation() {
        yaw = -90.0f;
        pitch = 0.0f;
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public Vector3f getDirection() {
        return new Vector3f(
                (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))),
                (float) Math.sin(Math.toRadians(pitch)),
                (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)))
        ).normalize();
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public Matrix4f getViewMatrix() {
        Vector3f direction = getDirection();
        Vector3f up = new Vector3f(0, 1, 0);
        return new Matrix4f().lookAt(position, position.add(direction, new Vector3f()), up);
    }

    public Matrix4f getProjectionMatrix(float aspectRatio, float near, float far) {
        if (isOrthographic) {
            float halfWidth = orthoSize * aspectRatio;
            float halfHeight = orthoSize;
            return new Matrix4f().ortho(-halfWidth, halfWidth, -halfHeight, halfHeight, near, far);
        } else {
            return new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, near, far);
        }
    }

    public void toggleProjection() {
        isOrthographic = !isOrthographic;
        System.out.println("Проекция переключена на: " + (isOrthographic ? "ортографическая" : "перспективная"));
    }

    public boolean isOrthographic() {
        return isOrthographic;
    }

    // Методы для настройки параметров проекции
    public void setFov(float fov) {
        this.fov = Math.max(30.0f, Math.min(90.0f, fov));
    }

    public float getFov() {
        return fov;
    }

    public void setOrthoSize(float size) {
        this.orthoSize = Math.max(1.0f, size);
    }

    public float getOrthoSize() {
        return orthoSize;
    }
}