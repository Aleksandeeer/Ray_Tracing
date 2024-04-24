import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RayTracer {
    public static void main(String[] args) {
        int width = 800;
        int height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Определение позиции камеры
        Vector3 cameraPos = new Vector3(0, 0, 0);

        // Определение сферы
        Sphere sphere = new Sphere(new Vector3(0, 0, -5), 1, Color.GREEN);

        // Трассировка
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                double x = (2 * (i + 0.5) / (double) width - 1);
                double y = (1 - 2 * (j + 0.5) / (double) height);
                Ray ray = new Ray(cameraPos, new Vector3(x, y, -1).normalize());
                Color color = trace(ray, sphere);
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

    public static Color trace(Ray ray, Sphere sphere) {
        double t = sphere.intersect(ray);
        if (t > 0) {
            return sphere.color;
        } else {
            return Color.BLACK;
        }
    }
}