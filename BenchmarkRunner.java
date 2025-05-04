import java.lang.management.*;
import java.util.List;

import MultithreadedSolutionWithThreadPools.MultithreadedSolutionWithThreadPools;

public class BenchmarkRunner {

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

        //SequentialSolution.main(args,100000,"WikiDumps/large_wiki_file.xml");
        //MultithreadedSolutionWithThreadPools.main(args,100000,"WikiDumps/large_wiki_file.xml");

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
        System.out.println("Wall-clock time: " + (wallClockAfter - wallClockBefore)/1_000_000 + " ms");
        System.out.println("Approx. CPU time: " + (cpuAfter - cpuBefore)/1_000_000 + " ms");
        System.out.println("Memory used: " + (memAfter - memBefore) / (1024 * 1024) + " MB");
        System.out.println("Garbage Collector collections: " + (gcCountAfter - gcCountBefore));
        System.out.println("Garbage Collector time: " + (gcTimeAfter - gcTimeBefore) + " ms");
    }
}
