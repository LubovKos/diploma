package working_project.core;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgramiv;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderiv;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

public class ShaderProgram {
    private int program;

    public ShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        checkShaderCompileError(vertexShader, "Vertex Shader");

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);
        checkShaderCompileError(fragmentShader, "Fragment Shader");

        program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        checkProgramLinkError(program, "Shader Program");

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    public void use() {
        glUseProgram(program);
    }

    public void setUniformMatrix4fv(String name, FloatBuffer matrix) {
        glUniformMatrix4fv(glGetUniformLocation(program, name), false, matrix);
    }

    public void setUniform1f(String name, float value) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location == -1) {
            System.err.println("Uniform " + name + " not found in the shader");
        }
        GL20.glUniform1f(location, value);
    }

    public void setUniform3f(String name, float x, float y, float z) {
        int location = GL20.glGetUniformLocation(program, name);
        if (location == -1) {
            System.err.println("Uniform " + name + " not found in the shader");
        }
        GL20.glUniform3f(location, x, y, z);
    }

    private void checkShaderCompileError(int shader, String shaderName) {
        IntBuffer success = BufferUtils.createIntBuffer(1);
        glGetShaderiv(shader, GL_COMPILE_STATUS, success);
        String infoLog = glGetShaderInfoLog(shader);
        System.out.println(shaderName + " compile log: " + (infoLog.isEmpty() ? "OK" : infoLog));
        if (success.get(0) == GL_FALSE) {
            throw new RuntimeException(shaderName + " compilation failed: " + infoLog);
        }
    }

    private void checkProgramLinkError(int program, String programName) {
        IntBuffer success = BufferUtils.createIntBuffer(1);
        glGetProgramiv(program, GL_LINK_STATUS, success);
        String infoLog = glGetProgramInfoLog(program);
        System.out.println(programName + " link log: " + (infoLog.isEmpty() ? "OK" : infoLog));
        if (success.get(0) == GL_FALSE) {
            throw new RuntimeException(programName + " linking failed: " + infoLog);
        }
    }

    public void cleanup() {
        glDeleteProgram(program);
    }
}
