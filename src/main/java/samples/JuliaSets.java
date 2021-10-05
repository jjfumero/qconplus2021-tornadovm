package samples;

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.annotations.Parallel;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.stream.IntStream;

/**
 * Example in TornadoVM for generating Julia Set Fractals. The algorithm used was adapted from
 * the examples in Rosetta Code:
 * <url>https://rosettacode.org/wiki/Julia_set#Java</url>
 *
 * How to run?
 *
 * <code>
 * tornado -Dversion=<tornado|mt|seq> -cp target/qconplus2021-1.0-SNAPSHOT.jar samples.JuliaSets
 *
 * # Example:
 *    $ tornado -Dversion=tornado -cp target/qconplus2021-1.0-SNAPSHOT.jar samples.JuliaSets
 * </code>
 */
public class JuliaSets {

    public final static int SIZE = 8192;

    // Version could be "tornado|mt|seq"
    // tornado = accelerate via TornadoVM
    // mt = Streams multi thread
    // seq = stream sequential
    public static final String VERSION = System.getProperty("version", "tornado");

    // Parameters for the algorithm used
    private static final int MAX_ITERATIONS = 1000;
    private static final float ZOOM = 1;
    private static final float CX = -0.7f;
    private static final float CY = 0.27015f;
    private static final float MOVE_X = 0;
    private static final float MOVE_Y = 0;

    private static TaskSchedule s0;
    private static int[] result;
    private static float[] hue;
    private static float[] brightness;


    private final static int ITERATIONS = 100;

    public JuliaSets() {
        result = new int[SIZE * SIZE];
        hue = new float[SIZE * SIZE];
        brightness = new float[SIZE * SIZE];
        if (VERSION.toLowerCase().equals("tornado")) {
            s0 = new TaskSchedule("s0")
                    .task("t0", JuliaSets::juliaSetTornado, SIZE, hue, brightness)
                    .streamOut(hue, brightness);
        }
    }

    private static int[] juliaSetsMultiSequential(int size, float[] hue, float[] brightness) {
        int[] image = new int[size * size];
        IntStream.range(0, size).sequential().forEach(x -> {
            IntStream.range(0, size).sequential().forEach(y -> {
                float zx = 1.5f * (x - size / 2) / (0.5f * ZOOM * size) + MOVE_X;
                float zy = (y - size / 2) / (0.5f * ZOOM * size) + MOVE_Y;
                float i = MAX_ITERATIONS;
                while (zx * zx + zy * zy < 4 && i > 0) {
                    float tmp = zx * zx - zy * zy + CX;
                    zy = 2.0f * zx * zy + CY;
                    zx = tmp;
                    i--;
                }
                hue[x * size + y] = (MAX_ITERATIONS / i);
                brightness[x * size + y] = i > 0 ? 1 : 0;

            });
        });
        return image;
    }

    private static int[] juliaSetsMultiThread(int size, float[] hue, float[] brightness) {
        int[] image = new int[size * size];
        IntStream.range(0, size).parallel().forEach(x -> {
            IntStream.range(0, size).parallel().forEach(y -> {
                float zx = 1.5f * (x - size / 2) / (0.5f * ZOOM * size) + MOVE_X;
                float zy = (y - size / 2) / (0.5f * ZOOM * size) + MOVE_Y;
                float i = MAX_ITERATIONS;
                while (zx * zx + zy * zy < 4 && i > 0) {
                    float tmp = zx * zx - zy * zy + CX;
                    zy = 2.0f * zx * zy + CY;
                    zx = tmp;
                    i--;
                }
                hue[x * size + y] = (MAX_ITERATIONS / i);
                brightness[x * size + y] = i > 0 ? 1 : 0;

            });
        });
        return image;
    }

    /**
     * Julia Set version adapted for TornadoVM
     *
     * It has two parallel loops, generating a 2D kernel for GPUs and FPGAs.
     *
     */
    private static void juliaSetTornado(int size, float[] hue, float[] brightness) {
        for (@Parallel int ix = 0; ix < size; ix++) {
            for (@Parallel int jx = 0; jx < size; jx++) {
                float zx = 1.5f * (ix - size / 2) / (0.5f * ZOOM * size) + MOVE_X;
                float zy = (jx - size / 2) / (0.5f * ZOOM * size) + MOVE_Y;
                float k = MAX_ITERATIONS;
                while (zx * zx + zy * zy < 4 && k > 0) {
                    float tmp = zx * zx - zy * zy + CX;
                    zy = 2.0f * zx * zy + CY;
                    zx = tmp;
                    k--;
                }
                hue[ix * size + jx] = (MAX_ITERATIONS / k);
                brightness[ix * size + jx] = k > 0 ? 1 : 0;
            }
        }
    }

    private static BufferedImage writeFile(int[] output, int size) {
        BufferedImage img = null;
        try {
            img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            WritableRaster write = img.getRaster();
            File outputFile = new File("/tmp/juliaSets.png");

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    int colour = output[(i * size + j)];
                    write.setSample(i, j, 1, colour);
                }
            }
            ImageIO.write(img, "PNG", outputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return img;
    }

    private static void runSequential() {
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            result = juliaSetsMultiSequential(SIZE, hue, brightness);
            long end = System.nanoTime();
            System.out.println("Total Sequential: " + (end - start) + " (ns)");
        }
    }

    private static void runMultiThread() {
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            result = juliaSetsMultiThread(SIZE, hue, brightness);
            long end = System.nanoTime();
            System.out.println("Total Multi-threaded: " + (end - start) + " (ns)");
        }
    }

    private static void runWithTornado() {
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            s0.execute();
            long end = System.nanoTime();
            System.out.println("Total Tornado: " + (end - start) + " (ns)");
        }
    }

    public void run() {
        if (VERSION.toLowerCase().equals("mt")) {
            runMultiThread();
        } else if (VERSION.toLowerCase().startsWith("seq")) {
            runSequential();
        } else if (VERSION.toLowerCase().equals("tornado")) {
            runWithTornado();
        }
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                result[i * SIZE + j] = Color.HSBtoRGB(hue[i * SIZE + j] % 1, 1, brightness[i * SIZE + j]);
            }
        }
        writeFile(result, SIZE);
    }

    public static void main(String[] args) {
        JuliaSets juliaSets = new JuliaSets();
        juliaSets.run();
    }
}