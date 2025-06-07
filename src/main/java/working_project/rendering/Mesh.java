package working_project.rendering;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class Mesh {
    private int vao, vbo, ebo, indexCount;

    public Mesh(float[] vertices, int[] indices) {
        vao = glGenVertexArrays();
        checkGLError("glGenVertexArrays");

        glBindVertexArray(vao);
        checkGLError("glBindVertexArray");

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexData = BufferUtils.createFloatBuffer(vertices.length);
        vertexData.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);
        checkGLError("glBufferData for VBO");

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * 4, 0);
        checkGLError("glVertexAttribPointer for position");
        glEnableVertexAttribArray(0);
        checkGLError("glEnableVertexAttribArray for position");

        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * 4, 3 * 4);
        checkGLError("glVertexAttribPointer for color/normal");
        glEnableVertexAttribArray(1);
        checkGLError("glEnableVertexAttribArray for color/normal");

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexData = BufferUtils.createIntBuffer(indices.length);
        indexData.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);
        checkGLError("glBufferData for EBO");

        indexCount = indices.length;
        glBindVertexArray(0);
        checkGLError("glBindVertexArray (unbind)");
    }

    public void render() {
        render(GL_TRIANGLES);
    }

    public void render(int mode) {
        glBindVertexArray(vao);
        checkGLError("glBindVertexArray before draw");
        glDrawElements(mode, indexCount, GL_UNSIGNED_INT, 0);
        checkGLError("glDrawElements");
        glBindVertexArray(0);
        checkGLError("glBindVertexArray (unbind after draw)");
    }

    public void cleanup() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
    }

    public int getVao() {
        return vao;
    }

    private void checkGLError(String stage) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.err.println("OpenGL Error at " + stage + ": " + error);
        }
    }
}
