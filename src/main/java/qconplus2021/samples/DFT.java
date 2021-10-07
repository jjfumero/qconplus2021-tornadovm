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

import uk.ac.manchester.tornado.api.TaskSchedule;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.collections.math.TornadoMath;

/**
 * Example to run Discrete Fourier Transform (DFT) with TornadoVM.
 *
 * This example has been imported from the TornadoVM example suite:
 * <url>https://github.com/beehive-lab/TornadoVM/tree/master/examples/src/main/java/uk/ac/manchester/tornado/examples/compute</url>
 *
 */
public class DFT {

    private static boolean CHECK_RESULT = true;

    private static void computeDft(float[] inreal, float[] inimag, float[] outreal, float[] outimag, int[] inputSize) {
        int n = inreal.length;
        for (@Parallel int k = 0; k < n; k++) { // For each output element
            float sumreal = 0;
            float sumimag = 0;
            for (int t = 0; t < n; t++) { // For each input element
                float angle = ((2 * TornadoMath.floatPI() * t * k) / (float) n);
                sumreal += (inreal[t] * (TornadoMath.floatCos(angle)) + inimag[t] * (TornadoMath.floatSin(angle)));
                sumimag += -(inreal[t] * (TornadoMath.floatSin(angle)) + inimag[t] * (TornadoMath.floatCos(angle)));
            }
            outreal[k] = sumreal;
            outimag[k] = sumimag;
        }
    }

    private static boolean validate(int size, float[] inReal, float[] inImag, float[] outReal, float[] outImag, int[] inputSize) {
        boolean val = true;
        float[] outRealTor = new float[size];
        float[] outImagTor = new float[size];

        computeDft(inReal, inImag, outRealTor, outImagTor, inputSize);

        for (int i = 0; i < size; i++) {
            if (Math.abs(outImagTor[i] - outImag[i]) > 0.1) {
                System.out.println(outImagTor[i] + " vs " + outImag[i] + "\n");
                val = false;
                break;
            }
            if (Math.abs(outReal[i] - outRealTor[i]) > 0.1) {
                System.out.println(outReal[i] + " vs " + outRealTor[i] + "\n");
                val = false;
                break;
            }
        }
        System.out.println("Is valid?: " + val + "\n");
        return val;
    }

    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.println("Usage: <size> <mode:parallel|sequential> <iterations>");
            System.exit(-1);
        }

        final int size = Integer.parseInt(args[0]);
        String executionType = args[1];
        int iterations = Integer.parseInt(args[2]);

        long end,start;

        TaskSchedule taskGraph;
        float[] inReal;
        float[] inImag;
        float[] outReal;
        float[] outImag;
        int[] inputSize;

        inReal = new float[size];
        inImag = new float[size];
        outReal = new float[size];
        outImag = new float[size];
        inputSize = new int[1];

        inputSize[0] = size;

        for (int i = 0; i < size; i++) {
            inReal[i] = 1 / (float) (i + 2);
            inImag[i] = 1 / (float) (i + 2);
        }

        taskGraph = new TaskSchedule("s0")
                .task("t0", DFT::computeDft, inReal, inImag, outReal, outImag, inputSize)
                .streamOut(outReal, outImag);

        for (int i = 0; i < iterations; i++) {
            switch (executionType) {
                case "sequential":
                    start = System.nanoTime();
                    computeDft(inReal, inImag, outReal, outImag, inputSize);
                    end = System.nanoTime();
                    break;
                case "parallel":
                default:
                    start = System.nanoTime();
                    taskGraph.execute();
                    end = System.nanoTime();
            }
            System.out.println("Total time:  " + (end - start) + " (ns)");
        }

        if (CHECK_RESULT) {
            if (validate(size, inReal, inImag, outReal, outImag, inputSize)) {
                System.out.println("Validation: " + "SUCCESS " + "\n");
            } else {
                System.out.println("Validation: " + " FAIL " + "\n");
            }
        }
    }
}
