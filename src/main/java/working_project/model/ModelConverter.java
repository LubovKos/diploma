package working_project.model;

import working_project.rendering.Point3D;
import working_project.rendering.Triangle;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class ModelConverter {

    // Новый метод для загрузки модели напрямую
    public static Model loadModel(String filePath) throws IOException {
        String fileExtension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        switch (fileExtension) {
            case "stl":
                return readStl(filePath);
            case "ply":
                return readPly(filePath);
            case "obj":
                return readObj(filePath);
            default:
                throw new IOException("Unsupported file format: " + fileExtension);
        }
    }

    // STL to PLY conversion
    public static void stlToPly(String stlFile, String plyFile, boolean binaryPly) throws IOException {
        Model model = readStl(stlFile);
        writePly(model, plyFile, binaryPly);
    }

    // OBJ to PLY conversion
    public static void objToPly(String objFile, String plyFile, boolean binaryPly) throws IOException {
        Model model = readObj(objFile);
        writePly(model, plyFile, binaryPly);
    }

    // STL to OBJ conversion
    public static void stlToObj(String stlFile, String objFile) throws IOException {
        Model model = readStl(stlFile);
        writeObj(model, objFile);
    }

    // PLY to STL conversion
    public static void plyToStl(String plyFile, String stlFile, boolean binaryStl) throws IOException {
        Model model = readPly(plyFile);
        writeStl(model, stlFile, binaryStl);
    }

    // PLY to OBJ conversion
    public static void plyToObj(String plyFile, String objFile) throws IOException {
        Model model = readPly(plyFile);
        writeObj(model, objFile);
    }

    // OBJ to STL conversion
    public static void objToStl(String objFile, String stlFile, boolean binaryStl) throws IOException {
        Model model = readObj(objFile);
        writeStl(model, stlFile, binaryStl);
    }

    // STL to STL conversion (for format conversion between ASCII and binary)
    public static void stlToStl(String stlFile, String outputStlFile, boolean binaryStl) throws IOException {
        Model model = readStl(stlFile);
        writeStl(model, outputStlFile, binaryStl);
    }

    static boolean isStlBinary(String filename) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            byte[] header = new byte[80];
            file.read(header);
            String headerStr = new String(header).trim();
            return !headerStr.toLowerCase().startsWith("solid");
        }
    }

    static boolean isPlyBinary(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line = reader.readLine();
            if (line != null && line.trim().equals("ply")) {
                line = reader.readLine();
                if (line != null) {
                    return line.trim().startsWith("format binary_little_endian");
                }
            }
            return false;
        }
    }

    private static Model readStl(String filename) throws IOException {
        if (isStlBinary(filename)) {
            return readStlBinary(filename);
        } else {
            return readStlAscii(filename);
        }
    }

    private static Model readStlAscii(String filename) throws IOException {
        Model model = new Model();
        Map<Point3D, Integer> vertexMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            List<Point3D> facetVertices = new ArrayList<>();
            Point3D normal = null;
            boolean insideFacet = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue; // Пропускаем пустые строки

                if (line.startsWith("solid")) {
                    continue; // Пропускаем заголовок
                } else if (line.startsWith("endsolid")) {
                    break; // Конец файла
                } else if (line.startsWith("facet normal")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 5) {
                        throw new IOException("Invalid facet normal format: " + line);
                    }
                    try {
                        normal = new Point3D(
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3]),
                                Float.parseFloat(parts[4])
                        );
                        insideFacet = true;
                    } catch (NumberFormatException e) {
                        throw new IOException("Failed to parse normal values: " + line, e);
                    }
                } else if (line.startsWith("vertex") && insideFacet) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 4) {
                        throw new IOException("Invalid vertex format: " + line);
                    }
                    try {
                        Point3D vertex = new Point3D(
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])
                        );
                        facetVertices.add(vertex);

                        if (!vertexMap.containsKey(vertex)) {
                            model.vertices.add(vertex);
                            vertexMap.put(vertex, model.vertices.size());
                        }
                    } catch (NumberFormatException e) {
                        throw new IOException("Failed to parse vertex values: " + line, e);
                    }
                } else if (line.startsWith("endfacet")) {
                    if (facetVertices.size() != 3) {
                        throw new IOException("Expected 3 vertices in facet, but found " + facetVertices.size());
                    }
                    model.triangles.add(new Triangle(
                            facetVertices.get(0),
                            facetVertices.get(1),
                            facetVertices.get(2),
                            normal
                    ));
                    facetVertices.clear();
                    insideFacet = false;
                }
            }

            if (insideFacet) {
                throw new IOException("Incomplete facet at the end of file");
            }
        }
        return model;
    }

    private static Model readStlBinary(String filename) throws IOException {
        Model model = new Model();
        Map<Point3D, Integer> vertexMap = new HashMap<>();

        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            file.seek(80);

            byte[] countBytes = new byte[4];
            file.read(countBytes);
            ByteBuffer bb = ByteBuffer.wrap(countBytes).order(ByteOrder.LITTLE_ENDIAN);
            int triangleCount = bb.getInt();

            for (int i = 0; i < triangleCount; i++) {
                byte[] triangleData = new byte[50];
                file.read(triangleData);
                bb = ByteBuffer.wrap(triangleData).order(ByteOrder.LITTLE_ENDIAN);

                Point3D normal = new Point3D(
                        bb.getFloat(0),
                        bb.getFloat(4),
                        bb.getFloat(8)
                );

                Point3D v1 = new Point3D(bb.getFloat(12), bb.getFloat(16), bb.getFloat(20));
                Point3D v2 = new Point3D(bb.getFloat(24), bb.getFloat(28), bb.getFloat(32));
                Point3D v3 = new Point3D(bb.getFloat(36), bb.getFloat(40), bb.getFloat(44));

                if (!vertexMap.containsKey(v1)) {
                    model.vertices.add(v1);
                    vertexMap.put(v1, model.vertices.size());
                }
                if (!vertexMap.containsKey(v2)) {
                    model.vertices.add(v2);
                    vertexMap.put(v2, model.vertices.size());
                }
                if (!vertexMap.containsKey(v3)) {
                    model.vertices.add(v3);
                    vertexMap.put(v3, model.vertices.size());
                }

                model.triangles.add(new Triangle(v1, v2, v3, normal));
            }
        }
        return model;
    }

    private static Model readObj(String filename) throws IOException {
        Model model = new Model();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    String[] parts = line.split("\\s+");
                    model.vertices.add(new Point3D(
                            Float.parseFloat(parts[1]),
                            Float.parseFloat(parts[2]),
                            Float.parseFloat(parts[3])
                    ));
                } else if (line.startsWith("f ")) {
                    String[] parts = line.split("\\s+");
                    String[] v1Parts = parts[1].split("/");
                    String[] v2Parts = parts[2].split("/");
                    String[] v3Parts = parts[3].split("/");
                    int v1Index = Integer.parseInt(v1Parts[0]) - 1;
                    int v2Index = Integer.parseInt(v2Parts[0]) - 1;
                    int v3Index = Integer.parseInt(v3Parts[0]) - 1;

                    Point3D v1 = model.vertices.get(v1Index);
                    Point3D v2 = model.vertices.get(v2Index);
                    Point3D v3 = model.vertices.get(v3Index);
                    model.triangles.add(new Triangle(v1, v2, v3, null));
                }
            }
        }
        return model;
    }

    private static Model readPly(String filename) throws IOException {
        if (isPlyBinary(filename)) {
            return readPlyBinary(filename);
        } else {
            return readPlyAscii(filename);
        }
    }

    private static Model readPlyAscii(String filename) throws IOException {
        Model model = new Model();
        int vertexCount = 0, faceCount = 0;
        boolean header = true;

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (header) {
                    if (line.startsWith("element vertex")) {
                        vertexCount = Integer.parseInt(line.split("\\s+")[2]);
                    } else if (line.startsWith("element face")) {
                        faceCount = Integer.parseInt(line.split("\\s+")[2]);
                    } else if (line.equals("end_header")) {
                        header = false;
                    }
                    continue;
                }

                String[] parts = line.split("\\s+");
                if (model.vertices.size() < vertexCount) {
                    if (parts.length >= 3) { // Минимальное количество координат (x, y, z)
                        try {
                            model.vertices.add(new Point3D(
                                    Float.parseFloat(parts[0]),
                                    Float.parseFloat(parts[1]),
                                    Float.parseFloat(parts[2])
                            ));
                        } catch (NumberFormatException e) {
                            throw new IOException("Failed to parse vertex coordinates: " + line, e);
                        }
                    }
                } else if (model.triangles.size() < faceCount && parts.length > 0) {
                    if (parts[0].equals("3")) {
                        if (parts.length >= 4) { // Ожидаем 3 индекса вершин
                            try {
                                Point3D v1 = model.vertices.get(Integer.parseInt(parts[1]));
                                Point3D v2 = model.vertices.get(Integer.parseInt(parts[2]));
                                Point3D v3 = model.vertices.get(Integer.parseInt(parts[3]));
                                model.triangles.add(new Triangle(v1, v2, v3, null));
                            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                throw new IOException("Failed to parse face indices: " + line, e);
                            }
                        }
                    }
                }
            }
            System.out.println("Read PLY: " + model.vertices.size() + " vertices, " + model.triangles.size() + " triangles.");
            if (model.vertices.size() > 0 && model.triangles.size() == 0) {
                System.out.println("Detected point cloud (no triangles).");
            }
        }
        return model;
    }

    private static Model readPlyBinary(String filename) throws IOException {
        Model model = new Model();
        int vertexCount = 0, faceCount = 0;
        long dataStart = 0;

        try (RandomAccessFile file = new RandomAccessFile(filename, "r")) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    dataStart += line.length() + 1;
                    line = line.trim();
                    if (line.startsWith("element vertex")) {
                        vertexCount = Integer.parseInt(line.split("\\s+")[2]);
                    } else if (line.startsWith("element face")) {
                        faceCount = Integer.parseInt(line.split("\\s+")[2]);
                    } else if (line.equals("end_header")) {
                        break;
                    }
                }
            }

            file.seek(dataStart);

            for (int i = 0; i < vertexCount; i++) {
                byte[] vertexData = new byte[12];
                file.read(vertexData);
                ByteBuffer bb = ByteBuffer.wrap(vertexData).order(ByteOrder.LITTLE_ENDIAN);
                model.vertices.add(new Point3D(
                        bb.getFloat(0),
                        bb.getFloat(4),
                        bb.getFloat(8)
                ));
            }

            for (int i = 0; i < faceCount; i++) {
                byte[] faceData = new byte[13];
                file.read(faceData);
                ByteBuffer bb = ByteBuffer.wrap(faceData).order(ByteOrder.LITTLE_ENDIAN);
                byte vertexCountInFace = bb.get(0);
                if (vertexCountInFace == 3) {
                    int v1Index = bb.getInt(1);
                    int v2Index = bb.getInt(5);
                    int v3Index = bb.getInt(9);
                    try {
                        model.triangles.add(new Triangle(
                                model.vertices.get(v1Index),
                                model.vertices.get(v2Index),
                                model.vertices.get(v3Index),
                                null
                        ));
                    } catch (IndexOutOfBoundsException e) {
                        throw new IOException("Invalid face index in binary PLY: " + i, e);
                    }
                }
            }
            System.out.println("Read PLY: " + model.vertices.size() + " vertices, " + model.triangles.size() + " triangles.");
            if (model.vertices.size() > 0 && model.triangles.size() == 0) {
                System.out.println("Detected point cloud (no triangles).");
            }
        }
        return model;
    }

    public static void writeStl(Model model, String filename, boolean binary) throws IOException {
        if (binary) {
            writeStlBinary(model, filename);
        } else {
            writeStlAscii(model, filename);
        }
    }

    private static void writeStlAscii(Model model, String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("solid model\n");
            for (Triangle t : model.triangles) {
                Point3D normal = t.normal != null ? t.normal : calculateNormal(t);
                writer.write(String.format("facet normal %e %e %e\n", normal.x, normal.y, normal.z));
                writer.write("    outer loop\n");
                writer.write(String.format("        vertex %e %e %e\n", t.v1.x, t.v1.y, t.v1.z));
                writer.write(String.format("        vertex %e %e %e\n", t.v2.x, t.v2.y, t.v2.z));
                writer.write(String.format("        vertex %e %e %e\n", t.v3.x, t.v3.y, t.v3.z));
                writer.write("    endloop\n");
                writer.write("endfacet\n");
            }
            writer.write("endsolid model\n");
        }
    }

    private static void writeStlBinary(Model model, String filename) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filename, "rw")) {
            byte[] header = new byte[80];
            file.write(header);

            ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            bb.putInt(model.triangles.size());
            file.write(bb.array());

            for (Triangle t : model.triangles) {
                Point3D normal = t.normal != null ? t.normal : calculateNormal(t);
                bb = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN);

                bb.putFloat(0, normal.x);
                bb.putFloat(4, normal.y);
                bb.putFloat(8, normal.z);

                bb.putFloat(12, t.v1.x);
                bb.putFloat(16, t.v1.y);
                bb.putFloat(20, t.v1.z);
                bb.putFloat(24, t.v2.x);
                bb.putFloat(28, t.v2.y);
                bb.putFloat(32, t.v2.z);
                bb.putFloat(36, t.v3.x);
                bb.putFloat(40, t.v3.y);
                bb.putFloat(44, t.v3.z);

                bb.putShort(48, (short) 0);

                file.write(bb.array());
            }
        }
    }

    private static void writeObj(Model model, String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            Map<Point3D, Integer> vertexIndices = new HashMap<>();
            int index = 1;

            for (Point3D v : model.vertices) {
                writer.write(String.format("v %f %f %f\n", v.x, v.y, v.z));
                vertexIndices.put(v, index++);
            }

            for (Triangle t : model.triangles) {
                writer.write(String.format("f %d %d %d\n",
                        vertexIndices.get(t.v1),
                        vertexIndices.get(t.v2),
                        vertexIndices.get(t.v3)));
            }
        }
    }

    private static void writePly(Model model, String filename, boolean binary) throws IOException {
        if (binary) {
            writePlyBinary(model, filename);
        } else {
            writePlyAscii(model, filename);
        }
    }

    private static void writePlyAscii(Model model, String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("ply\n");
            writer.write("format ascii 1.0\n");
            writer.write("element vertex " + model.vertices.size() + "\n");
            writer.write("property float x\n");
            writer.write("property float y\n");
            writer.write("property float z\n");
            writer.write("element face " + model.triangles.size() + "\n");
            writer.write("property list uchar int vertex_indices\n");
            writer.write("end_header\n");

            for (Point3D v : model.vertices) {
                writer.write(String.format("%f %f %f\n", v.x, v.y, v.z));
            }

            Map<Point3D, Integer> vertexIndices = new HashMap<>();
            int index = 0;
            for (Point3D v : model.vertices) {
                vertexIndices.put(v, index++);
            }

            for (Triangle t : model.triangles) {
                writer.write(String.format("3 %d %d %d\n",
                        vertexIndices.get(t.v1),
                        vertexIndices.get(t.v2),
                        vertexIndices.get(t.v3)));
            }
        }
    }

    private static void writePlyBinary(Model model, String filename) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filename, "rw")) {
            String header = String.format(
                    "ply\n" +
                            "format binary_little_endian 1.0\n" +
                            "element vertex %d\n" +
                            "property float x\n" +
                            "property float y\n" +
                            "property float z\n" +
                            "element face %d\n" +
                            "property list uchar int vertex_indices\n" +
                            "end_header\n",
                    model.vertices.size(), model.triangles.size()
            );
            file.write(header.getBytes());

            for (Point3D v : model.vertices) {
                ByteBuffer bb = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
                bb.putFloat(0, v.x);
                bb.putFloat(4, v.y);
                bb.putFloat(8, v.z);
                file.write(bb.array());
            }

            Map<Point3D, Integer> vertexIndices = new HashMap<>();
            int index = 0;
            for (Point3D v : model.vertices) {
                vertexIndices.put(v, index++);
            }

            for (Triangle t : model.triangles) {
                ByteBuffer bb = ByteBuffer.allocate(13).order(ByteOrder.LITTLE_ENDIAN);
                bb.put(0, (byte) 3);
                bb.putInt(1, vertexIndices.get(t.v1));
                bb.putInt(5, vertexIndices.get(t.v2));
                bb.putInt(9, vertexIndices.get(t.v3));
                file.write(bb.array());
            }
        }
    }

    private static Point3D calculateNormal(Triangle t) {
        Point3D u = new Point3D(t.v2.x - t.v1.x, t.v2.y - t.v1.y, t.v2.z - t.v1.z);
        Point3D v = new Point3D(t.v3.x - t.v1.x, t.v3.y - t.v1.y, t.v3.z - t.v1.z);

        float nx = u.y * v.z - u.z * v.y;
        float ny = u.z * v.x - u.x * v.z;
        float nz = u.x * v.y - u.y * v.x;

        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length == 0) length = 1;

        return new Point3D(nx / length, ny / length, nz / length);
    }
};

