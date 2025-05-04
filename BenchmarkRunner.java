import java.lang.management.*;
import java.util.List;
import java.util.Scanner;

import ForkJoinFrameworkSolution.ForkJoinFrameworkSolution;
import MultithreadedSolutionWithThreadPools.MultithreadedSolutionWithThreadPools;
import MultithreadedSolutionWithoutThreadPools.MultithreadedSolutionWithoutThreadPools;
import SequentialSolution.SequentialSolution;

public class BenchmarkRunner {
    // This benchmark runner was developed to run every implementation to solve the problem.
    // It will give the necessary inputs ( max number of pages, and input file ) and benchmark the execution.

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Choose an implementation to benchmark:");
        System.out.println("1 - Sequential Solution");
        System.out.println("2 - Multithreaded with Thread Pools");
        System.out.println("3 - ForkJoin Framework");
        System.out.println("4 - Multithreaded without Thread Pools");
        System.out.print("Enter choice (1-4): ");
        int choice = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Enter maxPages (e.g, 100000 ): ");
        int maxPages = scanner.nextInt();
        scanner.nextLine();

        System.out.print("Enter file name (e.g., large_wiki_file.xml): ");
        String fileName = "WikiDumps/";
        fileName += scanner.nextLine();

        System.out.print("Enter number of threads (if applicable): ");
        int threadNumber = scanner.nextInt();
        scanner.nextLine();

        System.out.println("\nBenchmarking begins...\n");

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

        switch (choice) {
            case 1 -> SequentialSolution.run(maxPages, fileName);
            case 2 -> MultithreadedSolutionWithThreadPools.run(maxPages, fileName, threadNumber);
            case 3 -> ForkJoinFrameworkSolution.run(maxPages, fileName, threadNumber);
            case 4 -> MultithreadedSolutionWithoutThreadPools.run(maxPages, fileName, threadNumber);
            default -> {
                System.out.println("Invalid option.");
                return;
            }
        }

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

        scanner.close();
    }
}
