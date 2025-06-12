package working_project.rendering;

import working_project.model.ModelLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import working_project.core.Camera;
import working_project.core.ShaderProgram;
import working_project.core.WindowManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {
    private int meshVaoId;
    private int meshVboId;

    private int pointsVaoId = 0;
    private int pointsVboId = 0;
    private int currentPointsCount = 0;

    private ShaderProgram modelShader;
    private ShaderProgram triangleShader;
    private ShaderProgram gridShader;
    private Mesh gridMesh;
    private Mesh axisMesh;
    private float objectColorR = 0.3f;
    private float objectColorG = 0.3f;
    private float objectColorB = 0.3f;
    private float gridExtent = 1.0f;

    public Renderer() {
        // Шейдер для точек
        String modelVertexShader = "#version 410 core\n" +
                "layout(location = 0) in vec3 aPos;\n" +
                "uniform mat4 model;\n" +
                "uniform mat4 view;\n" +
                "uniform mat4 projection;\n" +
                "uniform float pointSizeScale;\n" +
                "void main() {\n" +
                "    gl_Position = projection * view * model * vec4(aPos, 1.0);\n" +
                "    vec4 viewPos = view * model * vec4(aPos, 1.0);\n" +
                "    float distance = length(viewPos);\n" +
                "    gl_PointSize = pointSizeScale / distance;\n" +
                "    gl_PointSize = clamp(gl_PointSize, 5.0, 20.0);\n" +
                "}\n";

        String modelFragmentShader = "#version 410 core\n" +
                "out vec4 FragColor;\n" +
                "uniform vec3 objectColor;\n" +
                "void main() {\n" +
                "    vec2 coord = gl_PointCoord - vec2(0.5);\n" +
                "    if (length(coord) > 0.5) discard;\n" +
                "    FragColor = vec4(objectColor, 1.0);\n" +
                "}\n";

        modelShader = new ShaderProgram(modelVertexShader, modelFragmentShader);


        // Шейдер для треугольников с Phong-освещением для обеих сторон
        String triangleVertexShader = "#version 410 core\n" +
                "layout(location = 0) in vec3 aPos;\n" +
                "layout(location = 1) in vec3 aNormal;\n" +
                "out vec3 FragPos;\n" +
                "out vec3 Normal;\n" +
                "uniform mat4 model;\n" +
                "uniform mat4 view;\n" +
                "uniform mat4 projection;\n" +
                "void main() {\n" +
                "    FragPos = vec3(model * vec4(aPos, 1.0));\n" +
                "    Normal = mat3(transpose(inverse(model))) * aNormal;\n" +
                "    gl_Position = projection * view * model * vec4(aPos, 1.0);\n" +
                "}\n";

        String triangleFragmentShader = """
                #version 410 core
                out vec4 FragColor;
                in vec3 FragPos;
                in vec3 Normal;
                uniform vec3 lightPos;
                uniform vec3 viewPos;
                uniform vec3 lightColor;
                uniform vec3 objectColor;
                uniform vec3 selectedColor;
                void main() {
                    if (selectedColor != vec3(0.0)) {
                        FragColor = vec4(selectedColor, 1.0);
                        return;
                    }
                    vec3 norm = normalize(Normal);
                    vec3 viewDir = normalize(viewPos - FragPos);
                    vec3 baseColor = (dot(norm, viewDir) > 0.0) ? objectColor : vec3(0.5, 0.5, 0.5);
                    vec3 lightNorm = (dot(norm, viewDir) > 0.0) ? norm : -norm;
                    float ambientStrength = 0.1;
                    vec3 ambient = ambientStrength * lightColor;
                    vec3 lightDir = normalize(lightPos - FragPos);
                    float diff = max(dot(lightNorm, lightDir), 0.0);
                    vec3 diffuse = diff * lightColor;
                    float specularStrength = 0.5;
                    vec3 reflectDir = reflect(-lightDir, lightNorm);
                    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32);
                    vec3 specular = specularStrength * spec * lightColor;
                    vec3 result = (ambient + diffuse + specular) * baseColor;
                    FragColor = vec4(result, 1.0);
                }
                """;

        triangleShader = new ShaderProgram(triangleVertexShader, triangleFragmentShader);

        String gridVertexShader = "#version 410 core\n" +
                "layout(location = 0) in vec3 aPos;\n" +
                "layout(location = 1) in vec3 aColor;\n" +
                "out vec3 color;\n" +
                "uniform mat4 model;\n" +
                "uniform mat4 view;\n" +
                "uniform mat4 projection;\n" +
                "uniform float scale;\n" +
                "void main() {\n" +
                "    gl_Position = projection * view * model * vec4(aPos * scale, 1.0);\n" +
                "    color = aColor;\n" +
                "}\n";

        String gridFragmentShader = "#version 410 core\n" +
                "in vec3 color;\n" +
                "out vec4 FragColor;\n" +
                "void main() {\n" +
                "    FragColor = vec4(color, 1.0);\n" +
                "}\n";

        gridShader = new ShaderProgram(gridVertexShader, gridFragmentShader);

        setupGridAndAxes();

        meshVaoId = GL30.glGenVertexArrays();
        meshVboId = GL15.glGenBuffers();
        GL30.glBindVertexArray(meshVaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, meshVboId);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        glDisable(GL_CULL_FACE);
        setObjectColor(0.0f, 0.5f, 1.0f);
    }

    public void initOrUpdatePointsBuffers(List<Point3D> points) {
        if (points == null || points.isEmpty()) {
            cleanupPointsBuffers();
            return;
        }

        if (pointsVaoId == 0) {
            pointsVaoId = GL30.glGenVertexArrays();
            System.out.println("DEBUG: Generated pointsVaoId = " + pointsVaoId);
            pointsVboId = GL15.glGenBuffers();
            System.out.println("DEBUG: Generated pointsVboId = " + pointsVboId);
            System.out.println("Generated pointsVaoId: " + pointsVaoId + ", pointsVboId: " + pointsVboId);
        }

        currentPointsCount = points.size();

        float[] vertexData = new float[currentPointsCount * 3];
        for (int i = 0; i < currentPointsCount; i++) {
            Point3D point = points.get(i);
            vertexData[i * 3] = point.x;
            vertexData[i * 3 + 1] = point.y;
            vertexData[i * 3 + 2] = point.z;
        }

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertexData.length);
        vertexBuffer.put(vertexData).flip();

        GL30.glBindVertexArray(pointsVaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, pointsVboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        MemoryUtil.memFree(vertexBuffer);

        checkGLError("initOrUpdatePointsBuffers");
    }

    public void cleanupPointsBuffers() {
        if (pointsVaoId != 0) {
            GL30.glDeleteVertexArrays(pointsVaoId);
            pointsVaoId = 0;
        }
        if (pointsVboId != 0) {
            GL15.glDeleteBuffers(pointsVboId);
            pointsVboId = 0;
        }
        currentPointsCount = 0;
        checkGLError("cleanupPointsBuffers");
    }

    private void setupGridAndAxes() {
        List<Float> gridVertices = new ArrayList<>();
        List<Integer> gridIndices = new ArrayList<>();
        int index = 0;
        for (int x = -50; x <= 50; x++) {
            gridVertices.add((float) x); gridVertices.add(0.0f); gridVertices.add(-50.0f);
            gridVertices.add(0.3f); gridVertices.add(0.3f); gridVertices.add(0.3f); // Тёмно-серый
            gridVertices.add((float) x); gridVertices.add(0.0f); gridVertices.add(50.0f);
            gridVertices.add(0.3f); gridVertices.add(0.3f); gridVertices.add(0.3f);
            gridVertices.add((float) -50); gridVertices.add(0.0f); gridVertices.add((float) x);
            gridVertices.add(0.3f); gridVertices.add(0.3f); gridVertices.add(0.3f);
            gridVertices.add((float) 50); gridVertices.add(0.0f); gridVertices.add((float) x);
            gridVertices.add(0.3f); gridVertices.add(0.3f); gridVertices.add(0.3f);
            gridIndices.add(index++); gridIndices.add(index++);
            gridIndices.add(index++); gridIndices.add(index++);
        }

        float[] gridVerticesArray = new float[gridVertices.size()];
        for (int i = 0; i < gridVertices.size(); i++) {
            gridVerticesArray[i] = gridVertices.get(i);
        }
        int[] gridIndicesArray = new int[gridIndices.size()];
        for (int i = 0; i < gridIndices.size(); i++) {
            gridIndicesArray[i] = gridIndices.get(i);
        }
        gridMesh = new Mesh(gridVerticesArray, gridIndicesArray);

        float[] axisVertices = {
                -50.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                50.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, -50.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 50.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, -50.0f, 0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 50.0f, 0.0f, 0.0f, 1.0f
        };
        int[] axisIndices = {0, 1, 2, 3, 4, 5};
        axisMesh = new Mesh(axisVertices, axisIndices);
    }

    public void updateGridSize(float extent) {
        gridExtent = extent * 10.0f;
        List<Float> gridVertices = new ArrayList<>();
        List<Integer> gridIndices = new ArrayList<>();
        int index = 0;
        float step = gridExtent / 50.0f; // Плотность линий
        for (float x = -gridExtent; x <= gridExtent; x += step) {
            gridVertices.add(x); gridVertices.add(0.0f); gridVertices.add(-gridExtent);
            gridVertices.add(0.3f); gridVertices.add(0.3f); gridVertices.add(0.3f);
            gridVertices.add(x); gridVertices.add(0.0f); gridVertices.add(gridExtent);
            gridVertices.add(0.3f); gridVertices.add(0.3f); gridVertices.add(0.3f);
            gridVertices.add(-gridExtent); gridVertices.add(0.0f); gridVertices.add(x);
            gridVertices.add(0.3f); gridVertices.add(0.3f); gridVertices.add(0.3f);
            gridVertices.add(gridExtent); gridVertices.add(0.0f); gridVertices.add(x);
            gridVertices.add(0.3f); gridVertices.add(0.3f); gridVertices.add(0.3f);
            gridIndices.add(index++); gridIndices.add(index++);
            gridIndices.add(index++); gridIndices.add(index++);
        }

        float[] gridVerticesArray = new float[gridVertices.size()];
        for (int i = 0; i < gridVertices.size(); i++) {
            gridVerticesArray[i] = gridVertices.get(i);
        }
        int[] gridIndicesArray = new int[gridIndices.size()];
        for (int i = 0; i < gridIndices.size(); i++) {
            gridIndicesArray[i] = gridIndices.get(i);
        }
        if (gridMesh != null) {
            gridMesh.cleanup();
        }
        gridMesh = new Mesh(gridVerticesArray, gridIndicesArray);

        float[] axisVertices = {
                -gridExtent, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                gridExtent, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, -gridExtent, 0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, gridExtent, 0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, -gridExtent, 0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, gridExtent, 0.0f, 0.0f, 1.0f
        };
        int[] axisIndices = {0, 1, 2, 3, 4, 5};
        if (axisMesh != null) {
            axisMesh.cleanup();
        }
        axisMesh = new Mesh(axisVertices, axisIndices);
    }


    public void setObjectColor(float r, float g, float b) {
        objectColorR = r;
        objectColorG = g;
        objectColorB = b;
        modelShader.use();
        modelShader.setUniform3f("objectColor", r, g, b);
        triangleShader.use();
        triangleShader.setUniform3f("objectColor", r, g, b);
        checkGLError("After setting object color");
    }

    public void render(WindowManager window,Camera camera, List<ModelLoader.Chunk> chunks, float modelYaw, float modelPitch) {
        glClearColor(0.8f, 0.8f, 0.8f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        checkGLError("After clear");

        Matrix4f model = new Matrix4f().identity();
        Matrix4f view = camera.getViewMatrix();
        Matrix4f projection = camera.getProjectionMatrix((float) window.getWidth() / window.getHeight(), 0.1f, 1000.0f);

        FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(16);
        FloatBuffer viewBuffer = BufferUtils.createFloatBuffer(16);
        FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);

        // Рендеринг сетки и осей
        gridShader.use();
        model.identity().get(modelBuffer);
        view.get(viewBuffer);
        projection.get(projBuffer);
        gridShader.setUniformMatrix4fv("model", modelBuffer);
        gridShader.setUniformMatrix4fv("view", viewBuffer);
        gridShader.setUniformMatrix4fv("projection", projBuffer);
        gridShader.setUniform1f("scale", gridExtent / 50.0f); // Масштабируем сетку
        glLineWidth(1.0f);
        glDepthFunc(GL_LEQUAL);
        gridMesh.render(GL_LINES);
        glLineWidth(2.0f);
        axisMesh.render(GL_LINES);
        glLineWidth(1.0f);
        glDepthFunc(GL_LESS);
        checkGLError("After grid and axis draw");

        if (chunks.isEmpty()) {
            return;
        }

        triangleShader.use();
        model.identity();
        if (!chunks.isEmpty()) {
            ModelLoader.AABB globalAABB = new ModelLoader().getGlobalAABB(chunks);
            Vector3f center = globalAABB.center();
            model.translate(center.x, center.y, center.z);
            model.rotate((float) Math.toRadians(modelYaw), 0, 1, 0);
            model.rotate((float) Math.toRadians(modelPitch), 1, 0, 0);
            model.translate(-center.x, -center.y, -center.z);
        }
        model.get(modelBuffer);
        view.get(viewBuffer);
        projection.get(projBuffer);
        triangleShader.setUniformMatrix4fv("model", modelBuffer);
        triangleShader.setUniformMatrix4fv("view", viewBuffer);
        triangleShader.setUniformMatrix4fv("projection", projBuffer);
        triangleShader.setUniform3f("lightPos", 10.0f, 10.0f, 10.0f);
        triangleShader.setUniform3f("lightColor", 1.0f, 1.0f, 1.0f);
        triangleShader.setUniform3f("viewPos", camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
        triangleShader.setUniform3f("objectColor", objectColorR, objectColorG, objectColorB);
        triangleShader.setUniform3f("selectedColor", 0.0f, 0.0f, 0.0f);

        checkGLError("After setting uniforms for triangles");

        for (ModelLoader.Chunk chunk : chunks) {
            if (chunk.mesh.getVao() != 0) {
                chunk.mesh.render();
            }
        }
    }

    public void renderPoints(WindowManager window, Camera camera, List<Point3D> points, float yaw, float pitch) {
        if (points == null || points.isEmpty()) return;

        glClearColor(0.8f, 0.8f, 0.8f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL32C.GL_PROGRAM_POINT_SIZE);
        checkGLError("After setup");

        Vector3f center = new Vector3f();
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (Point3D point : points) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
            minZ = Math.min(minZ, point.z);
            maxZ = Math.max(maxZ, point.z);
        }
        center.set((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f);
        float extent = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) * 0.5f;

        updateGridSize(extent);

        Matrix4f model = new Matrix4f().identity();
        model.translate(center.x, center.y, center.z);
        model.rotate((float) Math.toRadians(pitch), 1, 0, 0);
        model.rotate((float) Math.toRadians(yaw), 0, 1, 0);
        model.translate(-center.x, -center.y, -center.z);

        Matrix4f view = camera.getViewMatrix();
        Matrix4f projection = camera.getProjectionMatrix((float) window.getWidth() / window.getHeight(), 0.1f, 1000.0f);

        float[] vertexData = new float[points.size() * 3];
        for (int i = 0; i < points.size(); i++) {
            Point3D point = points.get(i);
            vertexData[i * 3] = point.x;
            vertexData[i * 3 + 1] = point.y;
            vertexData[i * 3 + 2] = point.z;
        }

        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertexData.length);
        vertexBuffer.put(vertexData).flip();
        int vboPoints = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboPoints);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(vertexBuffer);

        int vaoPoints = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoPoints);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboPoints);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * 4, 0);
        GL20.glEnableVertexAttribArray(0);
        GL30.glBindVertexArray(0);

        modelShader.use();
        FloatBuffer modelBuffer = BufferUtils.createFloatBuffer(16);
        FloatBuffer viewBuffer = BufferUtils.createFloatBuffer(16);
        FloatBuffer projBuffer = BufferUtils.createFloatBuffer(16);
        model.get(modelBuffer);
        view.get(viewBuffer);
        projection.get(projBuffer);
        modelShader.setUniformMatrix4fv("model", modelBuffer);
        modelShader.setUniformMatrix4fv("view", viewBuffer);
        modelShader.setUniformMatrix4fv("projection", projBuffer);
        modelShader.setUniform1f("pointSizeScale", 10.0f);
        modelShader.setUniform3f("objectColor", objectColorR, objectColorG, objectColorB);

        checkGLError("After setting uniforms");

        GL30.glBindVertexArray(vaoPoints);
        GL11.glDrawArrays(GL11.GL_POINTS, 0, points.size());
        GL30.glBindVertexArray(0);

        GL15.glDeleteBuffers(vboPoints);
        GL30.glDeleteVertexArrays(vaoPoints);

        gridShader.use();
        model.identity().get(modelBuffer);
        gridShader.setUniformMatrix4fv("model", modelBuffer);
        gridShader.setUniformMatrix4fv("view", viewBuffer);
        gridShader.setUniformMatrix4fv("projection", projBuffer);
        gridShader.setUniform1f("scale", gridExtent / 50.0f);
        glLineWidth(1.0f);
        glDepthFunc(GL_LEQUAL);
        gridMesh.render(GL_LINES);
        glLineWidth(2.0f);
        axisMesh.render(GL_LINES);
        glLineWidth(1.0f);
        glDepthFunc(GL_LESS);
        checkGLError("After grid and axis draw");
    }

    private void checkGLError(String stage) {
        int error = glGetError();
        if (error != GL_NO_ERROR) {
            System.err.println("OpenGL Error at " + stage + ": " + error);
        }
    }

    public void cleanup() {
        modelShader.cleanup();
        triangleShader.cleanup();
        gridShader.cleanup();
        if (gridMesh != null) gridMesh.cleanup();
        if (axisMesh != null) axisMesh.cleanup();
        GL15.glDeleteBuffers(meshVboId);
        GL30.glDeleteVertexArrays(meshVaoId);
        cleanupPointsBuffers();
    }
}
