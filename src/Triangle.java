import java.awt.*;

class Triangle {
    Vector3 v0, v1, v2; // Точки треугольника
    Color color;
    double absorption; // Коэффициент поглощения
    double reflection; // Коэффициент отражения

    Triangle(Vector3 v0, Vector3 v1, Vector3 v2, Color color, double reflection, double absorption) {
        this.v0 = v0;
        this.v1 = v1;
        this.v2 = v2;
        this.color = color;
        this.absorption = absorption;
        this.reflection = reflection;
    }

    // Тестирование пересечения
    double intersect(Ray ray) {
        Vector3 edge1 = v1.subtract(v0);
        Vector3 edge2 = v2.subtract(v0);
        Vector3 h = ray.direction.cross(edge2);
        double a = edge1.dot(h);

        if (a > -1e-6 && a < 1e-6)
            return -1; // Луч параллелен плоскости треугольника

        double f = 1.0 / a;
        Vector3 s = ray.origin.subtract(v0);
        double u = f * (s.dot(h));

        if (u < 0.0 || u > 1.0)
            return -1;

        Vector3 q = s.cross(edge1);
        double v = f * ray.direction.dot(q);

        if (v < 0.0 || u + v > 1.0)
            return -1;

        double t = f * edge2.dot(q);
        return t > 1e-8 ? t : -1; // Пересечение есть, если t > 0
    }

    // Метод для вычисления нормали к поверхности треугольника
    Vector3 getNormal() {
        return v1.subtract(v0).cross(v2.subtract(v0)).normalize();
    }


}

