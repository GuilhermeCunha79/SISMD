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

    public static void run(int maxPages, String fileName, int threadCount) throws ExecutionException, InterruptedException {

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        Iterable<Page> pages = new Pages(maxPages, fileName);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Page page : pages) {
            if (page == null) break;

            // Submit page
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> processPage(page), executorService);
            futures.add(future);
        }

        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join(); // Wait for completion

        executorService.shutdown();

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
