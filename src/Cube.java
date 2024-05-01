import java.awt.*;

class Cube {
    Vector3 minPoint; // Минимальная точка куба
    Vector3 maxPoint; // Максимальная точка куба
    Color color;
    double absorption; // Коэффициент поглощения
    double reflection; // Коэффициент отражения

    Cube(Vector3 minPoint, Vector3 maxPoint, Color color, double reflection, double absorption) {
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.color = color;
        this.absorption = absorption;
        this.reflection = reflection;
    }

    // Тестирование пересечения
    double intersect(Ray ray) {
        double tMin = (minPoint.x - ray.origin.x) / ray.direction.x;
        double tMax = (maxPoint.x - ray.origin.x) / ray.direction.x;

        if (tMin > tMax) {
            double temp = tMin;
            tMin = tMax;
            tMax = temp;
        }

        double tyMin = (minPoint.y - ray.origin.y) / ray.direction.y;
        double tyMax = (maxPoint.y - ray.origin.y) / ray.direction.y;

        if (tyMin > tyMax) {
            double temp = tyMin;
            tyMin = tyMax;
            tyMax = temp;
        }

        if ((tMin > tyMax) || (tyMin > tMax))
            return -1;

        if (tyMin > tMin)
            tMin = tyMin;

        if (tyMax < tMax)
            tMax = tyMax;

        double tzMin = (minPoint.z - ray.origin.z) / ray.direction.z;
        double tzMax = (maxPoint.z - ray.origin.z) / ray.direction.z;

        if (tzMin > tzMax) {
            double temp = tzMin;
            tzMin = tzMax;
            tzMax = temp;
        }

        if ((tMin > tzMax) || (tzMin > tMax))
            return -1;

        if (tzMin > tMin)
            tMin = tzMin;

        if (tzMax < tMax)
            tMax = tzMax;

        return tMin > 0 ? tMin : tMax;
    }

    // Метод для вычисления нормали к поверхности куба в заданной точке
    public static Vector3 getNormal(Vector3 intersectionPoint, Cube cube) {
        Vector3 minPoint = cube.minPoint;
        Vector3 maxPoint = cube.maxPoint;
        Vector3 center = minPoint.add(maxPoint).scale(0.5);
        Vector3 d = maxPoint.subtract(minPoint).normalize();

        Vector3 normal = intersectionPoint.subtract(center);
        double minDist = Math.abs(normal.x);
        double dist = Math.abs(normal.y);
        if (dist < minDist) {
            minDist = dist;
            normal = new Vector3(0, normal.y, normal.z);
        }
        dist = Math.abs(normal.z);
        if (dist < minDist) {
            normal = new Vector3(0, 0, normal.z);
        }

        return normal.normalize();
    }


    public static boolean isShadowed(Ray ray, Cube cube, Vector3 lightPos, Plane plane) {
        // Пересекаем луч с кубом
        double tCube = cube.intersect(ray);

        // Если пересечения нет, точка находится не в тени
        if (tCube <= 0) {
            return false;
        }

        // Находим точку пересечения с кубом
        Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(tCube));

        // Вычисляем виртуальный луч от точки пересечения до плоскости
        Ray virtualRay = new Ray(intersectionPoint, plane.getNormal());

        // Находим точку пересечения виртуального луча с плоскостью
        double tPlane = plane.intersect(virtualRay);

        // Проверяем, находится ли точка пересечения внутри определенной области вокруг куба
        double shadowSize = 0.5; // Размер области вокруг куба, в которой будет тень

        return (tPlane > 0) && (tPlane < shadowSize);
    }

    // Метод для вычисления интенсивности света для куба
    static double calculateLightIntensity(Vector3 intersectionPoint, Vector3 lightPos) {
        // Вычисляем расстояние от точки пересечения до источника света
        double distanceToLight = lightPos.subtract(intersectionPoint).length();

        // Используем обратную зависимость расстояния для определения интенсивности света
        return 1.0 / distanceToLight;
    }

    // Метод для определения коэффициента тени для куба
    static double calculateShadowFactor(Vector3 intersectionPoint, Vector3 lightPos) {
        // Вычисляем расстояние от точки пересечения до источника света
        double distanceToLight = lightPos.subtract(intersectionPoint).length();

        // Вычисляем коэффициент тени на основе расстояния (чем дальше, тем теньнее)
        double maxDistance = 10.0; // Максимальное расстояние, на котором нет тени
        return Math.max(0, 1 - distanceToLight / maxDistance);
    }


    public static Vector3 rotatePoint(Vector3 point, double angle, Vector3 axis) {
        double radians = Math.toRadians(angle);
        double cosTheta = Math.cos(radians);
        double sinTheta = Math.sin(radians);

        double x = point.x;
        double y = point.y;

        // Поворачиваем точку вокруг оси Z
        double newX = x * cosTheta - y * sinTheta;
        double newY = x * sinTheta + y * cosTheta;

        return new Vector3(newX, newY, point.z);
    }

}
