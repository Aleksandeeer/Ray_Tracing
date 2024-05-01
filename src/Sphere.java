import java.awt.*;

class Sphere {
    Vector3 center;
    double radius;
    Color color;
    double absorption; // Коэффициент поглощения
    double reflection; // Коэффициент отражения

    Sphere(Vector3 center, double radius, Color color, double reflection, double absorption) {
        this.center = center;
        this.radius = radius;
        this.color = color;
        this.absorption = absorption;
        this.reflection = reflection;
    }

    double getRadius() {
        return radius;
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

    public Vector3 getCenter() {
        return center;
    }

    public static double calculateLightIntensity(Ray ray, Sphere sphere, Vector3 lightPos) {
        // Вычисляем расстояние от точки пересечения до источника света
        Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(sphere.intersect(ray)));
        double distanceToLight = lightPos.subtract(intersectionPoint).length();

        // Используем обратную зависимость расстояния для определения интенсивности света
        return 1.0 / distanceToLight;
    }

    // Определение коэффицента тени
    public static double calculateShadowFactor(Ray ray, Sphere sphere, Vector3 lightPos) {
        // Вычисляем расстояние от точки пересечения до источника света
        Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(sphere.intersect(ray)));
        double distanceToLight = lightPos.subtract(intersectionPoint).length();

        // Вычисляем коэффициент тени на основе расстояния (чем дальше, тем теньнее)
        double maxDistance = 10.0; // Максимальное расстояние, на котором нет тени

        return Math.max(0, 1 - distanceToLight / maxDistance);
    }

    // Проверка на нахождение в тени
    public static boolean isShadowed(Ray ray, Sphere sphere, Vector3 lightPos, Plane plane) {
        // Пересекаем луч с сферой
        double tSphere = sphere.intersect(ray);

        // Если пересечения нет, точка находится не в тени
        if (tSphere <= 0) {
            return false;
        }

        // Находим точку пересечения со сферой
        Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(tSphere));

        // Вычисляем виртуальный луч от точки пересечения до плоскости
        Ray virtualRay = new Ray(intersectionPoint, plane.getNormal());

        // Находим точку пересечения виртуального луча с плоскостью
        double tPlane = plane.intersect(virtualRay);

        // Проверяем, находится ли точка пересечения внутри определенной области вокруг сферы
        double shadowRadius = 1.5; // Радиус области вокруг сферы, в которой будет тень
        Vector3 sphereCenterToIntersection = intersectionPoint.subtract(sphere.getCenter());
        double distanceToSphereCenter = sphereCenterToIntersection.length();

        return tPlane > 0 && distanceToSphereCenter < sphere.getRadius() + shadowRadius;
    }
}
