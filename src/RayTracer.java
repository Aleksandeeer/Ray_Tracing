import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RayTracer {
    private static final Color SKY_COLOR = new Color(0, 247, 255, 255);
    private static final Color RAY_COLOR = new Color(255, 255, 255, 255);
    private static final Color FLAT_COLOR = new Color(62, 255, 107, 255);
    private static final Color SPHERE_COLOR = new Color(255, 0, 0, 255);

    CacheManager cacheManager = CacheManager.getInstance();

    public static void main(String[] args) {
        // * Д_А_Н_Н_Ы_Е И_З Ф_А_Й_Л_А
        Map<String, double[]> data_map = new HashMap<>();

        ExecutorService fileReaderExecutor = Executors.newSingleThreadExecutor();
        fileReaderExecutor.submit(() -> {
            try {
                Scanner scanner = new Scanner(new File("D:\\JAVA\\Ray_tracing\\src\\input.txt"), StandardCharsets.UTF_8);

                while (scanner.hasNextLine()) {
                    String[] temp = scanner.nextLine().split(" ");
                    String key = temp[0];
                    double[] values = new double[temp.length - 1];

                    for (int i = 1; i < temp.length; i++) {
                        values[i - 1] = Double.parseDouble(temp[i]);
                    }

                    data_map.put(key, values);
                }

                scanner.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        fileReaderExecutor.shutdown();
        try {
            fileReaderExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // ? Разрешение изображения
        int width = (int)data_map.get("width")[0], height = (int)data_map.get("height")[0];

        // ? Создание двух отдельных изображений для куба и сферы
        BufferedImage sphereImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        BufferedImage cubeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // ? Определение позиции камеры
        Vector3 cameraPos = new Vector3(data_map.get("camera_pos")[0], data_map.get("camera_pos")[1], data_map.get("camera_pos")[2]);

        // ? Определение поверхности под сферой (плоскость)
        Plane plane = new Plane(new Vector3(data_map.get("plane_pos")[0], data_map.get("plane_pos")[1], data_map.get("plane_pos")[2]),
                -1.5, FLAT_COLOR);

        // ? Определение сферы
        Sphere sphere = new Sphere(new Vector3(data_map.get("sphere")[0], data_map.get("sphere")[1], data_map.get("sphere")[2]),
                data_map.get("sphere")[3], SPHERE_COLOR, data_map.get("sphere")[4], data_map.get("sphere")[5]);

        // ? Определение источника света
        Vector3 lightPos = new Vector3(data_map.get("light_pos")[0], data_map.get("light_pos")[1], data_map.get("light_pos")[2]);

        // * О_С_Н_О_В_Н_О_Й К_О_Д
        Runtime runtime = Runtime.getRuntime();
        double memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        long time = System.currentTimeMillis();

        // ! Т_Р_А_С_С_И_Р_О_В_К_А С_Ф_Е_Р_Ы
        ExecutorService rayTracingExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int j = 0; j < height; j++) {
            final int y = j;
            rayTracingExecutor.submit(() -> {
                double yVal = (1 - 2 * (y + 0.5) / (double) height);
                for (int i = 0; i < width; i++) {
                    double xVal = (2 * (i + 0.5) / (double) width - 1);
                    Ray ray = new Ray(cameraPos, new Vector3(xVal, yVal, -1).normalize());

                    double minT = Double.POSITIVE_INFINITY;
                    Color pixelColor = SKY_COLOR;

                    double tPlane = plane.intersect(ray);
                    if (tPlane > 0 && tPlane < minT) {
                        minT = tPlane;
                        if (!Sphere.isShadowed(ray, sphere, lightPos, plane) && lightPos.subtract(ray.origin.add(ray.direction.scale(tPlane))).dot(sphere.center.subtract(lightPos)) > 0) {
                            pixelColor = plane.getColor();
                        } else {
                            pixelColor = plane.getColor().darker();
                        }
                    }

                    double tSphere = sphere.intersect(ray);
                    if (tSphere > 0 && tSphere < minT) {
                        minT = tSphere;
                        pixelColor = trace_sphere(ray, sphere, lightPos, RAY_COLOR);
                    }

                    sphereImage.setRGB(i, y, pixelColor.getRGB());
                }
            });
        }

        rayTracingExecutor.shutdown();
        try {
            rayTracingExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // ! Т_Р_А_С_С_И_Р_О_В_К_А К_У_Б_А
//        Cube cube = new Cube(new Vector3(-1, -1, -3), new Vector3(1, 1, -5), Color.BLUE, 0.15, 0.2);
//
//        // Трассировка куба
//        for (int j = 0; j < height; j++) {
//            y = (1 - 2 * (j + 0.5) / (double) height);
//            for (int i = 0; i < width; i++) {
//                x = (2 * (i + 0.5) / (double) width - 1);
//                Ray ray = new Ray(cameraPos, new Vector3(x, y, -1).normalize());
//
//                // Переменные для хранения ближайшего пересечения и цвета пикселя
//                minT = Double.POSITIVE_INFINITY;
//                pixelColor = SKY_COLOR;
//
//                // Проверяем пересечение с плоскостью, только если пока не найдено ближайшее пересечение
//                tPlane = plane.intersect(ray);
//                if (tPlane > 0 && tPlane < minT) {
//                    minT = tPlane;
//                    if (!Cube.isShadowed(ray, cube, lightPos, plane) && lightPos.subtract(ray.origin.add(ray.direction.scale(tPlane))).dot(cube.maxPoint.subtract(lightPos)) > 0) {
//                        pixelColor = plane.getColor(); // Освещенная точка
//                    } else {
//                        pixelColor = plane.getColor().darker(); // Тень от куба
//                    }
//                }
//
//                // Проверяем пересечение с кубом только если плоскость не пересекается или пересечение куба ближе
//                double tCube = cube.intersect(ray);
//                if (tCube > 0 && tCube < minT) {
//                    minT = tCube;
//                    pixelColor = trace_cub(ray, cube, lightPos, RAY_COLOR);
//                }
//
//                // Устанавливаем цвет пикселя в изображении
//                cubeImage.setRGB(i, j, pixelColor.getRGB());
//            }
//        }

        // * С_Т_А_Т_И_С_Т_И_Ч_Е_С_К_И_Е Д_А_Н_Н_Ы_Е
        {
            System.out.println("Время рендеринга: " + (double)(System.currentTimeMillis() - time) / 1000 + " c.");
            double memoryUsed = runtime.totalMemory() - runtime.freeMemory() - memoryBefore;
            System.out.println("Затраченная память: " + String.format("%.3f",memoryUsed / 1024 / 1024) + " Мб.");
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

            // Получаем использование памяти Java-программой
            long heapMemoryUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long nonHeapMemoryUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

            System.out.println("Использование heap памяти: " + String.format("%.3f", (double)heapMemoryUsed / 1024 / 1024) + " Мб.");
            System.out.println("Использование non-heap памяти: " + String.format("%.3f", (double)nonHeapMemoryUsed / 1024 / 1024) + " Мб.");
        }

        // * С_О_Х_Р_А_Н_Е_Н_И_Е К_А_Р_Т_И_Н_К_И
        try {
            File output = new File("Sphere_tracing.png");
            ImageIO.write(sphereImage, "png", output);
            System.out.println("Sphere image saved successfully.");

            output = new File("Cube_tracing.png");
            ImageIO.write(cubeImage, "png", output);
            System.out.println("Cube image saved successfully");
        } catch (IOException e) {
            System.err.println("Failed to save image: " + e.getMessage());
        }
    }

    // * Основная логика трассировки лучей
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
            return SKY_COLOR;
        }
    }

    // * Основная логика трассировки лучей для куба
    public static Color trace_cub(Ray ray, Cube cube, Vector3 lightPos, Color lightColor) {
        double t = cube.intersect(ray);
        if (t > 0) {
            Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(t));
            Vector3 normal = Cube.getNormal(intersectionPoint, cube);
            Vector3 toLight = lightPos.subtract(intersectionPoint).normalize();
            Vector3 toCamera = ray.origin.subtract(intersectionPoint).normalize();

            // ? Вычисляем интенсивность света и коэффициент тени
            double lightIntensity = Cube.calculateLightIntensity(intersectionPoint, lightPos);
            double shadowFactor = Cube.calculateShadowFactor(intersectionPoint, lightPos);


            // ? Расчет блеска на поверхности объекта (модель Фонга)
            double ambient = 0.1; // ? Коэффициент окружающего освещения
            double diffuse = Math.max(0, normal.dot(toLight));
            Vector3 reflected = normal.scale(2 * normal.dot(toLight)).subtract(toLight).normalize();
            double specular = Math.pow(Math.max(0, reflected.dot(toCamera)), 32); // ? Коэффициент блеска

            // ? Общий цвет, учитывающий освещенность, тени и блеск
            double intensity = ambient + diffuse + specular;

            // ? Учитываем коэффициенты поглощения и отражения
            double red = Math.min(255, (int) (cube.color.getRed() * (1 - cube.absorption) * intensity * shadowFactor * lightIntensity + cube.reflection * lightColor.getRed()));
            double green = Math.min(255, (int) (cube.color.getGreen() * (1 - cube.absorption) * intensity * shadowFactor * lightIntensity + cube.reflection * lightColor.getGreen()));
            double blue = Math.min(255, (int) (cube.color.getBlue() * (1 - cube.absorption) * intensity * shadowFactor * lightIntensity + cube.reflection * lightColor.getBlue()));

            return new Color((int) red, (int) green, (int) blue);
        } else {
            return SKY_COLOR;
        }
    }
}
