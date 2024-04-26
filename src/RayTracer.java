import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class RayTracer {
    private static final Color SKY_COLOR = new Color(0, 247, 255, 255);;
    private static final Color RAY_COLOR = new Color(255, 255, 255, 255);
    private static final Color FLAT_COLOR = new Color(62, 255, 107, 255);
    private static final Color SPHERE_COLOR = new Color(255, 0, 0, 255);
    public static void main(String[] args) {
        Runtime runtime = Runtime.getRuntime();
        double memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        int width = 2000;
        int height = 1500;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Определение позиции камеры
        Vector3 cameraPos = new Vector3(0, 0, 0);

        // Определение поверхности под сферой (плоскость)
        Plane plane = new Plane(new Vector3(0, 1, 0), -1.5, FLAT_COLOR);

        // Определение сферы
        Sphere sphere = new Sphere(new Vector3(0, 0, -5), 1, SPHERE_COLOR, 0.15, 0.2);

        // Определение источника света
        Vector3 lightPos = new Vector3(1, 1, -3);

        // Обьявление параметров
        double minT, x, y, tPlane;
        Color pixelColor;

        long time = System.currentTimeMillis();

        // Трассировка
        // TODO : сделать отражения от сферы (чтобы ее отражение было видно на Plane'e)
        for (int j = 0; j < height; j++) {
            y = (1 - 2 * (j + 0.5) / (double) height);
            for (int i = 0; i < width; i++) {
                x = (2 * (i + 0.5) / (double) width - 1);
                Ray ray = new Ray(cameraPos, new Vector3(x, y, -1).normalize());

                // Переменные для хранения ближайшего пересечения и цвета пикселя
                minT = Double.POSITIVE_INFINITY;
                pixelColor = SKY_COLOR;

                // Проверяем пересечение с плоскостью, только если пока не найдено ближайшее пересечение
                tPlane = plane.intersect(ray);
                if (tPlane > 0 && tPlane < minT) {
                    minT = tPlane;
                    // Проверяем, видима ли точка на плоскости из источника света и за сферой относительно источника света
                    if (!isShadowed(ray, sphere, lightPos, plane) && lightPos.subtract(ray.origin.add(ray.direction.scale(tPlane))).dot(sphere.center.subtract(lightPos)) > 0) {
                        pixelColor = plane.getColor(); // Освещенная точка
                    } else {
                        pixelColor = plane.getColor().darker(); // Тень от сферы
                    }
                }

                // Проверяем пересечение с сферой только если плоскость не пересекается или пересечение сферы ближе
                double tSphere = sphere.intersect(ray);
                if (tSphere > 0 && tSphere < minT) {
                    minT = tSphere;
                    pixelColor = trace(ray, sphere, lightPos, RAY_COLOR);
                }

                // Устанавливаем цвет пикселя в изображении
                image.setRGB(i, j, pixelColor.getRGB());
            }
        }

        // С_Т_А_Т_И_С_Т_И_Ч_Е_С_К_И_Е Д_А_Н_Н_Ы_Е
        {
            System.out.println("Время рендеринга: " + (double)(System.currentTimeMillis() - time) / 1000 + " c.");
            double memoryUsed = runtime.totalMemory() - runtime.freeMemory() - memoryBefore;
            System.out.println("Затраченная память: " + String.format("%.3f",memoryUsed / 1024 / 1024) + " Мб.");
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            // Получаем использование памяти Java-программой
            long heapMemoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long nonHeapMemoryUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

            System.out.println("Использование кучи (heap) памяти: " + String.format("%.3f", (double)heapMemoryUsed / 1024 / 1024) + " Мб.");
            System.out.println("Использование некучи (non-heap) памяти: " + String.format("%.3f", (double)nonHeapMemoryUsed / 1024 / 1024) + " Мб.");
        }

        // Сохранение картинки
        try {
            File output = new File("ray_tracing_output.png");
            ImageIO.write(image, "png", output);
            System.out.println("Image saved successfully.");
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }

    // Основная логика трассировки лучей
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
            double red = Math.min(255, (int) (sphere.color.getRed() * (1 - sphere.absorption) * intensity * shadowFactor * lightIntensity + sphere.reflection * lightColor.getRed()));
            double green = Math.min(255, (int) (sphere.color.getGreen() * (1 - sphere.absorption) * intensity * shadowFactor * lightIntensity + sphere.reflection * lightColor.getGreen()));
            double blue = Math.min(255, (int) (sphere.color.getBlue() * (1 - sphere.absorption) * intensity * shadowFactor * lightIntensity + sphere.reflection * lightColor.getBlue()));

            return new Color((int) red, (int) green, (int) blue);
        } else {
            return SKY_COLOR;
        }
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
