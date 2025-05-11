package CompletableFuturesBasedSolution;

import SharedUtilities.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.*;

public class CompletableFuturesBasedSolution {

    private static final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java CompletableFuturesBasedSolution <maxPages> <fileName> <threadCount>");
            return;
        }

        try {
            int maxPages = Integer.parseInt(args[0]);
            String fileName = args[1];
            int threadCount = Integer.parseInt(args[2]);

            run(maxPages, fileName, threadCount);
        } catch (NumberFormatException e) {
            System.err.println("Invalid number format for maxPages or threadCount. They must be integers.");
        } catch (ExecutionException | InterruptedException e) {
            System.err.println("Error during execution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void run(int maxPages, String fileName, int threadCount) throws ExecutionException, InterruptedException {
        long start = System.currentTimeMillis();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        Iterable<Page> pages = new Pages(maxPages, fileName);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Page page : pages) {
            if (page == null) break;

            // Submit page for processing
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processPage(page), executorService);
            futures.add(future);
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join(); // Wait for completion

        executorService.shutdown();

        long end = System.currentTimeMillis();
        System.out.println("Processed pages: " + maxPages);
        System.out.println("Elapsed time: " + (end - start) + "ms");

        LinkedHashMap<String, Integer> commonWords = counts.entrySet().stream()
                .sorted(Map.Entry.<String, AtomicInteger>comparingByValue(Comparator.comparingInt(AtomicInteger::get)).reversed())
                .limit(3)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get(),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        commonWords.forEach((word, count) -> System.out.println("Word: '" + word + "' with total " + count + " occurrences!"));
    }

    private static void processPage(Page page) {
        Iterable<String> words = new Words(page.getText());
        for (String word : words) {
            if (word.length() > 1 || word.equals("a") || word.equals("I")) {
                counts.computeIfAbsent(word, k -> new AtomicInteger(0)).incrementAndGet();
            }
        }
    }
}