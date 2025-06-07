package working_project.rendering;

public class Triangle {
    public Point3D v1;
    public Point3D v2;
    public Point3D v3;
    public Point3D normal;

    public Triangle(Point3D v1, Point3D v2, Point3D v3, Point3D normal) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
        this.normal = normal;
    }

    public Triangle(Point3D v1, Point3D v2, Point3D v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }
}
