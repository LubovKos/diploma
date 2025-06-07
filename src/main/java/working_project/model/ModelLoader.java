package working_project.model;

import org.joml.Vector3f;
import working_project.rendering.Mesh;

import java.util.ArrayList;
import java.util.List;

public class ModelLoader {

    public static class Chunk {
        public List<Triangle> triangles = new ArrayList<>();
        public AABB aabb = new AABB();
        public Mesh mesh;
        // НОВЫЙ КОД ДЛЯ ЛАССО: Список выделенных треугольников
        private List<Triangle> selectedTriangles = new ArrayList<>();
        // НОВЫЙ КОД ДЛЯ ЛАССО: Меш для выделенных треугольников
        private Mesh selectedMesh;

        public int[] indices;
        public float[] vertices;

        void setupMesh() {
            System.out.println("Setting up chunk mesh with " + triangles.size() + " triangles");
            float[] vertices = new float[triangles.size() * 3 * 6];
            int[] indices = new int[triangles.size() * 3];
            int vertexIndex = 0;
            for (int i = 0; i < triangles.size(); i++) {
                Triangle tri = triangles.get(i);
                for (int j = 0; j < 3; j++) {
                    Vertex v = tri.vertices[j];
                    vertices[vertexIndex] = v.position.x;
                    vertices[vertexIndex + 1] = v.position.y;
                    vertices[vertexIndex + 2] = v.position.z;
                    vertices[vertexIndex + 3] = v.normal.x;
                    vertices[vertexIndex + 4] = v.normal.y;
                    vertices[vertexIndex + 5] = v.normal.z;
                    indices[i * 3 + j] = i * 3 + j;
                    vertexIndex += 6;
                }
            }
            mesh = new Mesh(vertices, indices);
            System.out.println("Chunk mesh setup: VAO=" + mesh.getVao() + ", triangles=" + triangles.size());
        }

        public void setupSelectedMesh() {
            if (selectedMesh != null) {
                selectedMesh.cleanup();
                selectedMesh = null;
            }

            if (selectedTriangles.isEmpty()) {
                System.out.println("Skipping selected mesh setup: no selected triangles");
                return;
            }

            float[] selectedVertices = new float[selectedTriangles.size() * 3 * 6];
            int[] selectedIndices = new int[selectedTriangles.size() * 3];
            int vertexIndex = 0;
            for (int i = 0; i < selectedTriangles.size(); i++) {
                Triangle tri = selectedTriangles.get(i);
                for (int j = 0; j < 3; j++) {
                    Vertex v = tri.vertices[j];
                    selectedVertices[vertexIndex] = v.position.x;
                    selectedVertices[vertexIndex + 1] = v.position.y;
                    selectedVertices[vertexIndex + 2] = v.position.z;
                    selectedVertices[vertexIndex + 3] = v.normal.x;
                    selectedVertices[vertexIndex + 4] = v.normal.y;
                    selectedVertices[vertexIndex + 5] = v.normal.z;
                    selectedIndices[i * 3 + j] = i * 3 + j;
                    vertexIndex += 6;
                }
            }

            selectedMesh = new Mesh(selectedVertices, selectedIndices);
            System.out.println("Selected mesh setup: VAO=" + selectedMesh.getVao() + ", triangles=" + selectedTriangles.size());
        }

        // НОВЫЙ КОД ДЛЯ ЛАССО: Возвращает selectedMesh
        public Mesh getSelectedMesh() {
            return selectedMesh;
        }

        public void setSelectedTriangles(List<Triangle> newSelectedTriangles) {
            selectedTriangles.clear();
            selectedTriangles.addAll(newSelectedTriangles);
            System.out.println("Setting up selected mesh with " + selectedTriangles.size() + " triangles");
            setupSelectedMesh();
        }

        public void cleanup() {
            if (mesh != null) mesh.cleanup();
            // НОВЫЙ КОД ДЛЯ ЛАССО: Очистка selectedMesh
            if (selectedMesh != null) {
                selectedMesh.cleanup();
                selectedMesh = null;
            }
        }
    }

    public static class Vertex {
        public Vector3f position;
        Vector3f normal;

        Vertex(Vector3f position, Vector3f normal) {
            this.position = position;
            this.normal = normal;
        }
    }

    public static class Triangle {
        public Vertex[] vertices = new Vertex[3];

        Triangle(Vertex v0, Vertex v1, Vertex v2) {
            vertices[0] = v0;
            vertices[1] = v1;
            vertices[2] = v2;
        }

        boolean isDegenerate() {
            Vector3f v0 = vertices[0].position;
            Vector3f v1 = vertices[1].position;
            Vector3f v2 = vertices[2].position;
            return v0.equals(v1) || v1.equals(v2) || v0.equals(v2);
        }
    }

    public static class AABB {
        Vector3f min, max;

        AABB() {
            min = new Vector3f(Float.MAX_VALUE);
            max = new Vector3f(-Float.MAX_VALUE);
        }

        void update(Vector3f point) {
            min.min(point);
            max.max(point);
        }

        public boolean intersect(org.joml.Matrix4f viewProj) {
            Vector3f[] corners = {
                    new Vector3f(min.x, min.y, min.z),
                    new Vector3f(max.x, min.y, min.z),
                    new Vector3f(min.x, max.y, min.z),
                    new Vector3f(max.x, max.y, min.z),
                    new Vector3f(min.x, min.y, max.z),
                    new Vector3f(max.x, min.y, max.z),
                    new Vector3f(min.x, max.y, max.z),
                    new Vector3f(max.x, max.y, max.z)
            };
            for (Vector3f corner : corners) {
                org.joml.Vector4f clipSpace = new org.joml.Vector4f(corner, 1.0f).mul(viewProj);
                if (clipSpace.z / clipSpace.w > -1.0f && clipSpace.x / clipSpace.w >= -1.0f && clipSpace.x / clipSpace.w <= 1.0f &&
                        clipSpace.y / clipSpace.w >= -1.0f && clipSpace.y / clipSpace.w <= 1.0f) {
                    return true;
                }
            }
            return false;
        }

        public Vector3f center() {
            return new Vector3f(min).add(max).mul(0.5f);
        }

        public float extent() {
            return max.distance(min) * 0.5f;
        }
    }

    public AABB getGlobalAABB(List<Chunk> chunks) {
        AABB globalAABB = new AABB();
        for (Chunk chunk : chunks) {
            globalAABB.min.min(chunk.aabb.min);
            globalAABB.max.max(chunk.aabb.max);
        }
        return globalAABB;
    }

    public List<Chunk> createChunksFromData(List<float[]> triangleData) {
        System.out.println("Creating chunks from " + triangleData.size() + " triangles");
        List<Chunk> chunks = new ArrayList<>();
        Chunk currentChunk = new Chunk();
        int chunkSize = 500;
        int triangleCount = 0;

        AABB globalAABB = new AABB();
        for (float[] data : triangleData) {
            globalAABB.update(new Vector3f(data[0], data[1], data[2]));
            globalAABB.update(new Vector3f(data[6], data[7], data[8]));
            globalAABB.update(new Vector3f(data[12], data[13], data[14]));
        }

        Vector3f center = globalAABB.center();
        float extent = globalAABB.extent();
        float scale = extent > 0 ? 5.0f / extent : 1.0f;

        for (float[] data : triangleData) {
            if (currentChunk.triangles.size() >= chunkSize) {
                chunks.add(currentChunk);
                currentChunk.setupMesh();
                currentChunk = new Chunk();
            }

            Vertex[] vertices = new Vertex[3];

            for (int i = 0; i < 3; i++) {
                int offset = i * 6;
                Vector3f pos = new Vector3f(data[offset], data[offset + 1], data[offset + 2]);
                pos.sub(center).mul(scale);
                Vector3f normal = new Vector3f(data[offset + 3], data[offset + 4], data[offset + 5]);
                vertices[i] = new Vertex(pos, normal);
                currentChunk.aabb.update(pos);
            }

            Triangle triangle = new Triangle(vertices[0], vertices[1], vertices[2]);

            if (!triangle.isDegenerate()) {
                currentChunk.triangles.add(triangle);
                triangleCount++;
            }

            if (triangleCount % 500 == 0) {
                System.out.println("Processed " + triangleCount + " triangles");
            }
        }

        if (!currentChunk.triangles.isEmpty()) {
            chunks.add(currentChunk);
            currentChunk.setupMesh();
        }

        System.out.println("Created " + chunks.size() + " chunks with " + triangleCount + " triangles");
        return chunks;
    }
}
