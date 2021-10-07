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
import uk.ac.manchester.tornado.api.WorkerGrid;
import uk.ac.manchester.tornado.api.WorkerGrid1D;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.collections.types.Float4;
import uk.ac.manchester.tornado.api.collections.types.VectorFloat4;

import java.util.Random;

/**
 * TornadoVM - Simple computation using vector types in TornadoVM
 *
 * How to run?
 *
 * tornado --threadInfo --printKernel -cp target/qconplus2021-1.0-SNAPSHOT.jar samples.VectorTypes
 *
 * Flags:
 *  --threadInfo: prints in real time the accelerator used and the threads launched on the target device.
 *  --printKernel: prints the generated kernel by TornadoVM
 *
 */
public class VectorTypes {

    // Loop Parallel API
    private static void accelerateVectorOperations(VectorFloat4 inputA, VectorFloat4 inputB, VectorFloat4 output) {
        for (@Parallel int i = 0; i < inputA.getLength(); i++) {
            output.set(i, Float4.add(inputA.get(i), inputB.get(i)));
        }
    }

    // Kernel Parallel API
    private static void accelerateVectorOperationsKernelAPI(KernelContext context, VectorFloat4 inputA, VectorFloat4 inputB, VectorFloat4 output) {
        int idx = context.globalIdx;
        output.set(idx, Float4.sub(inputA.get(idx), inputB.get(idx)));
    }

    public static void main( String[] args ) {

        VectorFloat4 a = new VectorFloat4(8192);
        VectorFloat4 b = new VectorFloat4(8192);
        VectorFloat4 c = new VectorFloat4(8192);


        // Input Data Initialization
        Random r = new Random();
        for (int i = 0; i < a.getLength(); i++) {
            Float4 vectorElementA = new Float4();
            Float4 vectorElementB = new Float4();
            for (int j = 0; j < vectorElementA.size(); j++) {
                vectorElementA.set(j, r.nextFloat());
                vectorElementB.set(j, r.nextFloat());
            }
            a.set(i, vectorElementA);
            b.set(i, vectorElementB);
        }

        // Loop Parallel API
        new TaskSchedule("s0") //
                .task("t0", VectorTypes::accelerateVectorOperations, a, b, c)
                .streamOut(c) //
                .execute();

        // Kernel API
        WorkerGrid workerGrid = new WorkerGrid1D(8192);
        GridScheduler grid = new GridScheduler();
        grid.setWorkerGrid("s1.t0", workerGrid);
        KernelContext context = new KernelContext();
        new TaskSchedule("s1") //
                .task("t0", VectorTypes::accelerateVectorOperationsKernelAPI, context, a, b, c)
                .streamOut(c)
                .execute(grid);
    }
}
