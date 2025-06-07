package working_project.marching_cubes;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Простая реализация KD-дерева для быстрого поиска ближайшей точки в 3D-пространстве.
 */
class KDTree {
    private static class Node {
        double[] point; // Координаты точки в узле
        int axis;       // Ось разделения (0=x, 1=y, 2=z)
        Node left, right; // Дочерние узлы (левое и правое поддерево)

        Node(double[] point, int axis) {
            this.point = point;
            this.axis = axis;
        }
    }

    private Node root;

    /**
     * Создаёт KD-дерево из массива точек.
     * @param points Массив 3D-точек
     */
    public KDTree(double[][] points) {

        root = buildTree(points, 0);
    }

    /**
     * Рекурсивно строит KD-дерево, разделяя точки по медиане.
     * @param points Точки для текущего поддерева
     * @param depth Текущая глубина (определяет ось разделения)
     * @return Корневой узел поддерева
     */
    private Node buildTree(double[][] points, int depth) {
        if (points.length == 0) return null;
        if (points.length == 1) return new Node(points[0], depth % 3);

        int axis = depth % 3; // Выбираем ось (x, y, z чередуются по глубине)
        Arrays.sort(points, Comparator.comparingDouble(a -> a[axis]));
        int mid = points.length / 2;
        Node node = new Node(points[mid], axis);
        node.left = buildTree(Arrays.copyOfRange(points, 0, mid), depth + 1);
        node.right = buildTree(Arrays.copyOfRange(points, mid + 1, points.length), depth + 1);
        return node;
    }

    /**
     * Класс для результата поиска ближайшей точки.
     */
    public static class NearestNeighbor {
        public double[] point; // Ближайшая точка
        public double distance; // Расстояние до неё

        NearestNeighbor(double[] point, double distance) {
            this.point = point;
            this.distance = distance;
        }
    }

    /**
     * Ищет ближайшую точку к заданной.
     * @param query Координаты запроса
     * @return Объект с ближайшей точкой и расстоянием
     */
    public NearestNeighbor nearest(double[] query) {
        NearestNeighbor nn = new NearestNeighbor(null, Double.POSITIVE_INFINITY); // Инициализируем с бесконечным расстоянием
        nearest(root, query, nn);
        return nn;
    }

    /**
     * Рекурсивно ищет ближайшую точку.
     * @param node Текущий узел
     * @param query Точка запроса
     * @param nn Результат поиска (обновляется)
     */
    private void nearest(Node node, double[] query, NearestNeighbor nn) {
        if (node == null) return;

        // Вычисляем расстояние до точки текущего узла
        double dist = distance(query, node.point);
        if (dist < nn.distance) {
            nn.point = node.point;
            nn.distance = dist;
        }

        int axis = node.axis; // Ось разделения текущего узла
        // Выбираем, в какое поддерево идти: левое, если query[axis] < node.point[axis]
        Node near = query[axis] < node.point[axis] ? node.left : node.right;
        Node far = query[axis] < node.point[axis] ? node.right : node.left;

        nearest(near, query, nn); // Сначала проверяем ближнее поддерево
        // Проверяем дальнее поддерево, если расстояние до разделяющей плоскости меньше текущего минимума
        double axisDist = Math.abs(query[axis] - node.point[axis]);
        if (axisDist < nn.distance) {
            nearest(far, query, nn);
        }
    }

    /**
     * Вычисляет евклидово расстояние между двумя точками.
     * @param p1 Первая точка
     * @param p2 Вторая точка
     * @return Расстояние
     */
    private double distance(double[] p1, double[] p2) {
        double dx = p1[0] - p2[0];
        double dy = p1[1] - p2[1];
        double dz = p1[2] - p2[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
