package samples;

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
 * TornadoVM - Simple computation!
 *
 * How to run?
 *
 * tornado --threadInfo --printKernel -cp target/qconplus2021-1.0-SNAPSHOT.jar samples.Sample
 *
 */
public class Sample {

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
                .task("t0", Sample::accelerateVectorOperations, a, b, c)
                .streamOut(c) //
                .execute();

        // Kernel API
        WorkerGrid workerGrid = new WorkerGrid1D(8192);
        GridScheduler grid = new GridScheduler();
        grid.setWorkerGrid("s1.t0", workerGrid);
        KernelContext context = new KernelContext();
        new TaskSchedule("s1") //
                .task("t0", Sample::accelerateVectorOperationsKernelAPI, context, a, b, c)
                .streamOut(c)
                .execute(grid);
    }
}
