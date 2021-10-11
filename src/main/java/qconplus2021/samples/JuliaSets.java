/*
 * Copyright 2021, Juan Fumero
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package qconplus2021.samples;

import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.KernelContext;
import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.WorkerGrid2D;
import uk.ac.manchester.tornado.api.annotations.Parallel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.HashSet;
import java.util.stream.IntStream;

/**
 * Example in TornadoVM for generating Julia Set Fractals. The algorithm used was adapted from
 * the examples in Rosetta Code:
 * <url>https://rosettacode.org/wiki/Julia_set#Java</url>
 *
 * How to run?
 *
 * <code>
 * tornado qconplus2021.samples.JuliaSets --<tornado|tornadoContext|mt|seq>
 *
 * # Example:
 *    $ tornado qconplus2021.samples.JuliaSets --tornado
 * </code>
 */
public class JuliaSets {

    public final static int SIZE = 8192;

    // Version could be "--<tornado|mt|seq>"
    // tornado = accelerate via TornadoVM (Loop Parallel API)
    // tornado = accelerate via TornadoVM and KernelContext API
    // mt = Streams multi thread
    // seq = stream sequential
    public static String VERSION;

    // Parameters for the algorithm used
    private static final int MAX_ITERATIONS = 1000;
    private static final float ZOOM = 1;
    private static final float CX = -0.7f;
    private static final float CY = 0.27015f;
    private static final float MOVE_X = 0;
    private static final float MOVE_Y = 0;

    private static TaskSchedule s0;
    private static TaskSchedule sContext;
    private static int[] result;
    private static float[] hue;
    private static float[] brightness;


    private final static int ITERATIONS = 10;
    private static GridScheduler grid;

    private static final HashSet<String> VALID_OPTIONS = new HashSet<>();
    static {
        VALID_OPTIONS.add("sequential");
        VALID_OPTIONS.add("seq");
        VALID_OPTIONS.add("mt");
        VALID_OPTIONS.add("tornado");
        VALID_OPTIONS.add("tornadoContext");
        VALID_OPTIONS.add("tornadocontext");
    }

    public JuliaSets() {
        result = new int[SIZE * SIZE];
        hue = new float[SIZE * SIZE];
        brightness = new float[SIZE * SIZE];
        if (VERSION.equalsIgnoreCase("tornado")) {
            s0 = new TaskSchedule("s0")
                    .task("t0", JuliaSets::juliaSetTornado, SIZE, hue, brightness)
                    .streamOut(hue, brightness);
        } else if (VERSION.equalsIgnoreCase("tornadoContext")) {

            WorkerGrid2D worker2D = new WorkerGrid2D(SIZE, SIZE);
            KernelContext context = new KernelContext();
            grid = new GridScheduler();
            grid.setWorkerGrid("s0.t0", worker2D);

            s0 = new TaskSchedule("s0")
                    .task("t0", JuliaSets::juliaSetTornadoWithContext, SIZE, hue, brightness, context)
                    .streamOut(hue, brightness);
        }
    }

    private static void juliaSetsStreamsSequential(int size, float[] hue, float[] brightness) {
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
    }

    private static void juliaSetsStreamsParallel(int size, float[] hue, float[] brightness) {
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

    /**
     * Julia Set version adapted for TornadoVM
     *
     * It has two parallel loops, generating a 2D kernel for GPUs and FPGAs.
     *
     * It uses the KernelContext API
     *
     */
    private static void juliaSetTornadoWithContext(int size, float[] hue, float[] brightness, KernelContext context) {
        int ix = context.globalIdx;
        int jx = context.globalIdy;
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


    private static BufferedImage writeFile(int[] output, int size) {
        BufferedImage img = null;
        try {
            img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            WritableRaster write = img.getRaster();

            String tmpDirsLocation = System.getProperty("java.io.tmpdir");
            File outputFile = new File(tmpDirsLocation + "/juliaSets.png");

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
            juliaSetsStreamsSequential(SIZE, hue, brightness);
            long end = System.nanoTime();
            double seconds = (end - start) * 1E-9;
            System.out.println("Total Sequential: " + (end - start) + " (ns) --  " +  seconds + " (s)");
        }
    }

    private static void runMultiThread() {
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            juliaSetsStreamsParallel(SIZE, hue, brightness);
            long end = System.nanoTime();
            double seconds = (end - start) * 1E-9;
            System.out.println("Total Multi-threaded: " + (end - start) + " (ns) --  " +  seconds + " (s)");
        }
    }

    private static void runWithTornado() {
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            s0.execute();
            long end = System.nanoTime();
            double seconds = (end - start) * 1E-9;
            System.out.println("Total Tornado: " + (end - start) + " (ns) --  " +  seconds + " (s)");
        }
    }

    private static void runWithTornadoContext() {
        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            s0.execute(grid);
            long end = System.nanoTime();
            double seconds = (end - start) * 1E-9;
            System.out.println("Total Tornado: " + (end - start) + " (ns) --  " +  seconds + " (s)");
        }
    }

    public void run() {
        if (VERSION.equalsIgnoreCase("mt")) {
            runMultiThread();
        } else if (VERSION.toLowerCase().startsWith("seq")) {
            runSequential();
        } else if (VERSION.equalsIgnoreCase("tornado")) {
            runWithTornado();
        } else if (VERSION.equalsIgnoreCase("tornadoContext")) {
            runWithTornadoContext();
        }
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                result[i * SIZE + j] = Color.HSBtoRGB(hue[i * SIZE + j] % 1, 1, brightness[i * SIZE + j]);
            }
        }
        writeFile(result, SIZE);
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            VERSION = "tornado";
        } else {
            String version = args[0].substring(2);
            if (!VALID_OPTIONS.contains(version)) {
                System.out.println("Option not valid. Use:");
                System.out.println("\t--tornado: for accelerated version with TornadoVM");
                System.out.println("\t--tornadocontext: for accelerated version with TornadoVM");
                System.out.println("\t--seq: for running the sequential version with Java Streams");
                System.out.println("\t--mt: for running the CPU multi-thread version with Java Parallel Streams");
                System.exit(-1);
            } else {
                VERSION = version;
            }
        }

        JuliaSets juliaSets = new JuliaSets();
        juliaSets.run();
    }
}