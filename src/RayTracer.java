import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RayTracer {
    public static void main(String[] args) {
        int width = 4000;
        int height = 3000;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Определение позиции камеры
        Vector3 cameraPos = new Vector3(0, 0, 0);

        // Определение сферы
        Sphere sphere = new Sphere(new Vector3(0, 0, -5), 1, Color.GREEN);

        // Определение источника света
        Vector3 lightPos = new Vector3(2, 2, -2);
        Color lightColor = Color.WHITE;

        // Трассировка
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double x = (2 * (i + 0.5) / (double) width - 1);
                double y = (1 - 2 * (j + 0.5) / (double) height);
                Ray ray = new Ray(cameraPos, new Vector3(x, y, -1).normalize());
                Color color = trace(ray, sphere, lightPos, lightColor);
                image.setRGB(i, j, color.getRGB());
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
            // Проверяем, находится ли точка в тени
            if (isShadowed(ray, sphere, lightPos)) {
                // Определение коэффициента тени
                double shadowFactor = calculateShadowFactor(ray, sphere, lightPos);

                // Смешиваем цвет тени с фоновым цветом или другими объектами
                Color shadowColor = Color.BLACK; // Цвет фона или другого объекта
                int red = (int) (shadowFactor * shadowColor.getRed());
                int green = (int) (shadowFactor * shadowColor.getGreen());
                int blue = (int) (shadowFactor * shadowColor.getBlue());
                return new Color(red, green, blue);
            } else {
                return lightColor; // Цвет освещенной точки
            }
        } else {
            return Color.BLACK;
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
        double shadowFactor = Math.max(0, 1 - distanceToLight / maxDistance);

        return shadowFactor;
    }
}
