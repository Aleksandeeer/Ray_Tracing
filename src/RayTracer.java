import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class RayTracer {
    public static final Color SKY_COLOR = new Color(0, 247, 255, 255);
    private static final Color RAY_COLOR = new Color(255, 255, 255, 255);
    private static final Color FLAT_COLOR = new Color(62, 255, 107, 255);
    private static final Color SPHERE_COLOR = new Color(255, 0, 0, 255);
    public static final Color CUBE_COLOR = new Color(208, 100, 253, 255);

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
                data_map.get("plane_pos")[3], FLAT_COLOR);

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
                        pixelColor = Sphere.trace_sphere(ray, sphere, lightPos, RAY_COLOR);
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
        Cube cube = new Cube(new Vector3(-1.83, -1, -2.41), new Vector3(-0.41, 1, -5.54), Color.BLUE, 0.15, 0.2, init_triangles());

        double y, x, minT;
        Color pixelColor;
        double tPlane;

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

                    // Проверка, отбрасывает ли кубик тень на эту точку
                    Vector3 intersectionPoint = ray.origin.add(ray.direction.scale(tPlane));
                    Ray shadowRay = new Ray(intersectionPoint, lightPos.subtract(intersectionPoint).normalize());

                    boolean inShadow = false;
                    for (Triangle triangle : cube.triangles) {
                        if (triangle.intersect(shadowRay) > 0) {
                            inShadow = true;
                            break;
                        }
                    }

                    if (inShadow) {
                        pixelColor = plane.getColor().darker();
                    } else {
                        pixelColor = plane.getColor();
                    }
                }

                // Проверяем пересечение с треугольниками
                for (Triangle triangle : cube.triangles) {
                    double tTriangle = triangle.intersect(ray);
                    if (tTriangle > 0 && tTriangle < minT) {
                        minT = tTriangle;
                        pixelColor = Cube.trace_cub_part(ray, triangle, lightPos, Color.WHITE);
                    }
                }

                // Устанавливаем цвет пикселя в изображении
                cubeImage.setRGB(i, j, pixelColor.getRGB());
            }
        }

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

    public static List<Triangle> init_triangles() {
        double size = 2.0;
        Vector3 center = new Vector3(0, 0, -5);
        Vector3[] vertices = Cube.rotateCube(center, size, 45); // Поворот на 20 градусов относительно оси y

        List<Triangle> triangles = new ArrayList<>();

        double reflection = 0.25;
        double absortion = 0.2;
        // Грани куба
        // Передняя
        triangles.add(new Triangle(vertices[0], vertices[2], vertices[3], RayTracer.CUBE_COLOR, reflection, absortion));
        triangles.add(new Triangle(vertices[0], vertices[3], vertices[1], RayTracer.CUBE_COLOR, reflection, absortion));
        // Задняя
        triangles.add(new Triangle(vertices[4], vertices[6], vertices[7], RayTracer.CUBE_COLOR, reflection, absortion));
        triangles.add(new Triangle(vertices[4], vertices[7], vertices[5], RayTracer.CUBE_COLOR, reflection, absortion));
        // Верхняя
        triangles.add(new Triangle(vertices[0], vertices[4], vertices[5], RayTracer.CUBE_COLOR, reflection, absortion));
        triangles.add(new Triangle(vertices[0], vertices[5], vertices[1], RayTracer.CUBE_COLOR, reflection, absortion));
        // Нижняя
        triangles.add(new Triangle(vertices[2], vertices[6], vertices[7], RayTracer.CUBE_COLOR, reflection, absortion));
        triangles.add(new Triangle(vertices[2], vertices[7], vertices[3], RayTracer.CUBE_COLOR, reflection, absortion));
        // Левая
        triangles.add(new Triangle(vertices[0], vertices[6], vertices[4], Color.WHITE, reflection, absortion));
        triangles.add(new Triangle(vertices[0], vertices[2], vertices[6], Color.WHITE, reflection, absortion));
        // Правая
        triangles.add(new Triangle(vertices[1], vertices[7], vertices[5], RayTracer.CUBE_COLOR, reflection, absortion));
        triangles.add(new Triangle(vertices[1], vertices[3], vertices[7], RayTracer.CUBE_COLOR, reflection, absortion));

        center = new Vector3(5, 5, -7);
        vertices = Cube.rotateCube(center, size, 45);
        // Грани куба
        // Передняя
        triangles.add(new Triangle(vertices[0], vertices[2], vertices[3], Color.YELLOW, reflection, absortion));
        triangles.add(new Triangle(vertices[0], vertices[3], vertices[1], Color.YELLOW, reflection, absortion));
        // Задняя
        triangles.add(new Triangle(vertices[4], vertices[6], vertices[7], RayTracer.CUBE_COLOR, reflection, absortion));
        triangles.add(new Triangle(vertices[4], vertices[7], vertices[5], RayTracer.CUBE_COLOR, reflection, absortion));
        // Верхняя
        triangles.add(new Triangle(vertices[0], vertices[4], vertices[5], RayTracer.CUBE_COLOR, reflection, absortion));
        triangles.add(new Triangle(vertices[0], vertices[5], vertices[1], RayTracer.CUBE_COLOR, reflection, absortion));
        // Нижняя
        triangles.add(new Triangle(vertices[2], vertices[6], vertices[7], Color.RED, reflection, absortion));
        triangles.add(new Triangle(vertices[2], vertices[7], vertices[3], Color.RED, reflection, absortion));
        // Левая
        triangles.add(new Triangle(vertices[0], vertices[6], vertices[4], Color.WHITE, reflection, absortion));
        triangles.add(new Triangle(vertices[0], vertices[2], vertices[6], Color.WHITE, reflection, absortion));
        // Правая
        triangles.add(new Triangle(vertices[1], vertices[7], vertices[5], Color.RED, reflection, absortion));
        triangles.add(new Triangle(vertices[1], vertices[3], vertices[7], Color.RED, reflection, absortion));


        return triangles;
    }
}
