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

    // * Основная логика трассировки лучей сферы
    public static Color trace_sphere(Ray ray, Sphere sphere, Vector3 lightPos, Color lightColor) {
        double t = sphere.intersect(ray);
        if (t > 0) {
            Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(t));
            Vector3 normal = intersectionPoint.subtract(sphere.center).normalize();
            Vector3 toLight = lightPos.subtract(intersectionPoint).normalize();
            Vector3 toCamera = ray.origin.subtract(intersectionPoint).normalize();

            // Вычисляем интенсивность света и коэффициент тени
            double lightIntensity = Sphere.calculateLightIntensity(ray, sphere, lightPos);
            double shadowFactor = Sphere.calculateShadowFactor(ray, sphere, lightPos);

            // Расчет блеска на поверхности объекта (модель Фонга)
            double ambient = 0.1; // Коэффициент окружающего освещения
            double diffuse = Math.max(0, normal.dot(toLight));
            Vector3 reflected = normal.scale(2 * normal.dot(toLight)).subtract(toLight).normalize();
            double specular = Math.pow(Math.max(0, reflected.dot(toCamera)), 32); // Коэффициент блеска

            // Общий цвет, учитывающий освещенность, тени и блеск
            double intensity = ambient + diffuse + specular;

            // Учитываем коэффициенты поглощения и отражения
            double red = Math.min(255, (int) (sphere.color.getRed() * (1 - sphere.absorption) * intensity * shadowFactor * lightIntensity + sphere.reflection * lightColor.getRed()));
            double green = Math.min(255, (int) (sphere.color.getGreen() * (1 - sphere.absorption) * intensity * shadowFactor * lightIntensity + sphere.reflection * lightColor.getGreen()));
            double blue = Math.min(255, (int) (sphere.color.getBlue() * (1 - sphere.absorption) * intensity * shadowFactor * lightIntensity + sphere.reflection * lightColor.getBlue()));

            return new Color((int) red, (int) green, (int) blue);
        } else {
            return RayTracer.SKY_COLOR;
        }
    }

    public static boolean isShadowed(Ray ray, Sphere sphere, Vector3 lightPos, Plane plane) {
        Vector3 intersectionPoint = ray.pointAtParameter(sphere.intersect(ray));
        Vector3 shadowRayDirection = lightPos.subtract(intersectionPoint).normalize();
        Ray shadowRay = new Ray(intersectionPoint, shadowRayDirection);

        // Check if the shadow ray intersects the sphere or plane
        double tSphereShadow = sphere.intersect(shadowRay);
        double tPlaneShadow = plane.intersect(shadowRay);

        return (tSphereShadow > 0 && tSphereShadow < lightPos.subtract(intersectionPoint).length()) ||
                (tPlaneShadow > 0 && tPlaneShadow < lightPos.subtract(intersectionPoint).length());
    }

}
