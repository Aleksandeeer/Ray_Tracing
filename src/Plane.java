import java.awt.*;

public class Plane {
    private Vector3 normal;
    private double distance;
    private Color color;

    public Plane(Vector3 normal, double distance, Color color) {
        this.normal = normal.normalize();
        this.distance = distance;
        this.color = color;
    }

    // Метод для определения пересечения луча с плоскостью
    public double intersect(Ray ray) {
        double denominator = normal.dot(ray.direction);
        if (Math.abs(denominator) > 1e-6) {
            double t = (distance - normal.dot(ray.origin)) / denominator;
            if (t > 0) {
                return t;
            }
        }
        return -1;
    }

    // Геттеры для получения цвета и нормали плоскости
    public Color getColor() {
        return color;
    }

    public Vector3 getNormal() {
        return normal;
    }
}