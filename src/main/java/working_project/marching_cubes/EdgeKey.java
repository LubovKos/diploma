package working_project.marching_cubes;

import java.util.Arrays;

/**
 * Класс для уникального представления ребра между двумя точками, чтобы избежать дублирования вершин.
 * Точки сортируются по координатам для консистентности при сравнении.
 */
class EdgeKey {
    final double[] p1;
    final double[] p2;

    /**
     * Создаёт ключ ребра, сортируя точки для уникальности.
     * @param p1 Координаты первой точки
     * @param p2 Координаты второй точки
     */
    EdgeKey(double[] p1, double[] p2) {
        // Сортируем точки, чтобы (p1,p2) и (p2,p1) считались одним ребром
        if (compare(p1, p2) > 0) {
            this.p1 = p2;
            this.p2 = p1;
        } else {
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    /**
     * Сравнивает две точки по координатам (лексикографически).
     * @param a Первая точка
     * @param b Вторая точка
     * @return -1, если a < b; 1, если a > b; 0, если равны
     */
    private int compare(double[] a, double[] b) {
        for (int i = 0; i < 3; i++) {
            if (a[i] < b[i]) return -1;
            if (a[i] > b[i]) return 1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdgeKey edgeKey = (EdgeKey) o;
        return Arrays.equals(p1, edgeKey.p1) && Arrays.equals(p2, edgeKey.p2);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(p1);
        result = 31 * result + Arrays.hashCode(p2);
        return result;
    }
}
