import java.awt.*;
import java.util.ArrayList;
import java.util.List;

class Cube {
    Vector3 minPoint; // Минимальная точка куба
    Vector3 maxPoint; // Максимальная точка куба
    Color color;
    double absorption; // Коэффициент поглощения
    double reflection; // Коэффициент отражения
    public static List<Triangle> triangles = new ArrayList<>();

    Cube(Vector3 minPoint, Vector3 maxPoint, Color color, double reflection, double absorption, List<Triangle> triangles) {
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.color = color;
        this.absorption = absorption;
        this.reflection = reflection;
        this.triangles = triangles;
    }

    // Метод для поворота точки на заданный угол относительно другой точки
    private static Vector3 rotatePoint(Vector3 point, Vector3 center, double cosAngle, double sinAngle) {
        double x = cosAngle * (point.x - center.x) - sinAngle * (point.z - center.z) + center.x;
        double z = sinAngle * (point.x - center.x) + cosAngle * (point.z - center.z) + center.z;
        return new Vector3(x, point.y, z);
    }

    static double calculateShadowFactor(Vector3 intersectionPoint, Vector3 lightPos) {
        // Вычисляем расстояние от точки пересечения до источника света
        double distanceToLight = lightPos.subtract(intersectionPoint).length();

        // Вычисляем коэффициент тени на основе расстояния (чем дальше, тем теньнее)
        double maxDistance = 10.0; // Максимальное расстояние, на котором нет тени
        return Math.max(0, 1 - distanceToLight / maxDistance);
    }

    // Метод для поворота куба на заданный угол относительно оси y
    public static Vector3[] rotateCube(Vector3 center, double size, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double cosAngle = Math.cos(angleRadians);
        double sinAngle = Math.sin(angleRadians);

        // Вершины куба (до поворота)
        Vector3 frontTopLeft = new Vector3(center.x - size / 2, center.y + size / 2, center.z + size / 2);
        Vector3 frontTopRight = new Vector3(center.x + size / 2, center.y + size / 2, center.z + size / 2);
        Vector3 frontBottomLeft = new Vector3(center.x - size / 2, center.y - size / 2, center.z + size / 2);
        Vector3 frontBottomRight = new Vector3(center.x + size / 2, center.y - size / 2, center.z + size / 2);
        Vector3 backTopLeft = new Vector3(center.x - size / 2, center.y + size / 2, center.z - size / 2);
        Vector3 backTopRight = new Vector3(center.x + size / 2, center.y + size / 2, center.z - size / 2);
        Vector3 backBottomLeft = new Vector3(center.x - size / 2, center.y - size / 2, center.z - size / 2);
        Vector3 backBottomRight = new Vector3(center.x + size / 2, center.y - size / 2, center.z - size / 2);

        // Поворачиваем каждую вершину
        Vector3[] rotatedVertices = new Vector3[8];
        rotatedVertices[0] = rotatePoint(frontTopLeft, center, cosAngle, sinAngle);
        rotatedVertices[1] = rotatePoint(frontTopRight, center, cosAngle, sinAngle);
        rotatedVertices[2] = rotatePoint(frontBottomLeft, center, cosAngle, sinAngle);
        rotatedVertices[3] = rotatePoint(frontBottomRight, center, cosAngle, sinAngle);
        rotatedVertices[4] = rotatePoint(backTopLeft, center, cosAngle, sinAngle);
        rotatedVertices[5] = rotatePoint(backTopRight, center, cosAngle, sinAngle);
        rotatedVertices[6] = rotatePoint(backBottomLeft, center, cosAngle, sinAngle);
        rotatedVertices[7] = rotatePoint(backBottomRight, center, cosAngle, sinAngle);

        return rotatedVertices;
    }

    // * Основная логика трассировки лучей куба
    public static Color trace_cub_part(Ray ray, Triangle triangle, Vector3 lightPos, Color lightColor) {
        double t = triangle.intersect(ray);
        if (t > 0) {
            Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(t));
            Vector3 normal = triangle.getNormal();
            Vector3 toLight = lightPos.subtract(intersectionPoint).normalize();
            Vector3 toCamera = ray.origin.subtract(intersectionPoint).normalize();

            // Вычисляем интенсивность света и коэффициент тени
            double lightIntensity = Math.max(0, normal.dot(toLight));
            double shadowFactor = 1.0; // Простой случай без учета теней
            shadowFactor = calculateShadowFactor(intersectionPoint, lightPos);

            // Расчет блеска на поверхности объекта (модель Фонга)
            double ambient = 0.1; // Коэффициент окружающего освещения
            double diffuse = Math.max(0, normal.dot(toLight));
            Vector3 reflected = normal.scale(2 * normal.dot(toLight)).subtract(toLight).normalize();
            double specular = Math.pow(Math.max(0, reflected.dot(toCamera)), 32); // Коэффициент блеска

            // Общий цвет, учитывающий освещенность, тени и блеск
            double intensity = ambient + diffuse + specular;
            // Учитываем коэффициенты поглощения и отражения
            double red = Math.min(255, (int) (triangle.color.getRed() * (1 - triangle.absorption) * intensity * shadowFactor * lightIntensity + triangle.reflection * lightColor.getRed()));
            double green = Math.min(255, (int) (triangle.color.getGreen() * (1 - triangle.absorption) * intensity * shadowFactor * lightIntensity + triangle.reflection * lightColor.getGreen()));
            double blue = Math.min(255, (int) (triangle.color.getBlue() * (1 - triangle.absorption) * intensity * shadowFactor * lightIntensity + triangle.reflection * lightColor.getBlue()));

            return new Color((int) red, (int) green, (int) blue);
        } else {
            return RayTracer.SKY_COLOR;
        }
    }
}
