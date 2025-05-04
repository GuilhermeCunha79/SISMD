import java.lang.management.*;
import java.util.List;

import MultithreadedSolutionWithThreadPools.MultithreadedSolutionWithThreadPools;

public class BenchmarkRunner {
    // This benchmark runner was developed to run every implementation to solve the problem.
    // It will give the necessary inputs ( max number of pages, and input file ) and benchmark the execution.

    private static String fileName = "WikiDumps/large_wiki_file.xml";

    private static int maxPages = 100000;

    public static void main(String[] args) throws Exception {
        System.out.println("Benchmarking begins...\n");

        Runtime runtime = Runtime.getRuntime();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        long gcCountBefore = 0;
        long gcTimeBefore = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            gcCountBefore += gc.getCollectionCount();
            gcTimeBefore += gc.getCollectionTime();
        }

        long cpuBefore = System.nanoTime();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        long wallClockBefore = System.nanoTime();

        /* 
        Place here any of the classes to benchmark, assure that the arguments stay the same throughout every class.
        */

        //SequentialSolution.main(args,maxPages,fileName);
        //MultithreadedSolutionWithThreadPools.main(args,maxPages,fileName);

        long wallClockAfter = System.nanoTime();
        long cpuAfter = System.nanoTime(); 
        long memAfter = runtime.totalMemory() - runtime.freeMemory();

        long gcCountAfter = 0;
        long gcTimeAfter = 0;
        for (GarbageCollectorMXBean gc : gcBeans) {
            gcCountAfter += gc.getCollectionCount();
            gcTimeAfter += gc.getCollectionTime();
        }

        // Print benchmark results
        System.out.println("\n====== Benchmark Results ======");
        System.out.println("Elapsed time: " + (wallClockAfter - wallClockBefore)/1_000_000 + " ms"); // Real time passed.
        System.out.println("CPU time: " + (cpuAfter - cpuBefore)/1_000_000 + " ms"); // Time that CPU spent executing.
        System.out.println("Memory used: " + (memAfter - memBefore) / (1024 * 1024) + " MB"); // RAM used during execution.
        System.out.println("Garbage Collector collections: " + (gcCountAfter - gcCountBefore)); // Amount of GC collections.
        System.out.println("Garbage Collector time: " + (gcTimeAfter - gcTimeBefore) + " ms"); // Time GC spent collecting memory.
    }
}
