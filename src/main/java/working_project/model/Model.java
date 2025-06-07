package working_project.model;

import working_project.rendering.Point3D;
import working_project.rendering.Triangle;

import java.util.*;

public class Model {
    public List<Point3D> vertices;
    public List<Triangle> triangles;

    public Model() {
        vertices = new ArrayList<>();
        triangles = new ArrayList<>();
    }

    public Model(List<Point3D> vertices, List<Triangle> triangles) {
        this.vertices = new ArrayList<>(vertices);
        this.triangles = new ArrayList<>(triangles);
    }

    public void removeNoise(int minComponentSize) {
        System.out.println("Starting noise removal: vertices=" + vertices.size() + ", triangles=" + triangles.size());

        if (triangles.isEmpty()) {
            System.out.println("No triangles to process. Skipping noise removal.");
            return;
        }

        // Собираем уникальные вершины из треугольников и устраняем дубликаты
        Map<Point3D, Point3D> vertexMap = new HashMap<>();
        Set<Point3D> uniqueVertices = new HashSet<>();
        for (Triangle t : triangles) {
            Point3D v1 = vertexMap.computeIfAbsent(t.v1, k -> t.v1);
            Point3D v2 = vertexMap.computeIfAbsent(t.v2, k -> t.v2);
            Point3D v3 = vertexMap.computeIfAbsent(t.v3, k -> t.v3);
            t.v1 = v1;
            t.v2 = v2;
            t.v3 = v3;
            uniqueVertices.add(v1);
            uniqueVertices.add(v2);
            uniqueVertices.add(v3);
        }
        System.out.println("Unique vertices in triangles: " + uniqueVertices.size());

        // Проверяем соответствие вершин
        int verticesInList = vertices.stream().filter(uniqueVertices::contains).mapToInt(v -> 1).sum();
        System.out.println("Vertices from triangles found in vertices list: " + verticesInList);

        // Динамический порог
        int dynamicMinSize = Math.max(minComponentSize, uniqueVertices.size() / 100); // 1% от вершин
        System.out.println("Dynamic min component size: " + dynamicMinSize);

        // Построение графа связности
        Map<Point3D, Set<Point3D>> adjacencyList = new HashMap<>();
        for (Triangle t : triangles) {
            addEdge(adjacencyList, t.v1, t.v2);
            addEdge(adjacencyList, t.v2, t.v3);
            addEdge(adjacencyList, t.v3, t.v1);
        }
        System.out.println("Adjacency list size: " + adjacencyList.size());

        // Поиск связных компонент с итеративным DFS
        List<Set<Point3D>> components = new ArrayList<>();
        Set<Point3D> visited = new HashSet<>();
        for (Point3D vertex : uniqueVertices) {
            if (!visited.contains(vertex)) {
                Set<Point3D> component = new HashSet<>();
                iterativeDfs(vertex, adjacencyList, visited, component);
                if (!component.isEmpty()) {
                    components.add(component);
                }
            }
        }
        System.out.println("Found " + components.size() + " connected components");

        // Сортируем компоненты по размеру (убывание)
        components.sort((a, b) -> Integer.compare(b.size(), a.size()));
        Set<Point3D> verticesToKeep = new HashSet<>();
        for (int i = 0; i < components.size(); i++) {
            Set<Point3D> component = components.get(i);
            System.out.println("Component " + (i + 1) + " size: " + component.size());
            if (component.size() >= dynamicMinSize) {
                verticesToKeep.addAll(component);
            }
        }
        System.out.println("Vertices to keep: " + verticesToKeep.size());

        // Удаляем треугольники, содержащие вершины вне выбранных компонент
        Set<Point3D> verticesToRemove = new HashSet<>(uniqueVertices);
        verticesToRemove.removeAll(verticesToKeep);
        triangles.removeIf(t ->
                !verticesToKeep.contains(t.v1) ||
                        !verticesToKeep.contains(t.v2) ||
                        !verticesToKeep.contains(t.v3));
        System.out.println("Triangles after removal: " + triangles.size());

        vertices.removeIf(v -> !verticesToKeep.contains(v));
        System.out.println("Vertices after removal: " + vertices.size());

        if (triangles.isEmpty()) {
            System.out.println("Warning: All triangles removed. Model is empty.");
        } else {
            computeNormals();
            System.out.println("Normals recomputed after noise removal.");
        }
    }

    private void addEdge(Map<Point3D, Set<Point3D>> adjacencyList, Point3D v1, Point3D v2) {
        adjacencyList.computeIfAbsent(v1, k -> new HashSet<>()).add(v2);
        adjacencyList.computeIfAbsent(v2, k -> new HashSet<>()).add(v1);
    }

    private void iterativeDfs(Point3D startVertex, Map<Point3D, Set<Point3D>> adjacencyList, Set<Point3D> visited, Set<Point3D> component) {
        Stack<Point3D> stack = new Stack<>();
        stack.push(startVertex);
        while (!stack.isEmpty()) {
            Point3D vertex = stack.pop();
            if (!visited.contains(vertex)) {
                visited.add(vertex);
                component.add(vertex);
                for (Point3D neighbor : adjacencyList.getOrDefault(vertex, Collections.emptySet())) {
                    if (!visited.contains(neighbor)) {
                        stack.push(neighbor);
                    }
                }
            }
        }
    }

    public void findLargestConnectedComponent() {
        if (triangles.isEmpty()) {
            return;
        }

        // Построение списка смежности
        Map<Integer, List<Integer>> adj = new HashMap<>();
        for (int i = 0; i < triangles.size(); i++) {
            adj.put(i, new ArrayList<>());
        }

        Map<Point3D, List<Integer>> vertexToTriangleIndices = new HashMap<>();
        for (int i = 0; i < triangles.size(); i++) {
            Triangle tri = triangles.get(i);
            for (Point3D vertex : new Point3D[]{tri.v1, tri.v2, tri.v3}) {
                vertexToTriangleIndices.computeIfAbsent(vertex, k -> new ArrayList<>()).add(i);
            }
        }

        for (int i = 0; i < triangles.size(); i++) {
            Triangle t1 = triangles.get(i);
            Point3D[] t1Verts = {t1.v1, t1.v2, t1.v3};
            Set<Integer> potentialNeighbors = new HashSet<>();
            // Собираем всех треугольников, которые делят хотя бы одну вершину с t1
            for (Point3D vertex : t1Verts) {
                potentialNeighbors.addAll(vertexToTriangleIndices.get(vertex));
            }

            for (Integer j : potentialNeighbors) {
                if (i >= j) continue; // Рассматриваем каждую пару один раз и не сравниваем треугольник сам с собой

                Triangle t2 = triangles.get(j);
                Point3D[] t2Verts = {t2.v1, t2.v2, t2.v3};
                int sharedVertices = 0;
                for (Point3D v1 : t1Verts) {
                    for (Point3D v2 : t2Verts) {
                        if (v1.equals(v2)) {
                            sharedVertices++;
                        }
                    }
                }
                if (sharedVertices == 2) { // Две общих вершины означают общее ребро
                    adj.get(i).add(j);
                    adj.get(j).add(i);
                }
            }
        }

        // Шаг 2 и 3: Поиск связанных компонент и выбор наибольшей
        List<Triangle> largestComponent = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        for (int i = 0; i < triangles.size(); i++) {
            if (!visited.contains(i)) {
                List<Triangle> currentComponentTriangles = new ArrayList<>();
                List<Integer> currentComponentIndices = new ArrayList<>();
                Queue<Integer> queue = new LinkedList<>();

                queue.add(i);
                visited.add(i);

                while(!queue.isEmpty()){
                    int u = queue.poll();
                    currentComponentIndices.add(u);
                    currentComponentTriangles.add(triangles.get(u));
                    for(int v : adj.get(u)){
                        if(!visited.contains(v)){
                            visited.add(v);
                            queue.add(v);
                        }
                    }
                }

                if (currentComponentTriangles.size() > largestComponent.size()) {
                    largestComponent = currentComponentTriangles;
                }
            }
        }
        triangles = largestComponent;
    }

    public void gaussianSmooth(float sigma, int iterations) {
        System.out.println("Starting Gaussian smoothing: vertices=" + vertices.size() + ", triangles=" + triangles.size());

        // Вычисляем среднее расстояние между вершинами для нормализации
        double totalDistance = 0;
        int edgeCount = 0;
        for (Triangle t : triangles) {
            totalDistance += Math.sqrt(
                    (t.v1.x - t.v2.x) * (t.v1.x - t.v2.x) +
                            (t.v1.y - t.v2.y) * (t.v1.y - t.v2.y) +
                            (t.v1.z - t.v2.z) * (t.v1.z - t.v2.z)
            );
            totalDistance += Math.sqrt(
                    (t.v2.x - t.v3.x) * (t.v2.x - t.v3.x) +
                            (t.v2.y - t.v3.y) * (t.v2.y - t.v3.y) +
                            (t.v2.z - t.v3.z) * (t.v2.z - t.v3.z)
            );
            totalDistance += Math.sqrt(
                    (t.v3.x - t.v1.x) * (t.v3.x - t.v1.x) +
                            (t.v3.y - t.v1.y) * (t.v3.y - t.v1.y) +
                            (t.v3.z - t.v1.z) * (t.v3.z - t.v1.z)
            );
            edgeCount += 3;
        }
        double avgDistance = edgeCount > 0 ? totalDistance / edgeCount : 1.0;
        double scale = avgDistance > 0 ? 1.0 / avgDistance : 1.0;
        System.out.println("Average edge length: " + avgDistance + ", scale factor: " + scale);

        // Многократное сглаживание
        for (int iter = 0; iter < iterations; iter++) {
            // Построение графа соседей
            Map<Point3D, Set<Point3D>> adjacencyList = new HashMap<>();
            for (Triangle t : triangles) {
                addEdge(adjacencyList, t.v1, t.v2);
                addEdge(adjacencyList, t.v2, t.v3);
                addEdge(adjacencyList, t.v3, t.v1);
            }

            Map<Point3D, Point3D> newPositions = new HashMap<>();
            double maxDisplacement = 0;
            int maxNeighbors = 0;
            for (Point3D vertex : vertices) {
                double sumX = vertex.x;
                double sumY = vertex.y;
                double sumZ = vertex.z;
                double sumWeights = 1.0;
                Set<Point3D> neighbors = adjacencyList.getOrDefault(vertex, Collections.emptySet());
                maxNeighbors = Math.max(maxNeighbors, neighbors.size());
                for (Point3D neighbor : neighbors) {
                    double d = Math.sqrt(
                            (vertex.x - neighbor.x) * (vertex.x - neighbor.x) +
                                    (vertex.y - neighbor.y) * (vertex.y - neighbor.y) +
                                    (vertex.z - neighbor.z) * (vertex.z - neighbor.z)
                    ) * scale;
                    double w = Math.exp(-d * d / (2 * sigma * sigma));
                    sumX += neighbor.x * w;
                    sumY += neighbor.y * w;
                    sumZ += neighbor.z * w;
                    sumWeights += w;
                }
                Point3D newPos = new Point3D(
                        (float) (sumX / sumWeights),
                        (float) (sumY / sumWeights),
                        (float) (sumZ / sumWeights)
                );
                newPositions.put(vertex, newPos);
                double disp = Math.sqrt(
                        (newPos.x - vertex.x) * (newPos.x - vertex.x) +
                                (newPos.y - vertex.y) * (newPos.y - vertex.y) +
                                (newPos.z - vertex.z) * (newPos.z - vertex.z)
                );
                maxDisplacement = Math.max(maxDisplacement, disp);
            }
            System.out.println("Iteration " + (iter + 1) + ": max displacement = " + maxDisplacement + ", max neighbors = " + maxNeighbors);

            for (Point3D vertex : vertices) {
                Point3D newPos = newPositions.get(vertex);
                if (newPos != null) {
                    vertex.x = newPos.x;
                    vertex.y = newPos.y;
                    vertex.z = newPos.z;
                } else {
                    System.err.println("Warning: No new position for vertex");
                }
            }
        }

        computeNormals();
        System.out.println("Gaussian smoothing completed: vertices=" + vertices.size() + ", triangles=" + triangles.size());
    }

    // Метод сглаживания границ
    public void smoothBoundaries(float smoothingFactor, int iterations) {
        if (triangles.isEmpty()) {
            System.out.println("No triangles to smooth. Skipping boundary smoothing.");
            return;
        }

        System.out.println("Starting boundary smoothing: vertices=" + vertices.size() + ", triangles=" + triangles.size());

        for (int iter = 0; iter < iterations; iter++) {
            // Находим граничные вершины
            Map<Point3D, List<Point3D>> adjacency = new HashMap<>();
            Map<String, Integer> edgeCount = new HashMap<>();
            Set<Point3D> boundaryVertices = new HashSet<>();

            // Собираем рёбра и их частоту
            for (Triangle tri : triangles) {
                Point3D[] verts = {tri.v1, tri.v2, tri.v3};
                for (int i = 0; i < 3; i++) {
                    Point3D v1 = verts[i];
                    Point3D v2 = verts[(i + 1) % 3];
                    adjacency.computeIfAbsent(v1, k -> new ArrayList<>()).add(v2);
                    adjacency.computeIfAbsent(v2, k -> new ArrayList<>()).add(v1);
                    // Уникальный ключ ребра (сортируем по hashCode)
                    Point3D minV = v1.hashCode() < v2.hashCode() ? v1 : v2;
                    Point3D maxV = v1.hashCode() < v2.hashCode() ? v2 : v1;
                    String edgeKey = minV.hashCode() + "_" + maxV.hashCode();
                    edgeCount.merge(edgeKey, 1, Integer::sum);
                }
            }

            // Граничные вершины — те, чьи рёбра встречаются один раз
            for (Triangle tri : triangles) {
                Point3D[] verts = {tri.v1, tri.v2, tri.v3};
                for (int i = 0; i < 3; i++) {
                    Point3D v1 = verts[i];
                    Point3D v2 = verts[(i + 1) % 3];
                    Point3D minV = v1.hashCode() < v2.hashCode() ? v1 : v2;
                    Point3D maxV = v1.hashCode() < v2.hashCode() ? v2 : v1;
                    String edgeKey = minV.hashCode() + "_" + maxV.hashCode();
                    if (edgeCount.get(edgeKey) == 1) {
                        boundaryVertices.add(v1);
                        boundaryVertices.add(v2);
                    }
                }
            }

            System.out.println("Iteration " + (iter + 1) + ": Found " + boundaryVertices.size() + " boundary vertices");

            // Laplacian smoothing для граничных вершин
            Map<Point3D, Point3D> newPositions = new HashMap<>();
            for (Point3D vertex : boundaryVertices) {
                List<Point3D> neighbors = adjacency.getOrDefault(vertex, new ArrayList<>());
                if (neighbors.isEmpty()) continue;

                // Средняя позиция соседей
                float avgX = 0, avgY = 0, avgZ = 0;
                for (Point3D neighbor : neighbors) {
                    avgX += neighbor.x;
                    avgY += neighbor.y;
                    avgZ += neighbor.z;
                }
                avgX /= neighbors.size();
                avgY /= neighbors.size();
                avgZ /= neighbors.size();

                // Смещаем: newPos = (1 - factor) * oldPos + factor * avgPos
                float newX = (1 - smoothingFactor) * vertex.x + smoothingFactor * avgX;
                float newY = (1 - smoothingFactor) * vertex.y + smoothingFactor * avgY;
                float newZ = (1 - smoothingFactor) * vertex.z + smoothingFactor * avgZ;
                newPositions.put(vertex, new Point3D(newX, newY, newZ));
            }

            for (Triangle tri : triangles) {
                if (boundaryVertices.contains(tri.v1)) tri.v1 = newPositions.get(tri.v1);
                if (boundaryVertices.contains(tri.v2)) tri.v2 = newPositions.get(tri.v2);
                if (boundaryVertices.contains(tri.v3)) tri.v3 = newPositions.get(tri.v3);
            }

            for (int i = 0; i < vertices.size(); i++) {
                Point3D v = vertices.get(i);
                if (boundaryVertices.contains(v)) {
                    vertices.set(i, newPositions.get(v));
                }
            }
        }

        computeNormals();
        System.out.println("Boundary smoothing completed: vertices=" + vertices.size() + ", triangles=" + triangles.size());
    }


    public void computeNormals() {
        for (Triangle triangle : triangles) {
            Point3D p1 = triangle.v1;
            Point3D p2 = triangle.v2;
            Point3D p3 = triangle.v3;

            double uX = p2.x - p1.x;
            double uY = p2.y - p1.y;
            double uZ = p2.z - p1.z;

            double vX = p3.x - p1.x;
            double vY = p3.y - p1.y;
            double vZ = p3.z - p1.z;

            double nX = uY * vZ - uZ * vY;
            double nY = uZ * vX - uX * vZ;
            double nZ = uX * vY - uY * vX;

            double length = Math.sqrt(nX * nX + nY * nY + nZ * nZ);
            if (length > 0) {
                nX /= length;
                nY /= length;
                nZ /= length;
            } else {
                nX = 0;
                nY = 0;
                nZ = 1;
            }

            triangle.normal = new Point3D((float) nX,(float) nY, (float)nZ);
        }
    }

    public List<float[]> toChunkData() {
        List<float[]> chunkData = new ArrayList<>();
        for (Triangle t : triangles) {
            float[] triangleData = new float[18];
            triangleData[0] = (float) t.v1.x;
            triangleData[1] = (float) t.v1.y;
            triangleData[2] = (float) t.v1.z;
            Point3D normal = t.normal != null ? t.normal : new Point3D(0, 0, 1);
            triangleData[3] = (float) normal.x;
            triangleData[4] = (float) normal.y;
            triangleData[5] = (float) normal.z;
            triangleData[6] = (float) t.v2.x;
            triangleData[7] = (float) t.v2.y;
            triangleData[8] = (float) t.v2.z;
            triangleData[9] = (float) normal.x;
            triangleData[10] = (float) normal.y;
            triangleData[11] = (float) normal.z;
            triangleData[12] = (float) t.v3.x;
            triangleData[13] = (float) t.v3.y;
            triangleData[14] = (float) t.v3.z;
            triangleData[15] = (float) normal.x;
            triangleData[16] = (float) normal.y;
            triangleData[17] = (float) normal.z;
            chunkData.add(triangleData);
        }
        return chunkData;
    }
}
