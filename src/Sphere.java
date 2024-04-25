import java.awt.*;

class Sphere {
    Vector3 center;
    double radius;
    Color color;
    double absorption; // Коэффициент поглощения
    double reflection; // Коэффициент отражения

    Sphere(Vector3 center, double radius, Color color, double reflection) {
        this.center = center;
        this.radius = radius;
        this.color = color;
//        this.absorption = absorption;
        this.reflection = reflection;
    }

    // Тестирование пересечения
    double intersect(Ray ray) {
        Vector3 oc = ray.origin.subtract(center);
        double a = ray.direction.dot(ray.direction);
        double b = 2.0 * oc.dot(ray.direction);
        double c = oc.dot(oc) - radius * radius;
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return -1; // Нет
        } else {
            return (-b - Math.sqrt(discriminant)) / (2.0 * a);
        }
    }
}
