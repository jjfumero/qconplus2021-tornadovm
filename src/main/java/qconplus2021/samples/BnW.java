package qconplus2021.samples;

import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.KernelContext;
import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.WorkerGrid2D;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntime;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.stream.IntStream;

/**
 * Example using TornadoVM. This sample computes a blur filter from an JPEG image using different implementations:
 *
 * --tornado: it runs with TornadoVM using the Loop Parallel API (using a hardware accelerator)
 * --tornadoContext: it runs with TornadoVM using the Parallel Kernel API (using a hardware accelerator)
 * --mt: it runs with JDK 8 Streams (multi-threaded version without TornadoVM)
 * --seq: it runs sequentially (no acceleration)
 */
public class BnW {

    private static final int MAX_ITERATIONS = 10;

    public enum Implementation {
        SEQUENTIAL,
        MT,
        TORNADO_LOOP,
        TORNADO_KERNEL
    }


    public void loadImage() {
        try {
            image = ImageIO.read(new File(IMAGE_FILE));
            w = image.getWidth();
            h = image.getHeight();
            imageRGB = new int[w * h];
        } catch (IOException e) {
            throw new RuntimeException("Input file not found: " + IMAGE_FILE);
        }
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int rgb = image.getRGB(i, j);
                imageRGB[i * h + j] = rgb;
            }
        }
    }

    public BnW(Implementation implementation) {
        this.implementation = implementation;
        loadImage();
        if (implementation == Implementation.TORNADO_LOOP) {
            // Tasks using the Loop Parallel API
            parallelFilter = new TaskSchedule("s0") //
                    .task("t0", BnW::compute, imageRGB, w, h) //
                    .streamOut(imageRGB);

        } else if (implementation == Implementation.TORNADO_KERNEL) {
            // Tasks using the Kernel API
            KernelContext context = new KernelContext();
            grid = new GridScheduler();
            // This version might run slower, since thread block size can influence performance.
            // TornadoVM implements a heuristic for thread block selection (available for loop-parallel API)
            WorkerGrid2D worker = new WorkerGrid2D(w, h);
            worker.setLocalWork(16, 16, 1);

            grid.setWorkerGrid("s0.t0", worker);
            parallelFilter = new TaskSchedule("s0") //
                    .task("t0", BnW::computeContext, imageRGB, w, h, context) //
                    .streamOut(imageRGB);

        }
    }

    private static final HashMap<String, Implementation> VALID_OPTIONS = new HashMap<>();

    static {
        VALID_OPTIONS.put("sequential", Implementation.SEQUENTIAL);
        VALID_OPTIONS.put("seq", Implementation.SEQUENTIAL);
        VALID_OPTIONS.put("mt", Implementation.MT);
        VALID_OPTIONS.put("tornado", Implementation.TORNADO_LOOP);
        VALID_OPTIONS.put("tornadoContext", Implementation.TORNADO_KERNEL);
        VALID_OPTIONS.put("tornadocontext", Implementation.TORNADO_KERNEL);
    }

    private BufferedImage image;
    private Implementation implementation;
    private TaskSchedule parallelFilter;

    private static final String IMAGE_FILE = "/tmp/image.jpg";

    int w;
    int h;
    int[] imageRGB;
    private GridScheduler grid;

    private static void compute(int[] image, final int w, final int s) {
        for (@Parallel int i = 0; i < w; i++) {
            for (@Parallel int j = 0; j < s; j++) {
                int rgb = image[i * s + j];
                int alpha = (rgb >> 24) & 0xff;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = (rgb & 0xFF);

                int grayLevel = (red + green + blue) / 3;
                int gray = (alpha << 24) | (grayLevel << 16) | (grayLevel << 8) | grayLevel;

                image[i * s + j] = gray;
            }
        }
    }

    private static void computeContext(int[] image, final int w, final int s, KernelContext context) {
        int i = context.globalIdx;
        int j = context.globalIdy;
        int rgb = image[i * s + j];
        int alpha = (rgb >> 24) & 0xff;
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = (rgb & 0xFF);
        int grayLevel = (red + green + blue) / 3;
        int gray = (alpha << 24) | (grayLevel << 16) | (grayLevel << 8) | grayLevel;
        image[i * s + j] = gray;
    }

    private void sequentialComputation() {
        for (int i = 0; i< MAX_ITERATIONS; i++) {
            long start = System.nanoTime();
            compute(imageRGB, w, h);
            long end = System.nanoTime();
            System.out.println("Sequential Total time (ns) = " + (end - start) + " -- seconds = " + ((end - start) * 1e-9));
        }
    }

    private void parallelStreams() {
        for (int i = 0; i< MAX_ITERATIONS; i++) {
            long start = System.nanoTime();
            computeWithParallelStreams(imageRGB, w, h);
            long end = System.nanoTime();
            System.out.println("Streams Total time (ns) = " + (end - start) + " -- seconds = " + ((end - start) * 1e-9));
        }
    }

    private void computeWithParallelStreams(int[] imageInput, int w, int h) {
        IntStream.range(0, w).parallel().forEach(r -> {
            IntStream.range(0, h).parallel().forEach(c -> {
                int rgb = imageInput[r * h + c];
                int alpha = (rgb >> 24) & 0xff;
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = (rgb & 0xFF);

                int grayLevel = (red + green + blue) / 3;
                int gray = (alpha << 24) | (grayLevel << 16) | (grayLevel << 8) | grayLevel;

                imageInput[r * h + c] = gray;
            });
        });
    }

    private void runTornadoVM() {
       // TornadoDevice device = TornadoRuntime.getTornadoRuntime().getDriver(0).getDevice(0);
        //System.out.println(device);
        //parallelFilter.mapAllTo(device);
        for (int i = 0; i< MAX_ITERATIONS; i++) {
            long start = System.nanoTime();
            parallelFilter.execute();
            long end = System.nanoTime();
            System.out.println("Total Time (ns) = " + (end - start) + " -- seconds = " + ((end - start) * 1e-9));
        }
    }

    private void runTornadoVMWithContext() {
        TornadoDevice device = TornadoRuntime.getTornadoRuntime().getDriver(0).getDevice(0);
        System.out.println(device);
        parallelFilter.mapAllTo(device);
        for (int i = 0; i< MAX_ITERATIONS; i++) {
            long start = System.nanoTime();
            parallelFilter.execute(grid);
            long end = System.nanoTime();
            System.out.println("Total Time (ns) = " + (end - start) + " -- seconds = " + ((end - start) * 1e-9));
        }
    }

    private void writeImage(String fileName) {
        // unmarshall
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                image.setRGB(i, j, imageRGB[i * h + j]);
            }
        }

        try {
            ImageIO.write(image, "jpg", new File("/tmp/" + fileName));
        } catch (IOException e) {
            throw new RuntimeException("Input file not found: " + IMAGE_FILE);
        }
    }

    public void run() {
        switch (implementation) {
            case SEQUENTIAL:
                sequentialComputation();
                break;
            case MT:
                parallelStreams();
                break;
            case TORNADO_LOOP:
                runTornadoVM();
                break;
            case TORNADO_KERNEL:
                runTornadoVMWithContext();
                break;
        }
        writeImage("parallel.jpg");
    }

    public static void main(String[] args) {
        String version = "tornado";
        if (args.length != 0) {
            version = args[0].substring(2);
            if (!VALID_OPTIONS.containsKey(version)) {
                System.out.println("Option not valid. Use:");
                System.out.println("\t--tornado: for accelerated version with TornadoVM");
                System.out.println("\t--tornadoContext: for accelerated version with TornadoVM");
                System.out.println("\t--seq: for running the sequential version with Java Streams");
                System.out.println("\t--mt: for running the CPU multi-thread version with Java Parallel Streams");
                System.exit(-1);
            }
        }
        BnW imageFilter = new BnW(VALID_OPTIONS.get(version));
        imageFilter.run();
    }

}
