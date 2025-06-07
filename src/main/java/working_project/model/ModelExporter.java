package working_project.model;

import org.joml.Vector3f;
import working_project.rendering.Point3D;
import working_project.rendering.Triangle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ModelExporter {
    public static void exportModel(Model model, File file, String format) throws IOException {
        switch (format.toLowerCase()) {
            case "obj":
                exportToOBJ(model, file);
                break;
            case "stl":
                exportToSTL(model, file);
                break;
            case "ply":
                exportToPLY(model, file);
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private static void exportToSTL(Model model, File file) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("solid exported_model");
            for (Triangle triangle : model.triangles) {
                Vector3f v1 = new Vector3f(triangle.v1.x, triangle.v1.y, triangle.v1.z);
                Vector3f v2 = new Vector3f(triangle.v2.x, triangle.v2.y, triangle.v2.z);
                Vector3f v3 = new Vector3f(triangle.v3.x, triangle.v3.y, triangle.v3.z);
                Vector3f edge1 = new Vector3f(v2).sub(v1);
                Vector3f edge2 = new Vector3f(v3).sub(v1);
                Vector3f normal = new Vector3f(edge1).cross(edge2).normalize();
                out.printf(Locale.US, "  facet normal %.6f %.6f %.6f%n", normal.x, normal.y, normal.z);
                out.println("    outer loop");
                out.printf(Locale.US, "      vertex %.6f %.6f %.6f%n", triangle.v1.x, triangle.v1.y, triangle.v1.z);
                out.printf(Locale.US, "      vertex %.6f %.6f %.6f%n", triangle.v2.x, triangle.v2.y, triangle.v2.z);
                out.printf(Locale.US, "      vertex %.6f %.6f %.6f%n", triangle.v3.x, triangle.v3.y, triangle.v3.z);
                out.println("    endloop");
                out.println("  endfacet");
            }
            out.println("endsolid exported_model");
        }
    }

    private static void exportToOBJ(Model model, File file) throws IOException {
        Map<Point3D, Integer> vertexIndexMap = new HashMap<>();
        int index = 1;
        for (Point3D vertex : model.vertices) {
            vertexIndexMap.put(vertex, index++);
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            for (Point3D vertex : model.vertices) {
                out.printf(Locale.US, "v %.6f %.6f %.6f%n", vertex.x, vertex.y, vertex.z);
            }
            for (Triangle triangle : model.triangles) {
                Integer idx1 = vertexIndexMap.get(triangle.v1);
                Integer idx2 = vertexIndexMap.get(triangle.v2);
                Integer idx3 = vertexIndexMap.get(triangle.v3);
                if (idx1 != null && idx2 != null && idx3 != null) {
                    out.printf("f %d %d %d%n", idx1, idx2, idx3);
                }
            }
        }
    }

    private static void exportToPLY(Model model, File file) throws IOException {
        Map<Point3D, Integer> vertexIndexMap = new HashMap<>();
        int index = 0;
        for (Point3D vertex : model.vertices) {
            vertexIndexMap.put(vertex, index++);
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
            out.println("ply");
            out.println("format ascii 1.0");
            out.println("element vertex " + model.vertices.size());
            out.println("property float x");
            out.println("property float y");
            out.println("property float z");
            out.println("element face " + model.triangles.size());
            out.println("property list uchar int vertex_indices");
            out.println("end_header");

            for (Point3D vertex : model.vertices) {
                out.printf(Locale.US, "%.6f %.6f %.6f%n", vertex.x, vertex.y, vertex.z);
            }

            for (Triangle triangle : model.triangles) {
                Integer idx1 = vertexIndexMap.get(triangle.v1);
                Integer idx2 = vertexIndexMap.get(triangle.v2);
                Integer idx3 = vertexIndexMap.get(triangle.v3);
                if (idx1 != null && idx2 != null && idx3 != null) {
                    out.printf("3 %d %d %d%n", idx1, idx2, idx3);
                }
            }
        }
    }
}