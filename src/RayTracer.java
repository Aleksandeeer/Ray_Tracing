import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RayTracer {
    private static final Color FLAT_COLOR = Color.WHITE;
    private static final Color RAY_COLOR = Color.WHITE;

    public static void main(String[] args) {
        int width = 2000;
        int height = 1500;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Определение позиции камеры
        Vector3 cameraPos = new Vector3(0, 0, 0);

        // Определение поверхности под сферой (плоскость)
        Plane plane = new Plane(new Vector3(0, 1, 0), -1.5, Color.GRAY);

        // Определение сферы
        Sphere sphere = new Sphere(new Vector3(0, 0, -5), 1, Color.RED, 0.1);

        // Определение источника света
        Vector3 lightPos = new Vector3(2, 2, -2);

        // Трассировка
        for (int j = 0; j < height; j++) {
            double y = (1 - 2 * (j + 0.5) / (double) height);
            for (int i = 0; i < width; i++) {
                double x = (2 * (i + 0.5) / (double) width - 1);
                Ray ray = new Ray(cameraPos, new Vector3(x, y, -1).normalize());

                // Переменные для хранения ближайшего пересечения и цвета пикселя
                double minT = Double.POSITIVE_INFINITY;
                Color pixelColor = FLAT_COLOR;

                // Проверяем пересечение с плоскостью
                double tPlane = plane.intersect(ray);
                if (tPlane > 0 && tPlane < minT) {
                    minT = tPlane;
                    pixelColor = plane.getColor(); // Цвет пикселя берем из цвета плоскости
                }

                // Проверяем пересечение с сферой
                double tSphere = sphere.intersect(ray);
                if (tSphere > 0 && tSphere < minT) {
                    minT = tSphere;
                    pixelColor = trace(ray, sphere, lightPos, RAY_COLOR);
                }

                // Устанавливаем цвет пикселя в изображении
                image.setRGB(i, j, pixelColor.getRGB());
            }
        }


        // Сохранение картинки
        try {
            File output = new File("output.png");
            ImageIO.write(image, "png", output);
            System.out.println("Image saved successfully.");
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }

    public static Color trace(Ray ray, Sphere sphere, Vector3 lightPos, Color lightColor) {
        double t = sphere.intersect(ray);
        if (t > 0) {
            Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(t));
            Vector3 normal = intersectionPoint.subtract(sphere.center).normalize();
            Vector3 toLight = lightPos.subtract(intersectionPoint).normalize();
            Vector3 toCamera = ray.origin.subtract(intersectionPoint).normalize();

            // Вычисляем интенсивность света и коэффициент тени
            double lightIntensity = calculateLightIntensity(ray, sphere, lightPos);
            double shadowFactor = calculateShadowFactor(ray, sphere, lightPos);

            // Расчет блеска на поверхности объекта (модель Фонга)
            double ambient = 0.1; // Коэффициент окружающего освещения
            double diffuse = Math.max(0, normal.dot(toLight));
            Vector3 reflected = normal.scale(2 * normal.dot(toLight)).subtract(toLight).normalize();
            double specular = Math.pow(Math.max(0, reflected.dot(toCamera)), 32); // Коэффициент блеска

            // Общий цвет, учитывающий освещенность, тени и блеск
            double intensity = ambient + diffuse + specular;

            // Учитываем коэффициенты поглощения и отражения
            double red = Math.min(255, (int) (sphere.color.getRed() * (1 - sphere.reflection) * intensity * shadowFactor * lightIntensity + sphere.reflection * lightColor.getRed()));
            double green = Math.min(255, (int) (sphere.color.getGreen() * (1 - sphere.reflection) * intensity * shadowFactor * lightIntensity + sphere.reflection * lightColor.getGreen()));
            double blue = Math.min(255, (int) (sphere.color.getBlue() * (1 - sphere.reflection) * intensity * shadowFactor * lightIntensity + sphere.reflection * lightColor.getBlue()));

            return new Color((int) red, (int) green, (int) blue);
        } else {
            return FLAT_COLOR;
        }
    }




    public static double intersectPlane(Ray ray) {
        // Проверяем, что луч направлен вниз по оси Z
        if (Math.abs(ray.direction.z) > 1e-6) { // Проверяем, что знаменатель не равен нулю
            // Вычисляем расстояние до плоскости
            return -ray.origin.z / ray.direction.z;
        } else {
            // Луч параллелен плоскости
            return -1;
        }
    }


    // Проверка на нахождение в тени
    public static boolean isShadowed(Ray ray, Sphere sphere, Vector3 lightPos) {
        // Создаем луч от точки пересечения до источника света
        Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(sphere.intersect(ray)));
        Ray shadowRay = new Ray(intersectionPoint, lightPos.subtract(intersectionPoint).normalize());

        // Проверяем, пересекает ли луч другие объекты перед достижением источника света
        double t = sphere.intersect(shadowRay);
        return t > 0 && t < 1; // Если есть пересечение, точка находится в тени
    }

    // Интенсивность света
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
}
