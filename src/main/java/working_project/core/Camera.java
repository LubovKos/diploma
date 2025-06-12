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
        this.yaw = -90.0f; // Смотрим вдоль отрицательной оси X (на начало координат)
        this.pitch = 0.0f; // Горизонтально
        this.isOrthographic = false;
    }

    public void move(float x, float y, float z) {
        position.add(x, y, z);
    }

    public void rotate(float xOffset, float yOffset, float sensitivity) {
        xOffset *= sensitivity;
        yOffset *= sensitivity;
        yaw += xOffset;
        pitch += yOffset;
        pitch = Math.max(-89.0f, Math.min(89.0f, pitch));
    }


    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition);
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
        System.out.println("Projection: " + (isOrthographic ? "orthographic" : "perspective"));
    }
}
